package org.vstu.compprehension.mappers;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

final class MapperContract {

    interface Property {
        void check(MapperMethod mapping, long seed) throws Exception;
    }

    final class MapperMethod {
        private final Class<?> mapper;
        private final Object instance;
        private final Method method;

        private MapperMethod(Class<?> mapper, Object instance, Method method) {
            this.mapper = mapper;
            this.instance = instance;
            this.method = method;
        }

        String name() {
            return mapper.getSimpleName() + (qualified.contains(mapper) ? " " + signature(method) : "");
        }

        Object[] arguments(long seed) {
            return arguments(seed, false);
        }

        Object[] argumentsWithNulls(long seed) {
            return arguments(seed, true);
        }

        Object invoke(Object... args) throws Exception {
            try {
                method.setAccessible(true);
                return method.invoke(instance, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception cause) {
                    throw cause;
                }
                throw e;
            }
        }

        List<Structure.Mismatch> carriedProperties(Object[] sources, Object result) {
            List<Structure.Mismatch> out = new ArrayList<>();
            Parameter[] parameters = method.getParameters();
            Map<String, Object> properties = Structure.properties(result, "", ignoredIn(mapper));
            boolean sourceIsStructured = sources[0] != null && !Structure.isSimple(sources[0]);
            if (sourceIsStructured) {
                out.addAll(Structure.mismatches(sources[0], result,
                        ignoredIn(mapper).or(extraParameterNames(method)), unorderedIn(mapper)));
            }
            for (int i = sourceIsStructured ? 1 : 0; i < sources.length; i++) {
                String name = parameters[i].getName();
                if (properties.containsKey(name)) {
                    Structure.compare(sources[i], properties.get(name), name, ignoredIn(mapper), unorderedIn(mapper), out);
                }
            }
            return out;
        }

        List<Structure.Mismatch> differences(Object expected, Object actual) {
            return Structure.mismatches(expected, actual, ignoredIn(mapper), path -> false);
        }

        private Object[] arguments(long seed, boolean nullableAsNull) {
            RandomObjects random = new RandomObjects(seed, valuesFor(mapper), nullableAsNull);
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = random.next(ResolvableType.forMethodParameter(method, i, mapper), parameters[i]);
            }
            return args;
        }
    }

    private final List<String> packages;
    private final Map<Class<?>, Function<RandomObjects, ?>> values = new LinkedHashMap<>();
    private final Map<String, Map<Class<?>, Function<RandomObjects, ?>>> mapperValues = new LinkedHashMap<>();
    private final Map<Class<?>, Object> dependencies = new LinkedHashMap<>();
    private final Map<String, Set<String>> ignored = new LinkedHashMap<>();
    private final Map<String, Set<String>> unordered = new LinkedHashMap<>();
    private final Map<String, String> skipped = new LinkedHashMap<>();
    private int rounds = 20;

    private Set<Class<?>> mappers;
    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Set<Class<?>> qualified = new LinkedHashSet<>();

    private MapperContract(List<String> packages) {
        this.packages = packages;
    }

    static MapperContract forPackages(String... packages) {
        return new MapperContract(List.of(packages));
    }

    <T> MapperContract value(Class<T> type, Supplier<? extends T> supplier) {
        values.put(type, random -> supplier.get());
        return this;
    }

    <T> MapperContract value(Class<T> type, Function<RandomObjects, ? extends T> supplier) {
        values.put(type, supplier);
        return this;
    }

    <T> MapperContract value(String mapper, Class<T> type, Supplier<? extends T> supplier) {
        mapperValues.computeIfAbsent(mapper, m -> new LinkedHashMap<>()).put(type, random -> supplier.get());
        return this;
    }

    <T> MapperContract subtype(Class<T> type, Class<? extends T> concrete) {
        values.put(type, random -> random.next(concrete));
        return this;
    }

    <T> MapperContract dependency(Class<T> type, T instance) {
        dependencies.put(type, instance);
        return this;
    }

    MapperContract ignore(String mapper, String... properties) {
        ignored.computeIfAbsent(mapper, m -> new LinkedHashSet<>()).addAll(Arrays.asList(properties));
        return this;
    }

    MapperContract unordered(String mapper, String... properties) {
        unordered.computeIfAbsent(mapper, m -> new LinkedHashSet<>()).addAll(Arrays.asList(properties));
        return this;
    }

    MapperContract skip(String mapper, String reason) {
        skipped.put(mapper, reason);
        return this;
    }

    MapperContract rounds(int rounds) {
        this.rounds = rounds;
        return this;
    }

    Stream<DynamicNode> forEachMap(Property property) {
        return forEach("map", property);
    }

    Stream<DynamicNode> forEachApply(Property property) {
        return forEach("apply", property);
    }

    List<String> staleExceptions() {
        Set<String> known = new LinkedHashSet<>();
        mappers().forEach(m -> known.add(m.getSimpleName()));
        return Stream.of(mapperValues.keySet(), ignored.keySet(), unordered.keySet(), skipped.keySet())
                .flatMap(Set::stream)
                .filter(name -> !known.contains(name))
                .sorted()
                .toList();
    }

    private Stream<DynamicNode> forEach(String methodName, Property property) {
        return mappers().stream()
                .sorted(Comparator.comparing(Class::getSimpleName))
                .flatMap(mapper -> mappingsOf(mapper, methodName))
                .map(mapping -> DynamicTest.dynamicTest(mapping.name(), () -> run(mapping, property)));
    }

    private Stream<MapperMethod> mappingsOf(Class<?> mapper, String methodName) {
        List<Method> methods = Arrays.stream(mapper.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class && !Modifier.isStatic(m.getModifiers()))
                .filter(m -> !m.isBridge() && !m.isSynthetic() && !m.isDefault())
                .filter(m -> m.getName().equals("map") || m.getName().equals("apply"))
                .sorted(Comparator.comparing(Method::toGenericString))
                .toList();
        if (methods.size() > 1) {
            qualified.add(mapper);
        }
        return methods.stream()
                .filter(m -> m.getName().equals(methodName))
                .map(m -> new MapperMethod(mapper, null, m));
    }

    private void run(MapperMethod mapping, Property property) throws Exception {
        String name = mapping.mapper.getSimpleName();
        if (skipped.containsKey(name)) {
            abort(skipped.get(name));
        }
        MapperMethod live = new MapperMethod(mapping.mapper, instance(mapping.mapper), mapping.method);
        for (long seed = 1; seed <= rounds; seed++) {
            try {
                property.check(live, seed);
            } catch (RandomObjects.CannotGenerate e) {
                fail("seed " + seed + ": " + e.getMessage(), e);
            } catch (AssertionError e) {
                throw new AssertionError("seed " + seed + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new AssertionError("seed " + seed + ": mapper threw " + e, e);
            }
        }
    }

    private Map<Class<?>, Function<RandomObjects, ?>> valuesFor(Class<?> mapper) {
        Map<Class<?>, Function<RandomObjects, ?>> merged = new LinkedHashMap<>(values);
        merged.putAll(mapperValues.getOrDefault(mapper.getSimpleName(), Map.of()));
        return merged;
    }

    private Predicate<String> ignoredIn(Class<?> mapper) {
        return under(ignored.getOrDefault(mapper.getSimpleName(), Set.of()));
    }

    private Predicate<String> unorderedIn(Class<?> mapper) {
        return under(unordered.getOrDefault(mapper.getSimpleName(), Set.of()));
    }

    private static Predicate<String> extraParameterNames(Method method) {
        Set<String> names = new LinkedHashSet<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 1; i < parameters.length; i++) {
            names.add(parameters[i].getName());
        }
        return under(names);
    }

    private static Predicate<String> under(Set<String> properties) {
        return path -> properties.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + ".") || path.startsWith(p + "["));
    }

    private static String signature(Method method) {
        return method.getName() + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .toList();
    }

    private synchronized Set<Class<?>> mappers() {
        if (mappers == null) {
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(Mapping.class));
            Set<Class<?>> result = new LinkedHashSet<>();
            for (String pkg : packages) {
                for (var candidate : scanner.findCandidateComponents(pkg)) {
                    try {
                        Class<?> type = Class.forName(candidate.getBeanClassName());
                        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                            result.add(type);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            mappers = result;
        }
        return mappers;
    }

    private synchronized Object instance(Class<?> mapper) {
        Object existing = instances.get(mapper);
        if (existing != null) {
            return existing;
        }
        Constructor<?> constructor = Arrays.stream(mapper.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        Object[] args = new Object[constructor.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = resolve(mapper, ResolvableType.forConstructorParameter(constructor, i));
        }
        try {
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(args);
            instances.put(mapper, instance);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot instantiate " + mapper.getName() + ": " + e, e);
        }
    }

    private Object resolve(Class<?> mapper, ResolvableType parameter) {
        Class<?> raw = parameter.resolve(Object.class);
        for (Map.Entry<Class<?>, Object> dependency : dependencies.entrySet()) {
            if (raw.isAssignableFrom(dependency.getKey())) {
                return dependency.getValue();
            }
        }
        List<Class<?>> candidates = mappers().stream()
                .filter(m -> parameter.isAssignableFrom(ResolvableType.forClass(m)))
                .toList();
        if (candidates.size() == 1) {
            return instance(candidates.getFirst());
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException(mapper.getSimpleName() + " needs " + parameter
                    + ", but several mappers fit: " + candidates + "; pick one with MapperContract.dependency(...)");
        }
        if (raw.isInterface()) {
            return new RandomObjects(0, values, false).next(parameter);
        }
        throw new IllegalStateException(mapper.getSimpleName() + " needs " + parameter
                + "; provide it with MapperContract.dependency(...)");
    }
}
