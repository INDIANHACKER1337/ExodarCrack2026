/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {
    private final static Map<Class<?>, Set<RegisteredHandler>> handlers = new ConcurrentHashMap<>();
    // Cached sorted arrays for fast iteration (no allocation per event)
    private final static Map<Class<?>, RegisteredHandler[]> sortedHandlersCache = new ConcurrentHashMap<>();
    private final static ExecutorService asyncExecutor = Executors.newCachedThreadPool();

    public static void register(Object listener) {
        Class<?> clazz = listener.getClass();

        // walk up inheritance tree
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {

                // Check for @Subscribe or @Listener annotation (for cc.unknown compatibility)
                boolean hasSubscribe = method.isAnnotationPresent(Subscribe.class);
                boolean hasListener = method.isAnnotationPresent(Listener.class);
                if (!hasSubscribe && !hasListener) continue;
                if (method.getParameterCount() != 1) continue;

                Class<?> eventClass = method.getParameterTypes()[0];

                // Get priority from @Subscribe if present, otherwise use default
                EventPriority priority = EventPriority.NORMAL;
                boolean ignoreCancelled = false;
                if (hasSubscribe) {
                    Subscribe subscribe = method.getAnnotation(Subscribe.class);
                    priority = subscribe.priority();
                    ignoreCancelled = subscribe.ignoreCancelled();
                }

                RegisteredHandler handler = new RegisteredHandler(
                        listener,
                        method,
                        priority,
                        ignoreCancelled
                );

                handlers.computeIfAbsent(eventClass, k -> ConcurrentHashMap.newKeySet())
                        .add(handler);

                // Invalidate cache for this event type
                sortedHandlersCache.remove(eventClass);
            }
            clazz = clazz.getSuperclass();
        }
    }

    public static void unregister(Object listener) {
        handlers.values().forEach(set ->
            set.removeIf(handler -> handler.listener == listener));
        // Invalidate all caches on unregister
        sortedHandlersCache.clear();
    }

    public static void unregisterAll() {
        handlers.clear();
        sortedHandlersCache.clear();
    }

    public static <T extends Event> T post(T event) {
        Class<?> eventClass = event.getClass();

        // Use cached sorted array for fast iteration
        RegisteredHandler[] sortedHandlers = sortedHandlersCache.get(eventClass);
        if (sortedHandlers == null) {
            Set<RegisteredHandler> eventHandlers = handlers.get(eventClass);
            if (eventHandlers == null || eventHandlers.isEmpty()) return event;

            // Build and cache sorted array
            sortedHandlers = eventHandlers.stream()
                .sorted()
                .toArray(RegisteredHandler[]::new);
            sortedHandlersCache.put(eventClass, sortedHandlers);
        }

        // Fast array iteration - no stream, no allocations
        for (RegisteredHandler handler : sortedHandlers) {
            if (!event.isCancelled() || handler.ignoreCancelled) {
                handler.invoke(event);
            }
        }

        return event;
    }
    
    private static class RegisteredHandler implements Comparable<RegisteredHandler> {
        final Object listener;
        final java.lang.reflect.Method method;
        final EventPriority priority;
        final boolean ignoreCancelled;
        
        RegisteredHandler(Object listener, java.lang.reflect.Method method, 
                        EventPriority priority, boolean ignoreCancelled) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.method.setAccessible(true);
        }

        void invoke(Event event) {
            try {
                method.invoke(listener, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public int compareTo(RegisteredHandler other) {
            return other.priority.getValue() - priority.getValue();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RegisteredHandler)) return false;
            RegisteredHandler other = (RegisteredHandler) obj;
            return listener == other.listener && method.equals(other.method);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(listener, method);
        }
    }
}