package moe.yuuta.aero

import aero.ACCENT_POLICY
import aero.WINDOWCOMPOSITIONATTRIBDATA
import aero.pfnSetWindowCompositionAttribute
import kotlinx.cinterop.*
import platform.windows.*

object WinApi {
    fun getenv(name: String): String = memScoped {
        val buffer = nativeHeap.allocArray<CHARVar>(MAX_PATH)
        GetEnvironmentVariableA(name,
            buffer,
            MAX_PATH
        )
        val env = buffer.toKString()
        nativeHeap.free(buffer)
        return@memScoped env
    }

    fun msgLoop() {
        memScoped {
            val msg = nativeHeap.alloc<MSG>()
            while (GetMessageA(msg.ptr,
                    null,
                    0.convert(),
                    0.convert()) != 0) {
                TranslateMessage(msg.ptr)
                DispatchMessageA(msg.ptr)
            }
            // The queue quits
            nativeHeap.free(msg)
        }
    }

    fun setAero(hwnd: HWND?) {
        val hUser = GetModuleHandleW("user32.dll") ?: return
        val setWindowCompositionAttribute = GetProcAddress(hUser,
            "SetWindowCompositionAttribute")
                as pfnSetWindowCompositionAttribute? ?: return
        val accent = cValue<ACCENT_POLICY> {
            AccentState = aero.ACCENT_ENABLE_BLURBEHIND
            AccentFlags = 0.convert()
            GradientColor = 0.convert()
            AnimationId = 0.convert()
        }
        memScoped {
            val data = cValue<WINDOWCOMPOSITIONATTRIBDATA> {
                Attrib = aero.WCA_ACCENT_POLICY
                pvData = accent.ptr
                cbData = accent.size.convert()
            }
            setWindowCompositionAttribute(hwnd, data.ptr)
        }
    }
}