/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

/**
 * Low Level Mouse Hook para detectar estado físico del mouse
 * independiente de Robot y LWJGL.
 *
 * Usa LLMHF_INJECTED flag para distinguir entre clicks físicos
 * y clicks inyectados por Robot/software.
 */
public class MouseHook {

    private static volatile boolean leftButtonDown = false;
    private static volatile boolean rightButtonDown = false;
    private static volatile boolean hookInstalled = false;
    private static HHOOK mouseHook = null;
    private static Thread hookThread = null;

    // Windows messages
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP = 0x0202;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_RBUTTONUP = 0x0205;
    private static final int WH_MOUSE_LL = 14;

    // LLMHF_INJECTED flag - indica que el evento fue inyectado (Robot, SendInput, etc)
    private static final int LLMHF_INJECTED = 0x01;
    private static final int LLMHF_LOWER_IL_INJECTED = 0x02;

    // Estructura MSLLHOOKSTRUCT para Low Level Mouse Hook
    public static class MSLLHOOKSTRUCT extends Structure {
        public POINT pt;
        public int mouseData;
        public int flags;  // Contiene LLMHF_INJECTED si es evento inyectado
        public int time;
        public Pointer dwExtraInfo;  // ULONG_PTR como Pointer

        public MSLLHOOKSTRUCT() {
            super();
        }

        public MSLLHOOKSTRUCT(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("pt", "mouseData", "flags", "time", "dwExtraInfo");
        }
    }

    // Interface extendida de User32 para SetWindowsHookEx
    public interface User32Ext extends StdCallLibrary {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        HHOOK SetWindowsHookEx(int idHook, LowLevelMouseProc lpfn, HMODULE hMod, int dwThreadId);
        boolean UnhookWindowsHookEx(HHOOK hhk);
        LRESULT CallNextHookEx(HHOOK hhk, int nCode, WPARAM wParam, LPARAM lParam);
        int GetMessage(MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);
        boolean TranslateMessage(MSG lpMsg);
        LRESULT DispatchMessage(MSG lpMsg);
        boolean PostThreadMessage(int idThread, int Msg, WPARAM wParam, LPARAM lParam);
    }

    // Interface para Low Level Mouse Hook callback
    public interface LowLevelMouseProc extends StdCallLibrary.StdCallCallback {
        LRESULT callback(int nCode, WPARAM wParam, LPARAM lParam);
    }

    private static LowLevelMouseProc mouseProc = null;
    private static int hookThreadId = 0;
    private static final int WM_QUIT = 0x0012;

    /**
     * Instala el hook de mouse
     */
    public static void install() {
        if (hookInstalled) return;

        hookThread = new Thread(() -> {
            try {
                hookThreadId = Kernel32.INSTANCE.GetCurrentThreadId();

                mouseProc = new LowLevelMouseProc() {
                    @Override
                    public LRESULT callback(int nCode, WPARAM wParam, LPARAM lParam) {
                        if (nCode >= 0) {
                            // Leer la estructura MSLLHOOKSTRUCT desde lParam
                            MSLLHOOKSTRUCT hookStruct = new MSLLHOOKSTRUCT(new Pointer(lParam.longValue()));

                            // Verificar si el evento fue inyectado (Robot, SendInput, etc)
                            boolean isInjected = (hookStruct.flags & LLMHF_INJECTED) != 0 ||
                                                 (hookStruct.flags & LLMHF_LOWER_IL_INJECTED) != 0;

                            int msg = wParam.intValue();

                            // Solo procesar eventos FÍSICOS (no inyectados)
                            if (!isInjected) {
                                if (msg == WM_LBUTTONDOWN) {
                                    leftButtonDown = true;
                                } else if (msg == WM_LBUTTONUP) {
                                    leftButtonDown = false;
                                } else if (msg == WM_RBUTTONDOWN) {
                                    rightButtonDown = true;
                                } else if (msg == WM_RBUTTONUP) {
                                    rightButtonDown = false;
                                }
                            }
                            // Los eventos inyectados se ignoran - no cambian el estado
                        }
                        return User32Ext.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam, lParam);
                    }
                };

                HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
                mouseHook = User32Ext.INSTANCE.SetWindowsHookEx(WH_MOUSE_LL, mouseProc, hMod, 0);

                if (mouseHook != null) {
                    hookInstalled = true;
                    System.out.println("[MouseHook] Installed successfully (using LLMHF_INJECTED flag)");

                    // Message pump - necesario para que el hook funcione
                    MSG msg = new MSG();
                    while (hookInstalled) {
                        int result = User32Ext.INSTANCE.GetMessage(msg, null, 0, 0);
                        if (result == -1 || result == 0) {
                            break;
                        }
                        User32Ext.INSTANCE.TranslateMessage(msg);
                        User32Ext.INSTANCE.DispatchMessage(msg);
                    }
                } else {
                    int error = Kernel32.INSTANCE.GetLastError();
                    System.out.println("[MouseHook] Failed to install hook, error: " + error);
                }
            } catch (Exception e) {
                System.out.println("[MouseHook] Error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "MouseHook-Thread");

        hookThread.setDaemon(true);
        hookThread.start();

        // Esperar a que el hook se instale
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Desinstala el hook
     */
    public static void uninstall() {
        hookInstalled = false;

        // Enviar WM_QUIT al thread del hook para que salga del message loop
        if (hookThreadId != 0) {
            User32Ext.INSTANCE.PostThreadMessage(hookThreadId, WM_QUIT, new WPARAM(0), new LPARAM(0));
        }

        if (mouseHook != null) {
            User32Ext.INSTANCE.UnhookWindowsHookEx(mouseHook);
            mouseHook = null;
            System.out.println("[MouseHook] Uninstalled");
        }

        if (hookThread != null) {
            try {
                hookThread.join(500);
            } catch (InterruptedException ignored) {}
            hookThread = null;
        }

        mouseProc = null;
        hookThreadId = 0;
    }

    /**
     * Ya no es necesario llamar esto - el hook detecta automáticamente
     * eventos inyectados usando LLMHF_INJECTED flag.
     * Se mantiene por compatibilidad pero no hace nada.
     */
    @Deprecated
    public static void markRobotClick() {
        // No-op - ya no es necesario, el flag LLMHF_INJECTED lo detecta automáticamente
    }

    /**
     * Retorna si el botón izquierdo está FÍSICAMENTE presionado por el usuario.
     * Ignora clicks de Robot/software automáticamente.
     */
    public static boolean isLeftButtonDown() {
        return leftButtonDown;
    }

    /**
     * Retorna si el botón derecho está FÍSICAMENTE presionado por el usuario.
     * Ignora clicks de Robot/software automáticamente.
     */
    public static boolean isRightButtonDown() {
        return rightButtonDown;
    }

    /**
     * Retorna si el hook está instalado y funcionando
     */
    public static boolean isInstalled() {
        return hookInstalled && mouseHook != null;
    }

    /**
     * Resetea el estado de los botones (útil si hay problemas de sincronización)
     */
    public static void resetState() {
        leftButtonDown = false;
        rightButtonDown = false;
    }
}
