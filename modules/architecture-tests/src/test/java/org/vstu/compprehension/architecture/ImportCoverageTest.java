package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Assertions;

import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/**
 * Страховка от главного способа обесценить архитектурные тесты: правило перестаёт
 * находить классы и с тех пор всегда зелёное.
 * <p>
 * Так уже было — server пакуется в spring-boot fat-jar, классы лежат под
 * {@code BOOT-INF/classes/}, и импорт по classpath не видел ни одного контроллера.
 * Правила про контроллеры при этом «проходили». Здесь проверяется, что каждый слой,
 * на который есть правила, реально попал в анализ.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class ImportCoverageTest {

    @ArchTest
    static void every_layer_under_rules_is_actually_imported(JavaClasses classes) {
        assertNotEmpty(classes, "controllers", c -> c.getPackageName().contains(".controllers"));
        assertNotEmpty(classes, "repositories", c -> c.getPackageName().contains(".models.repository"));
        assertNotEmpty(classes, "services", c -> c.getPackageName().endsWith(".Service")
                || c.getPackageName().contains(".service"));
        assertNotEmpty(classes, "web DTOs", c -> c.getPackageName().contains(".dto"));
        assertNotEmpty(classes, "JPA entities", c -> c.isAnnotatedWith(Entity.class));
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
