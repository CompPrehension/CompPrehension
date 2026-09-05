package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/**
 * Правила про модели данных и мапперы.
 * <p>
 * Оба строгие, без заморозки: долг здесь выбран до нуля и возвращаться не должен.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class DataAndMappingTest {

    /**
     * Модели данных не зависят от слоя хранения.
     * <p>
     * Смысл {@code models.data} в том, что эти типы отсоединены от Hibernate: обращение
     * к любому их полю не выполняет запросов и не требует сессии. Ссылка на сущность —
     * пусть даже на такую, которая на самом деле является значением из json-колонки, —
     * это гарантию ломает и заставляет читателя проверять каждый раз.
     * <p>
     * Проверяются и сами {@code @Entity}, и всё, что названо {@code *Entity}: часть таких
     * классов на деле является значениями из json-колонок, но по имени этого не видно,
     * и разбираться в этом при каждом чтении не должен никто.
     * <p>
     * Перечисления из {@code models.entities.EnumData} правило не ловит: они не сущности
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
}
