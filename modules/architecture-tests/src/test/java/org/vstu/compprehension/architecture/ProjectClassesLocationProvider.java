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

/** Отдаёт ArchUnit скомпилированные классы всех модулей проекта. */
public class ProjectClassesLocationProvider implements LocationProvider {

    /** Модули, чьи классы попадают в анализ. */
    static final List<String> ANALYZED_MODULES = List.of(
            "core-api",
            "core",
            "backends",
            "domains",
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

    /** Ищет каталог {@code modules} вверх от рабочего каталога. */
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
