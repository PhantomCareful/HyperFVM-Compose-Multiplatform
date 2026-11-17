package com.careful.hyperfvm.compose.data.cardCatalog

/**
 * 防御卡数据库的「当前版本」，显示在目录页概览大卡的「当前版本」一栏。
 *
 * 与 Java 版 strings.xml 的 `version_card_data_search` 保持一致，卡表更新时手动同步。
 * 这个值是整套卡表的版本口径，跟数据一起变动，所以单独成文件存放，方便查找和修改。
 */
internal const val CardCatalogDataVersion = "20261112"
