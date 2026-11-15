package com.careful.hyperfvm.compose.data.dashboard

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 极简 DOM 封装。
 *
 * 原先用 `android.util.Xml.newPullParser()`（org.xmlpull.v1 只在 Android 上提供），
 * 换成 `javax.xml` 后两端同一套代码，代价是解析一次性读完而不是流式——
 * 这些配置文件只有几十 KB，无所谓。
 */
internal object XmlDom {

    fun parse(text: String): Document? = runCatching {
        val factory = DocumentBuilderFactory.newInstance()
        // 配置来自远端：禁掉 DOCTYPE 与外部实体，避免把远端内容当本地文件读（XXE）。
        // Android 的解析器未必支持这些 feature，失败就算了，不影响正常解析。
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { factory.isExpandEntityReferences = false }
        factory.newDocumentBuilder().parse(InputSource(StringReader(text)))
    }.getOrNull()

    /** 按标签名取全部后代元素（文档顺序）。 */
    fun elements(document: Document, tag: String): List<Element> {
        val nodes = document.getElementsByTagName(tag)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    /** 取属性值；空串按「没有这个属性」处理，调用方就不必再判空串。 */
    fun attr(element: Element, name: String): String? =
        element.getAttribute(name).takeIf { it.isNotEmpty() }
}
