package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Assertions;

import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/** Страховка от того, что правило перестанет находить классы своего слоя и станет всегда зелёным. */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class ImportCoverageTest {

    @ArchTest
    static void every_layer_under_rules_is_actually_imported(JavaClasses classes) {
        assertNotEmpty(classes, "controllers", c -> c.getPackageName().contains(".controllers"));
        assertNotEmpty(classes, "repositories", c -> c.getPackageName().contains(".repositories."));
        assertNotEmpty(classes, "data access", c -> c.getPackageName().contains(".repositories.data"));
        assertNotEmpty(classes, "data models", c -> c.getPackageName().equals(ROOT + ".data")
                || c.getPackageName().startsWith(ROOT + ".data."));
        assertNotEmpty(classes, "services", c -> c.getPackageName().contains(".service"));
        assertNotEmpty(classes, "web DTOs", c -> c.getPackageName().contains(".dto"));
        assertNotEmpty(classes, "JPA entities", c -> c.isAnnotatedWith(Entity.class));
        assertNotEmpty(classes, "mappers", c -> c.getPackageName().contains(".mappers"));
        assertNotEmpty(classes, "strategies", c -> c.getPackageName().contains(".strategies"));
    }

    private static void assertNotEmpty(
            JavaClasses classes,
            String layer,
            java.util.function.Predicate<com.tngtech.archunit.core.domain.JavaClass> predicate) {
        long count = classes.stream().filter(predicate).count();
        Assertions.assertTrue(count > 0, () -> String.format(
                "No classes were imported for the '%s' layer, so every architecture rule about it "
                        + "passes vacuously. Check %s and the module packaging.",
                layer, ProjectClassesLocationProvider.class.getSimpleName()));
    }
}
