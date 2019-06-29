package moe.yuuta.aero

import aero.*
import kotlinx.cinterop.*
import platform.windows.*

fun main() {
    SetProcessDPIAware()
    val dlgImpl = cValue<DLGTEMPLATE> {
        style = (WS_OVERLAPPEDWINDOW or
                DS_CENTER or
                DS_MODALFRAME).convert()
        dwExtendedStyle = 0.convert()
        cdit = 0.convert()
        x = 0
        y = 0
        cx = 400
        cy = 200
    }
    memScoped {
        return@memScoped DialogBoxIndirectParamW(GetModuleHandleA(null),
            dlgImpl.ptr,
            null,
            staticCFunction(::dlgProc) as DLGPROC,
            0)
    }
}

fun dlgProc(hDlg: HWND,
            message: UINT,
            wParam: WPARAM,
            lParam: LPARAM): INT_PTR {
    when (message.convert<Int>()) {
        WM_INITDIALOG -> {
            val hUser = GetModuleHandleW("user32.dll")
            if (hUser != null) {
                val setWindowCompositionAttribute = GetProcAddress(hUser, "SetWindowCompositionAttribute") as pfnSetWindowCompositionAttribute?
                if (setWindowCompositionAttribute != null) {
                    val accent = cValue<ACCENT_POLICY> {
                        AccentState = ACCENT_ENABLE_BLURBEHIND
                        AccentFlags = 0.convert()
                        GradientColor = 0.convert()
                        AnimationId = 0.convert()
                    }
                    memScoped {
                        val data = cValue<WINDOWCOMPOSITIONATTRIBDATA> {
                            Attrib = WCA_ACCENT_POLICY
                            pvData = accent.ptr
                            cbData = accent.size.convert()
                        }
                        setWindowCompositionAttribute(hDlg, data.ptr)
                    }
                }
            }
            return true.toByte().toLong()
        }
        WM_COMMAND -> {
            EndDialog(hDlg, wParam.convert())
            return true.toByte().toLong()
        }
        WM_CTLCOLORDLG -> {
            return GetStockObject(BLACK_BRUSH).rawValue.toLong()
        }
        else -> {
            return false.toByte().toLong()
        }
    }
}