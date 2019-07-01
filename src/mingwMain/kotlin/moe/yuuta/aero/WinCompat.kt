package moe.yuuta.aero

import kotlinx.cinterop.*
import platform.windows.GetModuleHandleA
import platform.windows.GetStartupInfoA
import platform.windows._STARTUPINFOA

object WinCompat {
    val nCmdShow
        get() = memScoped {
            val startUpInfo = nativeHeap.alloc<_STARTUPINFOA>()
            GetStartupInfoA(startUpInfo.ptr)
            val nCmdShow = startUpInfo.wShowWindow
            nativeHeap.free(startUpInfo)
            return@memScoped nCmdShow.convert<Int>()
        }
    val hInstance
        get() = GetModuleHandleA(null)
}