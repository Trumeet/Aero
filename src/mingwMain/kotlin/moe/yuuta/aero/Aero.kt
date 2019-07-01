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

    if (hwnd == null) {
        MessageBoxA(null,
            "The system returns an empty dialog (${GetLastError()})",
            "Aero",
            (MB_OK or MB_ICONERROR).convert())
        return
    }

    val result = DwmEnableComposition(DWM_EC_ENABLECOMPOSITION) and 0xFFFF
    if (result != 0) {
        MessageBoxA(hwnd,
            "DwmEnableComposition() fail (0x${result.toString(16)})",
            "Aero",
            (MB_OK or MB_ICONERROR).convert())
    }
    ShowWindow(hwnd, WinCompat.nCmdShow)
    WinApi.msgLoop()
}

fun windowProc(hwnd: HWND?,
               uMsg: UINT,
               wParam: WPARAM,
               lParam: LPARAM): LRESULT {
    return when (uMsg.convert<Int>()) {
        WM_CREATE -> {
            WinApi.setAero10(hwnd)
            1
        }
        WM_DESTROY -> {
            PostQuitMessage(0)
            1
        }
        WM_ACTIVATE -> {
            WinApi.setAero7(hwnd)
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