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

/**
 * Правила про модели данных и мапперы.
 * <p>
 * Все строгие. Правило про место маппинга Entity → Data какое-то время было заморожено:
 * перенос шёл постепенно, сервис за сервисом. Список исключений исчерпан, заморозка снята —
 * новое нарушение теперь падает сразу.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class DataAndMappingTest {

    /**
     * Модели данных не зависят от слоя хранения.
     * <p>
     * Смысл {@code data} в том, что эти типы отсоединены от Hibernate: обращение
     * к любому их полю не выполняет запросов и не требует сессии. Ссылка на сущность —
     * пусть даже на такую, которая на самом деле является значением из json-колонки, —
     * это гарантию ломает и заставляет читателя проверять каждый раз.
     * <p>
     * Проверяются и сами {@code @Entity}, и всё, что названо {@code *Entity}: часть таких
     * классов на деле является значениями из json-колонок, но по имени этого не видно,
     * и разбираться в этом при каждом чтении не должен никто.
     * <p>
     * Перечисления из {@code entities.EnumData} правило не ловит: они не сущности
     * и не названы {@code *Entity}, хотя лежат в неудачном пакете. Переезд — отдельная задача.
     */
    @ArchTest
    static final ArchRule data_models_should_not_depend_on_persistence =
            noClasses()
                    .that().resideInAPackage(DATA_MODELS)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
                    .as("data models should not depend on JPA entities");

    /**
     * Маппер обязан быть очевидным: на вход данные, на выход данные.
     * <p>
     * Ни репозиториев, ни сервисов. Всё, чего нет во входных данных, обязана дать
     * вызывающая сторона — иначе маппер незаметно ходит в базу, и по его сигнатуре
     * этого не видно. Зависеть маппер может только от других мапперов.
     */
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

    /**
     * Превращать сущности в модели данных имеет право только слой доступа к данным.
     * <p>
     * Класс, который видит и то и другое, занимается переносом между ними. А перенос
     * корректен только там, где известна форма выборки: сама сущность о ней не знает,
     * и код, написанный под один запрос, под другим запросом молча отдаёт полупустой
     * результат. Ровно так {@code toData(InteractionEntity)} оставлял вопрос пустым,
     * когда его звали из шага цепочки вспомогательных вопросов, — и домен падал на NPE.
     * <p>
     * Перечисления из {@code entities.EnumData} не считаются: они лежат в этом
     * пакете по недоразумению и сущностями не являются. Сами сущности тоже исключены —
     * они ссылаются на значения json-колонок из {@code data} по определению.
     * <p>
     * Интерфейсы Spring Data исключены вместе со всем {@code repositories}: они и
     * есть слой хранения, а перечисления вроде статуса заявки приходят к ним параметрами
     * запроса.
     */
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
                    boolean inEntities = target.equals(ROOT + ".entities") || target.startsWith(ROOT + ".entities.");
                    boolean inEnumData = target.equals(ROOT + ".entities.EnumData") || target.startsWith(ROOT + ".entities.EnumData.");
                    touchesEntities |= inEntities && !inEnumData;
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
