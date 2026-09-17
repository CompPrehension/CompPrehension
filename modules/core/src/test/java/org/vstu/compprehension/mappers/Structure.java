package org.vstu.compprehension.mappers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

final class Structure {

    record Mismatch(String path, String detail) {
        @Override
        public String toString() {
            return path + ": " + detail;
        }
    }

    private Structure() {
    }

    static Map<String, Object> properties(Object object) {
        return properties(object, "", path -> false);
    }

    static Map<String, Object> properties(Object object, String path, Predicate<String> ignored) {
        Map<String, Object> result = new TreeMap<>();
        if (object == null) {
            return result;
        }
        Predicate<String> skip = name -> ignored.test(path.isEmpty() ? name : path + "." + name);
        Class<?> type = Proxy.isProxyClass(object.getClass()) ? object.getClass().getInterfaces()[0] : object.getClass();
        if (type.getName().startsWith("java.") || type.getName().startsWith("jdk.")) {
            return result;
        }
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (!skip.test(component.getName())) {
                    result.put(component.getName(), invoke(component.getAccessor(), object));
                }
            }
            return result;
        }
        for (Method method : type.getMethods()) {
            if (method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0 || method.getReturnType() == void.class
                    || method.isBridge() || method.isSynthetic()) {
                continue;
            }
            String name = RandomObjects.propertyName(method);
            if (name.equals(method.getName()) || skip.test(name)) {
                continue;
            }
            result.put(name, invoke(method, object));
        }
        for (Field field : type.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !skip.test(field.getName())) {
                result.putIfAbsent(field.getName(), read(field, object));
            }
        }
        return result;
    }

    private record Options(Predicate<String> ignored, Predicate<String> unordered) {
    }

    static List<Mismatch> mismatches(Object expected, Object actual, Predicate<String> ignored,
                                     Predicate<String> unordered) {
        List<Mismatch> result = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        if (expected != null && isSimple(expected)) {
            compare(expected, actual, "$", new Options(ignored, unordered), result, visited);
        } else {
            compareProperties(expected, actual, "", new Options(ignored, unordered), result, visited);
        }
        return result;
    }

    static void compare(Object expected, Object actual, String path, Predicate<String> ignored,
                        Predicate<String> unordered, List<Mismatch> out) {
        compare(expected, actual, path, new Options(ignored, unordered), out,
                Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static void compareProperties(Object expected, Object actual, String path, Options options,
                                          List<Mismatch> out, Set<Object> visited) {
        if (expected == null || actual == null || !visited.add(expected)) {
            return;
        }
        Map<String, Object> expectedProperties = properties(expected, path, options.ignored());
        Map<String, Object> actualProperties = properties(actual, path, options.ignored());
        for (Map.Entry<String, Object> entry : actualProperties.entrySet()) {
            if (!expectedProperties.containsKey(entry.getKey())) {
                continue;
            }
            String propertyPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
            compare(expectedProperties.get(entry.getKey()), entry.getValue(), propertyPath, options, out, visited);
        }
    }

    private static void compare(Object expected, Object actual, String path, Options options,
                                List<Mismatch> out, Set<Object> visited) {
        Predicate<String> ignored = options.ignored();
        if (ignored.test(path) || expected == null) {
            return;
        }
        expected = unwrap(expected);
        actual = unwrap(actual);
        if (actual == null) {
            out.add(new Mismatch(path, "expected " + describe(expected) + ", got null"));
            return;
        }
        if (Objects.equals(expected, actual)) {
            return;
        }
        if (expected instanceof Number a && actual instanceof Number b) {
            if (!sameNumber(a, b)) {
                out.add(new Mismatch(path, "expected " + a + ", got " + b));
            }
            return;
        }
        if (expected instanceof Enum<?> || actual instanceof Enum<?>) {
            String a = expected instanceof Enum<?> e ? e.name() : String.valueOf(expected);
            String b = actual instanceof Enum<?> e ? e.name() : String.valueOf(actual);
            if (!a.equals(b)) {
                out.add(new Mismatch(path, "expected " + describe(expected) + ", got " + describe(actual)));
            }
            return;
        }
        if (isSequence(expected) && isSequence(actual)) {
            List<Object> a = asList(expected);
            List<Object> b = asList(actual);
            if (a.size() != b.size()) {
                out.add(new Mismatch(path, "expected " + a.size() + " elements, got " + b.size()));
                return;
            }
            if (options.unordered().test(path)) {
                compareUnordered(a, b, path, options, out, visited);
                return;
            }
            for (int i = 0; i < a.size(); i++) {
                compare(a.get(i), b.get(i), path + "[" + i + "]", options, out, visited);
            }
            return;
        }
        if (expected instanceof Map<?, ?> a && actual instanceof Map<?, ?> b) {
            for (Map.Entry<?, ?> entry : a.entrySet()) {
                compare(entry.getValue(), b.get(entry.getKey()), path + "[" + entry.getKey() + "]",
                        options, out, visited);
            }
            return;
        }
        if (isSimple(expected) || isSimple(actual)) {
            out.add(new Mismatch(path, "expected " + describe(expected) + ", got " + describe(actual)));
            return;
        }
        compareProperties(expected, actual, path, options, out, visited);
    }

    private static void compareUnordered(List<Object> expected, List<Object> actual, String path, Options options,
                                         List<Mismatch> out, Set<Object> visited) {
        List<Object> unmatched = new ArrayList<>(actual);
        for (int i = 0; i < expected.size(); i++) {
            Object candidate = null;
            for (Object b : unmatched) {
                List<Mismatch> trial = new ArrayList<>();
                compare(expected.get(i), b, path + "[" + i + "]", options, trial,
                        Collections.newSetFromMap(new IdentityHashMap<>()));
                if (trial.isEmpty()) {
                    candidate = b;
                    break;
                }
            }
            if (candidate == null) {
                out.add(new Mismatch(path + "[" + i + "]", "no element matches " + describe(expected.get(i))));
                return;
            }
            unmatched.remove(candidate);
        }
    }

    private static Object unwrap(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

    private static boolean sameNumber(Number a, Number b) {
        if (isIntegral(a) && isIntegral(b)) {
            return a.longValue() == b.longValue();
        }
        return Math.abs(a.doubleValue() - b.doubleValue()) < 1e-6;
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }

    private static boolean isSequence(Object value) {
        return value instanceof Iterable<?> || value.getClass().isArray();
    }

    private static List<Object> asList(Object value) {
        List<Object> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(result::add);
        } else {
            for (int i = 0, n = Array.getLength(value); i < n; i++) {
                result.add(Array.get(value, i));
            }
        }
        return result;
    }

    static boolean isSimple(Object value) {
        return value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Character || value instanceof Enum<?> || value instanceof Date
                || value instanceof Temporal || value instanceof UUID || value instanceof Class<?>
                || value instanceof Map<?, ?> || isSequence(value)
                || value.getClass().getName().startsWith("java.");
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (isSimple(value)) {
            return value + " (" + value.getClass().getSimpleName() + ")";
        }
        return value.getClass().getSimpleName();
    }

    private static Object invoke(Method method, Object target) {
        try {
            method.setAccessible(true);
            Object value = method.invoke(target);
            if (value == null && RandomObjects.notNull(method)) {
                throw new IllegalStateException("@NotNull " + method.getDeclaringClass().getSimpleName() + "."
                        + method.getName() + "() returned null");
            }
            return value;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(method + " threw " + e.getCause(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
