package org.vstu.compprehension.mappers;

import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class RandomObjects {

    static final class CannotGenerate extends RuntimeException {
        CannotGenerate(String message) {
            super(message);
        }
    }

    private static final int MAX_DEPTH = 4;

    private final Random random;
    private final Map<Class<?>, Function<RandomObjects, ?>> rules;
    private final boolean nullableAsNull;
    private final Deque<Class<?>> building = new ArrayDeque<>();

    RandomObjects(long seed, Map<Class<?>, Function<RandomObjects, ?>> rules, boolean nullableAsNull) {
        this.random = new Random(seed);
        this.rules = rules;
        this.nullableAsNull = nullableAsNull;
    }

    Object next(ResolvableType type) {
        return next(type, 0, "$");
    }

    Object next(ResolvableType type, Parameter parameter) {
        return next(type, 0, "$." + parameter.getName(), Slot.of(parameter));
    }

    <T> T next(Class<T> type) {
        return type.cast(next(ResolvableType.forClass(type)));
    }

    int nextInt(int bound) {
        return random.nextInt(bound);
    }

    private Object next(ResolvableType type, int depth, String path) {
        return next(type, depth, path, Slot.PLAIN);
    }

    private Object next(ResolvableType type, int depth, String path, Slot slot) {
        Class<?> raw = type.resolve();
        if (raw == null || (slot.nullable() && nullableAsNull)) {
            return null;
        }
        if (rules.containsKey(raw)) {
            return rules.get(raw).apply(this);
        }
        Object simple = simpleValue(raw, path);
        if (simple != null) {
            return simple;
        }
        if (raw.isEnum()) {
            Object[] constants = raw.getEnumConstants();
            return constants[random.nextInt(constants.length)];
        }
        if (raw.isArray()) {
            return array(type, depth, path);
        }
        if (Optional.class.equals(raw)) {
            return Optional.ofNullable(next(type.getGeneric(0), depth + 1, path));
        }
        if (Map.class.isAssignableFrom(raw)) {
            return map(type, depth, path);
        }
        if (Collection.class.isAssignableFrom(raw)) {
            return collection(type, raw, depth, path);
        }
        if (building.contains(raw) || (depth >= MAX_DEPTH && slot.nullable())) {
            if (slot.notNull()) {
                throw new CannotGenerate(path + ": " + raw.getSimpleName() + " refers back to itself through a"
                        + " @NotNull slot; register a value for it with MapperContract.value(...)");
            }
            return null;
        }
        if (raw.isInterface()) {
            return proxy(raw, depth, path);
        }
        if (Modifier.isAbstract(raw.getModifiers()) || raw.getName().startsWith("java.")) {
            throw new CannotGenerate(path + ": no way to build " + raw.getName()
                    + "; register a value for it with MapperContract.value(...)");
        }
        building.push(raw);
        try {
            return raw.isRecord() ? record(raw, type, depth, path) : bean(raw, type, depth, path);
        } finally {
            building.pop();
        }
    }

    private record Slot(boolean nullable, boolean notNull) {
        static final Slot PLAIN = new Slot(false, false);

        static Slot of(AnnotatedElement... elements) {
            return new Slot(RandomObjects.nullable(elements), RandomObjects.notNull(elements));
        }
    }

    private Object simpleValue(Class<?> raw, String path) {
        String name = path.substring(path.lastIndexOf('.') + 1);
        if (raw == String.class || raw == CharSequence.class || raw == Object.class) {
            return name + "#" + random.nextInt(1000);
        }
        if (raw == int.class || raw == Integer.class) return 1 + random.nextInt(1000);
        if (raw == long.class || raw == Long.class) return 1L + random.nextInt(1000);
        if (raw == short.class || raw == Short.class) return (short) (1 + random.nextInt(100));
        if (raw == byte.class || raw == Byte.class) return (byte) (1 + random.nextInt(100));
        if (raw == double.class || raw == Double.class) return random.nextInt(1000) / 4.0;
        if (raw == float.class || raw == Float.class) return random.nextInt(1000) / 4.0f;
        if (raw == boolean.class || raw == Boolean.class) return random.nextBoolean();
        if (raw == char.class || raw == Character.class) return (char) ('a' + random.nextInt(26));
        if (raw == BigDecimal.class) return BigDecimal.valueOf(random.nextInt(1000), 2);
        if (raw == BigInteger.class) return BigInteger.valueOf(random.nextInt(1000));
        if (raw == Date.class) return new Date(1_600_000_000_000L + random.nextInt(1_000_000) * 1000L);
        if (raw == Instant.class) return Instant.ofEpochSecond(1_600_000_000L + random.nextInt(1_000_000));
        if (raw == LocalDate.class) return LocalDate.of(2020, 1, 1).plusDays(random.nextInt(1000));
        if (raw == LocalDateTime.class) return LocalDateTime.of(2020, 1, 1, 0, 0).plusMinutes(random.nextInt(100_000));
        if (raw == UUID.class) return new UUID(random.nextLong(), random.nextLong());
        if (raw == Class.class) return String.class;
        return null;
    }

    private Object array(ResolvableType type, int depth, String path) {
        ResolvableType component = type.getComponentType();
        int size = size(depth, component);
        Object array = Array.newInstance(component.resolve(Object.class), size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, next(component, depth, path + "[" + i + "]"));
        }
        return array;
    }

    private Object collection(ResolvableType type, Class<?> raw, int depth, String path) {
        Collection<Object> result = Set.class.isAssignableFrom(raw) ? new LinkedHashSet<>() : new ArrayList<>();
        int size = size(depth, type.getGeneric(0));
        for (int i = 0; i < size; i++) {
            result.add(next(type.getGeneric(0), depth, path + "[" + i + "]"));
        }
        return result;
    }

    private Object map(ResolvableType type, int depth, String path) {
        Map<Object, Object> result = new LinkedHashMap<>();
        int size = Math.min(size(depth, type.getGeneric(0)), size(depth, type.getGeneric(1)));
        for (int i = 0; i < size; i++) {
            result.put(next(type.getGeneric(0), depth, path + ".key" + i),
                    next(type.getGeneric(1), depth, path + ".value" + i));
        }
        return result;
    }

    private int size(int depth) {
        return depth >= MAX_DEPTH ? 0 : 1 + random.nextInt(2);
    }

    private int size(int depth, ResolvableType element) {
        return building.contains(element.resolve(Object.class)) ? 0 : size(depth);
    }

    private Object proxy(Class<?> iface, int depth, String path) {
        Map<Object, Object> memo = new HashMap<>();
        Arrays.stream(iface.getMethods())
                .filter(m -> !m.isDefault() && m.getParameterCount() == 0 && m.getReturnType() != void.class)
                .sorted(Comparator.comparing(Method::getName))
                .forEach(m -> memo.put(m, next(
                        ResolvableType.forMethodReturnType(m, iface), depth + 1, path + "." + propertyName(m))));
        InvocationHandler handler = (self, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> self == args[0];
                    case "hashCode" -> System.identityHashCode(self);
                    default -> iface.getSimpleName() + "@" + path;
                };
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(self, method, args);
            }
            if (method.getReturnType() == void.class) {
                return null;
            }
            Object key = args == null ? method : new Call(method, args);
            synchronized (memo) {
                return memo.computeIfAbsent(key, k -> next(
                        ResolvableType.forMethodReturnType(method, iface), depth + 1, path + "." + propertyName(method)));
            }
        };
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    private record Call(Method method, Object[] args) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Call other && method.equals(other.method) && Arrays.deepEquals(args, other.args);
        }

        @Override
        public int hashCode() {
            return 31 * method.hashCode() + Arrays.deepHashCode(args);
        }
    }

    private Object record(Class<?> raw, ResolvableType type, int depth, String path) {
        RecordComponent[] components = raw.getRecordComponents();
        Class<?>[] paramTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Constructor<?> constructor = constructor(raw, paramTypes);
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            String slot = path + "." + components[i].getName();
            args[i] = next(ResolvableType.forConstructorParameter(constructor, i), depth + 1, slot,
                    Slot.of(parameters[i], components[i]));
        }
        return instantiate(constructor, args, path);
    }

    private Object bean(Class<?> raw, ResolvableType type, int depth, String path) {
        Constructor<?> constructor = Arrays.stream(raw.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new CannotGenerate(path + ": " + raw.getName() + " has no constructors"));
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            String slot = path + "." + parameters[i].getName();
            args[i] = next(ResolvableType.forConstructorParameter(constructor, i), depth + 1, slot,
                    Slot.of(parameters[i]));
        }
        Object bean = instantiate(constructor, args, path);
        for (Class<?> c = raw; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.isSynthetic()) {
                    continue;
                }
                String slot = path + "." + field.getName();
                try {
                    field.setAccessible(true);
                    if (field.get(bean) != null) {
                        continue;
                    }
                    field.set(bean, next(ResolvableType.forField(field, type), depth + 1, slot, Slot.of(field)));
                } catch (IllegalAccessException e) {
                    throw new CannotGenerate(slot + ": cannot set field: " + e.getMessage());
                }
            }
        }
        return bean;
    }

    private static Constructor<?> constructor(Class<?> raw, Class<?>[] paramTypes) {
        try {
            return raw.getDeclaredConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            throw new CannotGenerate(raw.getName() + " has no canonical constructor");
        }
    }

    private static Object instantiate(Constructor<?> constructor, Object[] args, String path) {
        Parameter[] parameters = constructor.getParameters();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null && notNull(parameters[i])) {
                throw new CannotGenerate(path + "." + parameters[i].getName() + ": nothing to put into a @NotNull slot"
                        + " of " + constructor.getDeclaringClass().getSimpleName()
                        + "; register a value for it with MapperContract.value(...)");
            }
        }
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw new CannotGenerate(path + ": " + constructor.getDeclaringClass().getSimpleName()
                    + " rejected generated arguments: " + e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new CannotGenerate(path + ": cannot instantiate " + constructor.getDeclaringClass().getName()
                    + ": " + e);
        }
    }

    private static boolean nullable(AnnotatedElement... elements) {
        return annotated(elements, "Nullable");
    }

    static boolean notNull(AnnotatedElement... elements) {
        return annotated(elements, "NotNull") || annotated(elements, "NonNull");
    }

    private static boolean annotated(AnnotatedElement[] elements, String annotationName) {
        for (AnnotatedElement element : elements) {
            if (has(element, annotationName)) {
                return true;
            }
            AnnotatedElement type = switch (element) {
                case Parameter parameter -> parameter.getAnnotatedType();
                case Field field -> field.getAnnotatedType();
                case RecordComponent component -> component.getAnnotatedType();
                case Method method -> method.getAnnotatedReturnType();
                default -> null;
            };
            if (type != null && has(type, annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean has(AnnotatedElement element, String annotationName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    static String propertyName(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2 && method.getReturnType() == boolean.class) {
            return decapitalize(name.substring(2));
        }
        return name;
    }

    private static String decapitalize(String name) {
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
