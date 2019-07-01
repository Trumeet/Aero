package moe.yuuta.aero

import kotlinx.cinterop.*
import platform.windows.GetModuleHandleA
import platform.windows.GetStartupInfoA
import platform.windows.WINBOOLVar
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

    fun DwmIsCompositionEnabled(): Boolean {
        val enable = nativeHeap.alloc<WINBOOLVar>()
        platform.windows.DwmIsCompositionEnabled(enable.ptr)
        val result = enable.value
        nativeHeap.free(enable)
        return result > 0
    }
}