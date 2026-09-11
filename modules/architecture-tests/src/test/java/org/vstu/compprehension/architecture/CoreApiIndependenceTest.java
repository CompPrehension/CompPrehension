package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Assertions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(locations = CoreApiClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class CoreApiIndependenceTest {

    @ArchTest
    static final ArchRule core_api_should_not_depend_on_spring =
            noClasses()
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .as("core-api should not depend on Spring");

    @ArchTest
    static final ArchRule core_api_should_not_depend_on_persistence_frameworks =
            noClasses()
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "io.hypersistence..")
                    .as("core-api should not depend on persistence frameworks");

    @ArchTest
    static final ArchRule core_api_should_not_depend_on_web_stack =
            noClasses()
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.servlet..",
                            "javax.servlet..")
                    .as("core-api should not depend on the web stack");

    @ArchTest
    static void core_api_classes_are_actually_imported(JavaClasses classes) {
        Assertions.assertTrue(classes.stream().count() > 0, () -> String.format(
                "No core-api classes were imported, so every rule here passes vacuously. Check %s.",
                CoreApiClassesLocationProvider.class.getSimpleName()));
    }
}
