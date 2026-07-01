package com.nexussu.manager.core
object NativeRoot {
    init { System.loadLibrary("nexussu_client") }
    external fun requestElevation(): Boolean
}
