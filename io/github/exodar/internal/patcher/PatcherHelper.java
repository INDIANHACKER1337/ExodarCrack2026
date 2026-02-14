/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import io.github.exodar.internal.Canceler;
import io.github.exodar.internal.Thrower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import java.lang.reflect.Field;
import java.util.*;

// bunch of static methods called by c++ transformer
public class PatcherHelper
{
    private static Class<?>[] classesToTransform = null;
    private static Map<Class<?>, ClassModifier> classModifierMap = new HashMap<>();

    public static boolean init()
    {
        List<Class<?>> classes = new ArrayList<>();
        for (ClassModifier classModifier : io.github.exodar.internal.patcher.Patcher.classModifiers)
        {
            try
            {
                Class<?> classModifierClass = PatcherHelper.class.getClassLoader().loadClass(classModifier.name.replace('/', '.'));
                classes.add(classModifierClass);
                classModifierMap.put(classModifierClass, classModifier);
            } catch (ClassNotFoundException e)
            {
                return false;
            }
        }

        // Manually register EntityRenderer with EntityRendererClassModifier
        // This uses SafeClassWriter and custom patching for Optifine shader compatibility
        try {
            Class<?> entityRendererClass = PatcherHelper.class.getClassLoader().loadClass("net.minecraft.client.renderer.EntityRenderer");
            EntityRendererClassModifier entityRendererModifier = new EntityRendererClassModifier(null);
            classes.add(entityRendererClass);
            classModifierMap.put(entityRendererClass, entityRendererModifier);
            System.out.println("[PatcherHelper] Registered EntityRendererClassModifier for render events");
        } catch (ClassNotFoundException e) {
            System.out.println("[PatcherHelper] WARNING: Could not register EntityRenderer patcher: " + e.getMessage());
        }

        // DISABLED - FontRenderer patcher causes classloader issues with Lunar Client
        // AntiObfuscate for tab list works through Spoof module packet handling instead
        // try {
        //     Class<?> fontRendererClass = PatcherHelper.class.getClassLoader().loadClass("net.minecraft.client.gui.FontRenderer");
        //     FontRendererClassModifier fontRendererModifier = new FontRendererClassModifier();
        //     classes.add(fontRendererClass);
        //     classModifierMap.put(fontRendererClass, fontRendererModifier);
        //     System.out.println("[PatcherHelper] Registered FontRendererClassModifier for AntiObfuscate");
        // } catch (ClassNotFoundException e) {
        //     System.out.println("[PatcherHelper] WARNING: Could not register FontRenderer patcher: " + e.getMessage());
        // }

        // EntityPlayer patcher for AttackEvent (attackTargetEntityWithCurrentItem)
        try {
            Class<?> entityPlayerClass = PatcherHelper.class.getClassLoader().loadClass("net.minecraft.entity.player.EntityPlayer");
            EntityPlayerClassModifier entityPlayerModifier = new EntityPlayerClassModifier();
            classes.add(entityPlayerClass);
            classModifierMap.put(entityPlayerClass, entityPlayerModifier);
            System.out.println("[PatcherHelper] Registered EntityPlayerClassModifier for AttackEvent");
        } catch (ClassNotFoundException e) {
            System.out.println("[PatcherHelper] WARNING: Could not register EntityPlayer patcher: " + e.getMessage());
        }

        classesToTransform = classes.toArray(new Class<?>[0]);

        Set<String> excludeSet = getLunarClassLoaderExcludeSet();
        if (excludeSet != null)
            excludeSet.addAll(getEventHandlerClassNames());

        return true;
    }

    public static Class<?>[] getClassesToTransform()
    {
        return classesToTransform;
    }

    public static ClassModifier getClassModifier(Class<?> classToModify)
    {
        return classModifierMap.get(classToModify);
    }

    // This is used for lunar client trick, which consists of adding these classes to the ones excluded by the genesis classLoader
    private static Set<String> getEventHandlerClassNames()
    {
        Set<String> excluded = new HashSet<>(Arrays.asList(Canceler.class.getName(), Thrower.class.getName()));

        for (ClassModifier classModifier : io.github.exodar.internal.patcher.Patcher.classModifiers)
            excluded.addAll(classModifier.getEventHandlerClassesNames());

        // Also include manually registered class modifiers (EntityRenderer, etc.)
        for (ClassModifier classModifier : classModifierMap.values())
            excluded.addAll(classModifier.getEventHandlerClassesNames());

        return excluded;
    }

    // returns null if not on lunar
    private static Set<String> getLunarClassLoaderExcludeSet()
    {
        ClassLoader lunarClassLoader = Minecraft.class.getClassLoader();
        Class<?> lunarClassLoaderClass = lunarClassLoader.getClass();

        for (Field field : lunarClassLoaderClass.getDeclaredFields())
        {
            if (field.toGenericString().startsWith("private java.util.Set<java.lang.String> com.moonsworth.lunar.genesis."))
            {
                field.setAccessible(true);
                try {
                    return (Set<String>)field.get(lunarClassLoader);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }
}
