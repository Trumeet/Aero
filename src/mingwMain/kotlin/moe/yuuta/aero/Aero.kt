package moe.yuuta.aero

import aero.*
import kotlinx.cinterop.*
import platform.windows.*

fun main() {
    SetProcessDPIAware()
    val hInstance = GetModuleHandleA(null)

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
        hInstance,
        null
    )

    val forceDialogEnv = memScoped {
        val buffer = nativeHeap.allocArray<CHARVar>(MAX_PATH)
        GetEnvironmentVariableA("AERO_DEMO_FORCE_DIALOG",
            buffer,
            MAX_PATH
        )
        val env = buffer.toKString()
        nativeHeap.free(buffer)
        return@memScoped env
    }

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
            return@memScoped DialogBoxIndirectParamW(hInstance,
                dlgImpl.ptr,
                null,
                staticCFunction(::dlgProc),
                0)
        }
    } else {
        val nCmdShow = memScoped {
            val startUpInfo = nativeHeap.alloc<_STARTUPINFOA>()
            GetStartupInfoA(startUpInfo.ptr)
            val nCmdShow = startUpInfo.wShowWindow
            nativeHeap.free(startUpInfo)
            return@memScoped nCmdShow.convert<Int>()
        }
        ShowWindow(hwnd, nCmdShow)
        msgLoop()
    }
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

fun windowProc(hwnd: HWND?,
               uMsg: UINT,
               wParam: WPARAM,
               lParam: LPARAM): LRESULT {
    return when (uMsg.convert<Int>()) {
        WM_CREATE -> {
            setAero(hwnd)
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
            setAero(hDlg)
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

fun setAero(hwnd: HWND?) {
    val hUser = GetModuleHandleW("user32.dll") ?: return
    val setWindowCompositionAttribute = GetProcAddress(hUser,
        "SetWindowCompositionAttribute")
            as pfnSetWindowCompositionAttribute? ?: return
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
        setWindowCompositionAttribute(hwnd, data.ptr)
    }
}