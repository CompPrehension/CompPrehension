package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.LocationProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Отдаёт ArchUnit скомпилированные классы всех модулей проекта.
 * <p>
 * Импортировать по classpath нельзя: server и background-server пакуются
 * spring-boot-maven-plugin в fat-jar, где классы лежат под {@code BOOT-INF/classes/}
 * и по обычному имени пакета не находятся. Если положиться на classpath, правила про
 * контроллеры молча проверяют пустое множество и всегда зелёные — самый бесполезный
 * вариант отказа. Поэтому читаем {@code modules/../target/classes} напрямую.
 */
public class ProjectClassesLocationProvider implements LocationProvider {

    /**
     * Модули, чьи классы обязаны попасть в анализ. Список явный, а не «всё, что нашлось»,
     * чтобы удаление или переименование модуля ломало сборку, а не тихо сужало покрытие.
     */
    static final List<String> ANALYZED_MODULES = List.of(
            "core",
            "server",
            "background-server",
            "bkt",
            "strategies",
            "common",
            "common-spring",
            "external-api",
            "expr-domain-question-generator"
    );

    @Override
    public Set<Location> get(Class<?> testClass) {
        Path modulesDir = findModulesDir();
        Set<Location> locations = new LinkedHashSet<>();
        for (String module : ANALYZED_MODULES) {
            Path classes = modulesDir.resolve(module).resolve("target").resolve("classes");
            if (!Files.isDirectory(classes) || isEmpty(classes)) {
                throw new IllegalStateException(
                        "Module '" + module + "' has no compiled classes at " + classes + ". "
                                + "Architecture rules would silently pass on an incomplete class set. "
                                + "Build the whole reactor first: mvn -DskipTests install");
            }
            locations.add(Location.of(classes));
        }
        return locations;
    }

    private static boolean isEmpty(Path dir) {
        try (Stream<Path> entries = Files.walk(dir)) {
            return entries.noneMatch(p -> p.toString().endsWith(".class"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Ищет каталог {@code modules} вверх от рабочего каталога: под surefire это каталог
     * модуля, в IDE — обычно корень проекта.
     */
    private static Path findModulesDir() {
        for (Path dir = Paths.get("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("modules");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Cannot locate the 'modules' directory upwards from " + Paths.get("").toAbsolutePath());
    }
}
