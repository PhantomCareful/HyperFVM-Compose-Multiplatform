package com.careful.hyperfvm.compose

import android.app.Application

/**
 * 应用入口。
 *
 * 数据图查看器迁到 Compose 页面内覆盖层之后，这里不再需要做任何框架初始化：
 * mojito 时代的加载器与视图工厂都是静态字段、必须在 Activity 之前注册，现在整条链路
 * 已经移除（图片解码交给 Telephoto，按需在组合里创建）。
 */
class HyperFVMApplication : Application()
