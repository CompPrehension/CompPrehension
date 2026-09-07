package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/** Правила про степень сцепления с фреймворком. */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class SpringCouplingTest {

    /** Инъекция в поля не даёт создать объект конструктором и прячет реальный список зависимостей. */
    @ArchTest
    static final ArchRule no_field_injection =
            fields()
                    .should().notBeAnnotatedWith(Autowired.class)
                    .as("dependencies should be injected via constructor, not into fields");

    /** Бизнес-логика не должна зависеть от spring-boot. */
    @ArchTest
    static final ArchRule business_logic_should_not_depend_on_spring_boot =
            noClasses()
                    .that().resideInAnyPackage(BUSINESS_LOGIC)
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.boot..")
                    .as("business logic should not depend on Spring Boot");

    /** Бизнес-логика не должна знать про HTTP. */
    @ArchTest
    static final ArchRule business_logic_should_not_depend_on_web_stack =
            noClasses()
                    .that().resideInAnyPackage(BUSINESS_LOGIC)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.http..",
                            "jakarta.servlet..")
                    .as("business logic should not depend on the web stack");
}
