/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import net.minecraft.client.Minecraft;

public class InventoryModuleHelper {
  private static String activeModule = null;
  private static long lastActiveTime = 0;
  private static final long TIMEOUT = 5000L;
  private static Object lastWorld = null;

  public static synchronized boolean tryAcquire(String moduleName) {
    // Reset on world change to prevent stale locks
    Minecraft mc = Minecraft.getMinecraft();
    Object currentWorld = (mc != null) ? mc.theWorld : null;
    if (currentWorld != lastWorld) {
      activeModule = null;
      lastActiveTime = 0;
      lastWorld = currentWorld;
    }

    long now = System.currentTimeMillis();
    if (activeModule == null || activeModule.equals(moduleName) || (now - lastActiveTime > TIMEOUT)) {
      activeModule = moduleName;
      lastActiveTime = now;
      return true;
    }
    return false;
  }

  public static synchronized void release(String moduleName) {
    if (moduleName.equals(activeModule)) {
      activeModule = null;
    }
  }

  public static synchronized void keepAlive(String moduleName) {
    if (moduleName.equals(activeModule)) {
      lastActiveTime = System.currentTimeMillis();
    }
  }

  public static synchronized boolean isActive(String moduleName) {
    return moduleName.equals(activeModule);
  }

  public static synchronized String getActiveModule() {
    return activeModule;
  }
}
