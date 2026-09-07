package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/** Правила про модели данных и мапперы. */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class DataAndMappingTest {

    /** Модели данных не зависят от Entity-классов. */
    @ArchTest
    static final ArchRule data_models_should_not_depend_on_persistence =
            noClasses()
                    .that().resideInAPackage(DATA_MODELS)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
                    .as("data models should not depend on JPA entities");

    /** Маппер зависит только от других мапперов. */
    @ArchTest
    static final ArchRule mappers_should_not_depend_on_repositories_or_services =
            noClasses()
                    .that().resideInAPackage(MAPPERS)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .orShould().dependOnClassesThat().areAnnotatedWith(Service.class)
                    .as("mappers should depend on nothing but other mappers");

    /** Маппер не должен и сам оказаться сущностью или репозиторием. */
    @ArchTest
    static final ArchRule mappers_should_not_be_entities =
            noClasses()
                    .that().resideInAPackage(MAPPERS)
                    .should().beAnnotatedWith(Entity.class)
                    .as("mappers should not be JPA entities");

    /** Маппинг Entity → Data может происходить только в data-репозиториях. */
    @ArchTest
    static final ArchRule entity_to_data_mapping_should_live_in_the_data_access_layer =
            classes()
                    .that().resideOutsideOfPackages(REPOSITORIES, ENTITIES)
                    .should(notConvertEntitiesIntoDataModels())
                    .as("only the data access layer should map JPA entities to data models");

    private static ArchCondition<JavaClass> notConvertEntitiesIntoDataModels() {
        return new ArchCondition<>("not depend on both JPA entities and data models") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean touchesEntities = false;
                boolean touchesDataModels = false;
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String target = dependency.getTargetClass().getPackageName();
                    touchesEntities |= target.equals(ROOT + ".entities") || target.startsWith(ROOT + ".entities.");
                    touchesDataModels |= target.equals(ROOT + ".data") || target.startsWith(ROOT + ".data.");
                }
                if (touchesEntities && touchesDataModels) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getName() + " depends on both JPA entities and data models"));
                }
            }
        };
    }
}
