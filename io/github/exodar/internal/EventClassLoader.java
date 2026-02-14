/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EventClassLoader extends ClassLoader
{
    private final MemoryJarClassLoader newClassLoader;

    public EventClassLoader(ClassLoader parent, MemoryJarClassLoader newClassloader)
    {
        super(parent);
        this.newClassLoader = newClassloader;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        try
        {
            return super.loadClass(name, resolve);
        }
        catch (ClassNotFoundException ignored)
        {
        }

        return newClassLoader.loadClassNoDelegation(name, resolve);
    }
}
