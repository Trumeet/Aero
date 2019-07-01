package moe.yuuta.aero

import kotlinx.cinterop.*
import platform.windows.*

fun main() {
    SetProcessDPIAware()
    val className = "Main Window Class"

    memScoped {
        val wc = cValue<WNDCLASSA> {
            lpfnWndProc = staticCFunction(::windowProc)
            this.hInstance = hInstance
            lpszClassName = className.cstr.ptr
        }
        RegisterClassA(wc.ptr)
        return@memScoped wc
    }

    val hwnd = CreateWindowExA(
        0.convert(),
        className,
        "", // Filling texts will get the white background of the label
        WS_OVERLAPPEDWINDOW.convert(),
        CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT,
        null,
        null,
        WinCompat.hInstance,
        null
    )

    val forceDialogEnv = WinApi.getenv("AERO_DEMO_FORCE_DIALOG")

    if (hwnd == null || (forceDialogEnv == "true")) {
        if (hwnd == null) {
            MessageBoxA(null,
                "The system returns an empty dialog (${GetLastError()})",
                "Aero",
                (MB_OK or MB_ICONERROR).convert())
        }
        memScoped {
            // Fallback
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
            return@memScoped DialogBoxIndirectParamW(WinCompat.hInstance,
                dlgImpl.ptr,
                null,
                staticCFunction(::dlgProc),
                0)
        }
    } else {
        ShowWindow(hwnd, WinCompat.nCmdShow)
        WinApi.msgLoop()
    }
}

fun windowProc(hwnd: HWND?,
               uMsg: UINT,
               wParam: WPARAM,
               lParam: LPARAM): LRESULT {
    return when (uMsg.convert<Int>()) {
        WM_CREATE -> {
            WinApi.setAero(hwnd)
            1
        }
        WM_DESTROY -> {
            PostQuitMessage(0)
            1
        }
        WM_PAINT -> {
            val ps = nativeHeap.alloc<PAINTSTRUCT>()
            val hdc = BeginPaint(hwnd, ps.ptr)
            FillRect(hdc,
                ps.rcPaint.ptr,
                GetStockObject(BLACK_BRUSH) as HBRUSH)
            EndPaint(hwnd, ps.ptr)
            nativeHeap.free(ps)
            1
        }
        else -> {
            DefWindowProcA(hwnd, uMsg, wParam, lParam)
        }
    }
}

fun dlgProc(hDlg: HWND?,
            message: UINT,
            wParam: WPARAM,
            lParam: LPARAM): INT_PTR {
    when (message.convert<Int>()) {
        WM_INITDIALOG -> {
            WinApi.setAero(hDlg)
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