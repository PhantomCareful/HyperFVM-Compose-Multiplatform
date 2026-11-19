package com.careful.hyperfvm.compose.data.itemQuery

import com.careful.hyperfvm.compose.platform.AppEnv
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * 道具表的磁盘缓存（`cache/item_query.idx`）。
 *
 * 整表解析要扫 5 MB XML，而只要数据包没换，解析结果就是固定的：
 * 把 [ItemEntry] 列表按顺序写成二进制，下次启动直接读回，省掉整次解析。
 *
 * 有效性靠文件头里的三项：格式版本、数据包 version、`card_desc.xml` 的大小与修改时间。
 * 任何一项对不上、文件被截断、缓存目录被系统清掉，都当作「没有缓存」，
 * 由调用方回退到解析并重写；读取失败一律不外抛，用户最多多等一次解析。
 *
 * 写入走「临时文件 + 改名」：解析完才落盘，写一半被杀掉不会留下半个索引。
 */
internal object ItemQueryCache {

    /** 文件头魔数（"IQU1"）：防止把别的文件误当成缓存。 */
    private const val Magic = 0x49515531

    /** 条目序列化格式的版本：字段增删时 +1，旧缓存自动作废。 */
    private const val FormatVersion = 1

    /** 条数上限：文件损坏时不能按一个巨大的数字去预分配。 */
    private const val MaxEntries = 200_000

    /** 单个字符串的字节上限：一条描述最长也就几 KB，超了必然是坏文件。 */
    private const val MaxStringBytes = 4 * 1024 * 1024

    /** 单个礼包的候选行上限，同 [MaxEntries] 的作用。 */
    private const val MaxBagEntries = 5_000

    private const val BufferSize = 64 * 1024

    fun file(env: AppEnv): File = File(env.cacheDir, "item_query.idx")

    /** 数据包换新后删除：新旧包的 XML 可能同名同大小，不能只靠文件头判断。 */
    fun delete(env: AppEnv) {
        file(env).delete()
    }

    /**
     * 读回上次的解析结果；没有缓存、文件头不符或内容坏了都返回 null。
     *
     * [packVersion] / [xmlLength] / [xmlModified] 是当前数据包的特征值，
     * 与文件头里记录的完全一致才认为缓存仍然对应当前这份 XML。
     */
    fun read(env: AppEnv, packVersion: Int, xmlLength: Long, xmlModified: Long): List<ItemEntry>? {
        val target = file(env)
        if (!target.isFile) return null
        return runCatching {
            DataInputStream(BufferedInputStream(target.inputStream(), BufferSize)).use { input ->
                if (input.readInt() != Magic) return@use null
                if (input.readInt() != FormatVersion) return@use null
                if (input.readInt() != packVersion) return@use null
                if (input.readLong() != xmlLength) return@use null
                if (input.readLong() != xmlModified) return@use null
                val count = input.readInt()
                if (count !in 0..MaxEntries) return@use null
                val result = ArrayList<ItemEntry>(count)
                repeat(count) { result += readEntry(input) }
                // 读完应当正好到文件末尾：还有剩余字节说明格式不是这一版，宁可重解析。
                if (input.read() != -1) return@use null
                result
            }
        }.getOrNull()
    }

    /** 写一份新缓存；[entries] 与三个特征值来自刚刚完成的那次解析。失败静默忽略。 */
    fun write(env: AppEnv, packVersion: Int, xmlLength: Long, xmlModified: Long, entries: List<ItemEntry>) {
        val target = file(env)
        val temp = File(target.parentFile, "${target.name}.tmp")
        val written = runCatching {
            target.parentFile?.mkdirs()
            DataOutputStream(BufferedOutputStream(temp.outputStream(), BufferSize)).use { output ->
                output.writeInt(Magic)
                output.writeInt(FormatVersion)
                output.writeInt(packVersion)
                output.writeLong(xmlLength)
                output.writeLong(xmlModified)
                output.writeInt(entries.size)
                for (entry in entries) writeEntry(output, entry)
            }
            // Windows 上改名不能覆盖已有文件，先删旧的再改名。
            temp.renameTo(target) || run {
                target.delete()
                temp.renameTo(target)
            }
        }.getOrDefault(false)
        if (!written) temp.delete()
    }

    private fun writeEntry(output: DataOutputStream, entry: ItemEntry) {
        output.writeString(entry.id)
        output.writeString(entry.name)
        output.writeString(entry.type)
        output.writeNullableString(entry.description)
        output.writeNullableString(entry.energy)
        output.writeNullableString(entry.hp)
        val bag = entry.bag
        output.writeBoolean(bag != null)
        if (bag != null) {
            output.writeString(bag.header)
            output.writeNullableString(bag.note)
            output.writeInt(bag.entries.size)
            for (row in bag.entries) {
                output.writeString(row.name)
                output.writeNullableString(row.quantity)
                output.writeNullableString(row.itemId)
                output.writeBoolean(row.isWeapon)
            }
        }
    }

    private fun readEntry(input: DataInputStream): ItemEntry {
        val id = input.readString()
        val name = input.readString()
        val type = input.readString()
        val description = input.readNullableString()
        val energy = input.readNullableString()
        val hp = input.readNullableString()
        val bag = if (input.readBoolean()) {
            val header = input.readString()
            val note = input.readNullableString()
            val count = input.readInt()
            require(count in 0..MaxBagEntries) { "礼包行数异常：$count" }
            val rows = ArrayList<BagEntry>(count)
            repeat(count) {
                rows += BagEntry(
                    name = input.readString(),
                    quantity = input.readNullableString(),
                    itemId = input.readNullableString(),
                    isWeapon = input.readBoolean(),
                )
            }
            ItemBag(header = header, note = note, entries = rows)
        } else {
            null
        }
        return ItemEntry(
            id = id,
            name = name,
            description = description,
            type = type,
            energy = energy,
            hp = hp,
            bag = bag,
        )
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeString(value)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..MaxStringBytes) { "字符串长度异常：$size" }
        val bytes = ByteArray(size)
        readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null
}
