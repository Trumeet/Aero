package moe.yuuta.aero

import aero.ACCENT_POLICY
import aero.WINDOWCOMPOSITIONATTRIBDATA
import aero.pfnSetWindowCompositionAttribute
import kotlinx.cinterop.*
import platform.windows.*
import versionhelper.IsWindows10OrGreater
import versionhelper.IsWindowsVistaOrGreater

object WinApi {
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

    private fun _setAero10(hwnd: HWND?) {
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

    fun setAero10(hwnd: HWND?) {
        if (IsWindows10OrGreater() == 1) {
            _setAero10(hwnd)
        }
    }

    private fun _setAero7(hwnd: HWND?): Int {
        val margins = cValue<MARGINS> {
            cxLeftWidth = -1
        }
        return memScoped {
            return@memScoped DwmExtendFrameIntoClientArea(hwnd, margins.ptr)
        }
    }

    fun setAero7(hwnd: HWND?) {
        if (IsWindowsVistaOrGreater() == 1 &&
            WinCompat.DwmIsCompositionEnabled()) {
            val result = _setAero7(hwnd)
            if (result != 0) {
                MessageBoxA(hwnd,
                    "Cannot extend the aero glass (0x${result.toString(16)})",
                    "Aero",
                    (MB_OK or MB_ICONERROR).convert())
            }
        }
    }
}