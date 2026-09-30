package org.vstu.compprehension.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/** Правила о направлении зависимостей между слоями. */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class LayerBoundaryTest {

    /** Репозиторий не должен знать про web-DTO. */
    @ArchTest
    static final ArchRule repositories_should_not_depend_on_web_dto =
            noClasses()
                    .that().resideInAPackage(REPOSITORIES)
                    .should().dependOnClassesThat().resideInAPackage(DTO)
                    .as("repositories should not depend on web DTOs");

    /** JPA-сущность не выходит за пределы сервиса в контроллер. */
    @ArchTest
    static final ArchRule entities_should_not_leak_into_controllers =
            noClasses()
                    .that().resideInAPackage(CONTROLLERS)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .as("JPA entities should not appear in controllers");

    /** Контроллер ходит в БД только через слой приложения. */
    @ArchTest
    static final ArchRule controllers_should_not_use_repositories_directly =
            noClasses()
                    .that().resideInAPackage(CONTROLLERS)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .as("controllers should not access repositories directly");

    /** DTO не ссылается на JPA-сущности. */
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

    /** Spring Data репозитории использует только слой доступа к данным. */
    @ArchTest
    static final ArchRule spring_data_repositories_should_be_used_only_by_the_data_access_layer =
            noClasses()
                    .that().resideOutsideOfPackage(REPOSITORIES)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .as("only the data access layer should use Spring Data repositories");

    /** JPA-сущность не выходит за пределы слоя доступа к данным. */
    @ArchTest
    static final ArchRule entities_should_not_leak_out_of_the_data_access_layer =
            noClasses()
                    .that().resideOutsideOfPackages(REPOSITORIES, ENTITIES)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .as("JPA entities should not leak out of the data access layer");

    /** Задачи, которые пока сами ходят в данные, минуя слой приложения. */
    private static final Set<String> JOBS_WITH_DIRECT_DATA_ACCESS = Set.of(
            ROOT + ".jobs.bankloadtesting.BankLoadTestingJob",
            ROOT + ".jobs.metadatahealth.MetadataHealthJob"
    );

    /** Задача по расписанию только запускает сценарий слоя приложения: логика и данные живут там. */
    @ArchTest
    static final ArchRule jobs_should_not_access_repositories =
            noClasses()
                    .that().resideInAPackage(JOBS)
                    .and(are_not_listed_in(JOBS_WITH_DIRECT_DATA_ACCESS))
                    .should().dependOnClassesThat().resideInAPackage(REPOSITORIES)
                    .as("jobs should not access repositories");

    /** Сценарий зависит от порта, а не от того, как порт реализован. */
    @ArchTest
    static final ArchRule application_services_should_not_depend_on_adapters =
            noClasses()
                    .that().resideInAPackage(APPLICATION_SERVICES)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTERS)
                    .as("application services should depend on ports, not on adapters");

    /** Слой приложения не знает, что поверх него стоит HTTP. */
    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
            noClasses()
                    .that().resideInAnyPackage(SERVICES)
                    .should().dependOnClassesThat().resideInAPackage(CONTROLLERS)
                    .as("services should not depend on controllers");

    private static DescribedPredicate<JavaClass> are_not_listed_in(Set<String> frozen) {
        return new DescribedPredicate<>("are not on the not-yet-migrated list") {
            @Override
            public boolean test(JavaClass item) {
                return !frozen.contains(item.getName());
            }
        };
    }
}
