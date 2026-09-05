package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/**
 * Правила про степень сцепления с фреймворком.
 * <p>
 * Цель не в том, чтобы «когда-нибудь уйти со Spring» — этого не случится. Цель в том,
 * чтобы бизнес-логику можно было завести обычным {@code new} в тесте, без поднятия
 * контекста, и чтобы тесты из-за этого шли секунды, а не минуты.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class SpringCouplingTest {

    /**
     * Инъекция в поля не даёт создать объект конструктором и прячет реальный список
     * зависимостей. В проекте уже есть классы с конструкторной инъекцией
     * (например, ExerciseService) — правило фиксирует это как норму.
     */
    @ArchTest
    static final ArchRule no_field_injection =
            FreezingArchRule.freeze(fields()
                    .should().notBeAnnotatedWith(Autowired.class)
                    .as("dependencies should be injected via constructor, not into fields"));

    /**
     * Бизнес-логика зависит от spring-context (аннотации, транзакции), но не должна
     * зависеть от spring-boot: автоконфигурация, стартеры и properties — это забота
     * запускающих модулей.
     */
    @ArchTest
    static final ArchRule business_logic_should_not_depend_on_spring_boot =
            noClasses()
                    .that().resideInAnyPackage(BUSINESS_LOGIC)
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.boot..")
                    .as("business logic should not depend on Spring Boot");

    /**
     * Бизнес-логика не должна знать про HTTP. Сейчас core тянет spring-web —
     * правило показывает, где именно, чтобы это можно было расшить постепенно.
     */
    @ArchTest
    static final ArchRule business_logic_should_not_depend_on_web_stack =
            FreezingArchRule.freeze(noClasses()
                    .that().resideInAnyPackage(BUSINESS_LOGIC)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.web..",
                            "org.springframework.http..",
                            "jakarta.servlet..")
                    .as("business logic should not depend on the web stack"));
}
