package core.lua;

import java.lang.reflect.*;

public final class LuaToJava {
    private LuaToJava() {
    }

    public static Object construct(String className, Object[] args) {
        try {
            Class<?> type = Class.forName(className);

            Constructor<?> target = findConstructor(type, args);

            if (target == null)
                throw new RuntimeException(
                        "Java constructor not found: " + className
                );

            target.setAccessible(true);

            Object[] converted = convertArguments(
                    target.getParameterTypes(),
                    args
            );

            return target.newInstance(converted);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "LuaToJava.construct failed: " + className,
                    e
            );
        }
    }

    public static Object callStatic(
            String className,
            String method,
            Object[] args
    ) {
        try {
            Class<?> type = Class.forName(className);

            Method target = findMethod(
                    type,
                    method,
                    true,
                    args
            );

            if (target == null)
                throw new RuntimeException(
                        "Static Java method not found: " +
                                className + "." + method
                );

            target.setAccessible(true);

            Object[] converted = convertArguments(
                    target.getParameterTypes(),
                    args
            );

            return target.invoke(null, converted);

        } catch (Throwable e) {
            throw new RuntimeException(
                    "LuaToJava.callStatic failed: " +
                            className + "." + method,
                    e
            );
        }
    }

    public static Object invoke(
            Object object,
            String method,
            Object[] args
    ) {
        if (object == null)
            throw new RuntimeException(
                    "Cannot invoke method on null"
            );

        try {
            Method target = findMethod(
                    object.getClass(),
                    method,
                    false,
                    args
            );

            if (target == null)
                throw new RuntimeException(
                        "Java method not found: " +
                                object.getClass().getName() +
                                "." +
                                method
                );

            target.setAccessible(true);

            Object[] converted = convertArguments(
                    target.getParameterTypes(),
                    args
            );

            return target.invoke(object, converted);

        } catch (Throwable e) {
            throw fail("LuaToJava.invoke " + method, e);
        }
    }

    public static Object get(Object object, String field) {
        if (object == null)
            throw new RuntimeException(
                    "Cannot get field from null"
            );

        try {
            Field target = findField(
                    object.getClass(),
                    field
            );

            if (target == null)
                throw new RuntimeException(
                        "Java field not found: " + field
                );

            target.setAccessible(true);

            return target.get(object);

        } catch (Throwable e) {
            throw new RuntimeException(
                    "LuaToJava.get failed: " + field,
                    e
            );
        }
    }

    public static void set(
            Object object,
            String field,
            Object value
    ) {
        if (object == null)
            throw new RuntimeException(
                    "Cannot set field on null"
            );

        try {
            Field target = findField(
                    object.getClass(),
                    field
            );

            if (target == null)
                throw new RuntimeException(
                        "Java field not found: " + field
                );

            target.setAccessible(true);

            target.set(
                    object,
                    convertValue(
                            target.getType(),
                            value
                    )
            );

        } catch (Throwable e) {
            throw new RuntimeException(
                    "LuaToJava.set failed: " + field,
                    e
            );
        }
    }

    private static Constructor<?> findConstructor(
            Class<?> type,
            Object[] args
    ) {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterCount() != args.length)
                continue;

            if (compatible(
                    constructor.getParameterTypes(),
                    args
            ))
                return constructor;
        }

        for (Constructor<?> constructor :
                type.getDeclaredConstructors()) {

            if (constructor.getParameterCount() != args.length)
                continue;

            if (compatible(
                    constructor.getParameterTypes(),
                    args
            ))
                return constructor;
        }

        return null;
    }

    private static Method findMethod(
            Class<?> type,
            String name,
            boolean requireStatic,
            Object[] args
    ) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name))
                continue;

            if (requireStatic != Modifier.isStatic(
                    method.getModifiers()
            ))
                continue;

            if (method.getParameterCount() != args.length)
                continue;

            if (compatible(
                    method.getParameterTypes(),
                    args
            ))
                return method;
        }

        for (Method method : type.getDeclaredMethods()) {
            if (!method.getName().equals(name))
                continue;

            if (requireStatic != Modifier.isStatic(
                    method.getModifiers()
            ))
                continue;

            if (method.getParameterCount() != args.length)
                continue;

            if (compatible(
                    method.getParameterTypes(),
                    args
            ))
                return method;
        }

        return null;
    }

    private static Field findField(
            Class<?> type,
            String name
    ) {
        Class<?> current = type;

        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);

                if (Modifier.isPublic(field.getModifiers()))
                    return field;

                String className = current.getName();

                if (className.startsWith("java.")
                        || className.startsWith("javax.")
                        || className.startsWith("jdk.")
                        || className.startsWith("sun.")) {
                    current = current.getSuperclass();
                    continue;
                }

                return field;

            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        try {
            return type.getField(name);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static boolean compatible(
            Class<?>[] parameters,
            Object[] args
    ) {
        for (int i = 0; i < parameters.length; i++) {
            if (!compatible(parameters[i], args[i]))
                return false;
        }

        return true;
    }

    private static boolean compatible(
            Class<?> type,
            Object value
    ) {
        if (value == null)
            return !type.isPrimitive();

        if (value instanceof LuaFunction)
            return type.isInterface() && isFunctionalInterface(type);

        if (!type.isPrimitive())
            return type.isAssignableFrom(
                    value.getClass()
            );

        if (type == boolean.class)
            return value instanceof Boolean;

        if (type == byte.class)
            return value instanceof Number;

        if (type == short.class)
            return value instanceof Number;

        if (type == int.class)
            return value instanceof Number;

        if (type == long.class)
            return value instanceof Number;

        if (type == float.class)
            return value instanceof Number;

        if (type == double.class)
            return value instanceof Number;

        if (type == char.class)
            return value instanceof Character;

        return false;
    }

    private static Object[] convertArguments(
            Class<?>[] parameterTypes,
            Object[] args
    ) {
        Object[] converted = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            converted[i] = convertValue(
                    parameterTypes[i],
                    args[i]
            );
        }

        return converted;
    }

    private static Object convertValue(
            Class<?> type,
            Object value
    ) {
        if (value == null)
            return null;

        if (value instanceof LuaFunction function) {
            if (!isFunctionalInterface(type))
                throw new RuntimeException(
                        "LuaFunction cannot be converted to: " +
                                type.getName()
                );

            return Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[]{type},
                    (proxy, method, args) ->
                            JavaToLua.call(
                                    function.getReference(),
                                    args == null
                                            ? new Object[0]
                                            : args
                            )
            );
        }

        if (!type.isPrimitive())
            return value;

        if (type == boolean.class)
            return ((Boolean) value).booleanValue();

        if (type == byte.class)
            return ((Number) value).byteValue();

        if (type == short.class)
            return ((Number) value).shortValue();

        if (type == int.class)
            return ((Number) value).intValue();

        if (type == long.class)
            return ((Number) value).longValue();

        if (type == float.class)
            return ((Number) value).floatValue();

        if (type == double.class)
            return ((Number) value).doubleValue();

        if (type == char.class)
            return ((Character) value).charValue();

        return value;
    }

    private static boolean isFunctionalInterface(Class<?> type) {
        int count = 0;

        for (Method method : type.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())
                    && method.getDeclaringClass() != Object.class) {
                count++;
            }
        }

        return count == 1;
    }

    public static boolean hasField(
            Object object,
            String field
    ) {
        if (object == null)
            return false;

        return findField(
                object.getClass(),
                field
        ) != null;
    }


    private static RuntimeException fail(String action, Throwable e) {
        Throwable root = (e instanceof InvocationTargetException ite && ite.getCause() != null)
                ? ite.getCause()
                : e;
        return new RuntimeException(action + " -> " + root, root);
    }
}