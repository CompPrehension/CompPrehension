package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/**
 * Правила о том, что какой слой имеет право видеть.
 * <p>
 * Смысл именно в направлении зависимостей: web знает про приложение, приложение — про
 * persistence, и никогда наоборот. Нарушения этих правил — то, из-за чего сейчас
 * ленивые коллекции догружаются в контроллерах и мапперах, порождая скрытые N+1.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class LayerBoundaryTest {

    /**
     * Репозиторий не должен знать про типы, которые отдаются наружу по HTTP.
     * Проекции для чтения — это отдельные типы, живущие рядом с репозиторием;
     * иначе изменение формата ответа API заставляет править JPQL.
     */
    @ArchTest
    static final ArchRule repositories_should_not_depend_on_web_dto =
            FreezingArchRule.freeze(noClasses()
                    .that().resideInAPackage(REPOSITORIES)
                    .should().dependOnClassesThat().resideInAPackage(DTO)
                    .as("repositories should not depend on web DTOs"));

    /**
     * Главное правило про N+1: JPA-сущность не выходит за границу сервиса.
     * За пределами транзакции у сущности ленивые связи либо взрываются, либо (при
     * open-in-view=true) молча делают дополнительные запросы.
     */
    @ArchTest
    static final ArchRule entities_should_not_leak_into_controllers =
            FreezingArchRule.freeze(noClasses()
                    .that().resideInAPackage(CONTROLLERS)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .as("JPA entities should not appear in controllers"));

    /** Контроллер ходит в БД только через слой приложения. */
    @ArchTest
    static final ArchRule controllers_should_not_use_repositories_directly =
            noClasses()
                    .that().resideInAPackage(CONTROLLERS)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .as("controllers should not access repositories directly");

    /**
     * DTO — плоский снимок данных. Ссылка на сущность внутри DTO означает, что
     * сериализация Jackson пойдёт по ленивому графу уже после закрытия транзакции.
     */
    @ArchTest
    static final ArchRule dtos_should_not_depend_on_entities =
            noClasses()
                    .that().resideInAPackage(DTO)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .as("DTOs should not reference JPA entities");

    /** Сущность — это данные, а не точка входа в бизнес-логику. */
    @ArchTest
    static final ArchRule entities_should_not_depend_on_services_or_repositories =
            noClasses()
                    .that().resideInAPackage(ENTITIES)
                    .should().dependOnClassesThat().resideInAnyPackage(SERVICES)
                    .orShould().dependOnClassesThat().resideInAPackage(REPOSITORIES)
                    .as("entities should not depend on services or repositories");

    /** Слой приложения не знает, что поверх него стоит HTTP. */
    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
            noClasses()
                    .that().resideInAnyPackage(SERVICES)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLERS)
                    .as("services should not depend on controllers");
}
