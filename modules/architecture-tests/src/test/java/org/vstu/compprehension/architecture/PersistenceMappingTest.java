package org.vstu.compprehension.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static org.vstu.compprehension.architecture.ArchitecturePackages.*;

/** Правила о разметке JPA. */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class PersistenceMappingTest {

    private static final List<String> TO_ONE_ANNOTATIONS = List.of(
            "jakarta.persistence.ManyToOne",
            "jakarta.persistence.OneToOne"
    );

    private static final String HIBERNATE_TYPE = "org.hibernate.annotations.Type";
    private static final String JSON_TYPE = "io.hypersistence.utils.hibernate.type.json.JsonType";

    /** {@code @ManyToOne}/{@code @OneToOne} обязаны объявлять {@code fetch = LAZY} явно. */
    @ArchTest
    static final ArchRule to_one_associations_must_be_explicitly_lazy =
            fields()
                    .that(are_a_to_one_association())
                    .and(are_not_annotated_with_not_found())
                    .should(be_declared_lazy())
                    .as("@ManyToOne and @OneToOne associations should declare fetch = LAZY");

    /** Сущности лежат в одном месте, иначе правила выше дырявые. */
    @ArchTest
    static final ArchRule entities_should_reside_in_entities_package =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage(ENTITIES)
                    .as("JPA entities should reside in " + ENTITIES);

    /** То же самое для репозиториев (кроме {@code Fake*} — заглушек в expr-domain-question-generator). */
    @ArchTest
    static final ArchRule repositories_should_reside_in_repository_package =
            classes()
                    .that().areAssignableTo(Repository.class)
                    .and().resideInAPackage(ROOT + "..")
                    .and().haveSimpleNameNotStartingWith("Fake")
                    .should().resideInAPackage(REPOSITORIES)
                    .as("repositories should reside in " + REPOSITORIES);

    /** Запросы не собирают результат конструктором ({@code select new}) — только интерфейсными проекциями. */
    @ArchTest
    static final ArchRule queries_should_not_use_constructor_expressions =
            methods()
                    .that().areAnnotatedWith("org.springframework.data.jpa.repository.Query")
                    .should(not_use_constructor_expressions())
                    .as("queries should use interface projections, not constructor expressions");

    @ArchTest
    static final ArchRule json_columns_must_hold_serializable_values =
            fields()
                    .that(are_mapped_with_json_type())
                    .should(hold_only_serializable_project_classes())
                    .as("classes stored in JSON columns (and everything reachable from their fields) must implement Serializable, "
                            + "because hypersistence-utils clones them with Java serialization for dirty checking");

    private static DescribedPredicate<JavaField> are_mapped_with_json_type() {
        return new DescribedPredicate<>("are mapped with @Type(JsonType.class)") {
            @Override
            public boolean test(JavaField field) {
                return field.tryGetAnnotationOfType(HIBERNATE_TYPE)
                        .flatMap(annotation -> annotation.get("value"))
                        .map(PersistenceMappingTest::className)
                        .filter(JSON_TYPE::equals)
                        .isPresent();
            }
        };
    }

    private static ArchCondition<JavaField> hold_only_serializable_project_classes() {
        return new ArchCondition<>("hold only Serializable project classes") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                Set<JavaClass> visited = new LinkedHashSet<>();
                collectReachableProjectClasses(field, visited);
                for (JavaClass reachable : visited) {
                    if (!reachable.isAssignableTo(Serializable.class)) {
                        events.add(SimpleConditionEvent.violated(field, String.format(
                                "%s is stored as JSON but holds %s, which is not Serializable, in %s",
                                field.getFullName(), reachable.getName(), reachable.getSourceCodeLocation())));
                    }
                }
            }
        };
    }

    private static void collectReachableProjectClasses(JavaField field, Set<JavaClass> visited) {
        for (JavaClass involved : field.getType().getAllInvolvedRawTypes()) {
            JavaClass type = involved.isArray() ? involved.getBaseComponentType() : involved;
            if (!type.getPackageName().startsWith(ROOT) || type.isInterface() || type.isEnum() || !visited.add(type)) {
                continue;
            }
            for (JavaField nested : type.getAllFields()) {
                if (!nested.getModifiers().contains(JavaModifier.STATIC) && !nested.getModifiers().contains(JavaModifier.TRANSIENT)) {
                    collectReachableProjectClasses(nested, visited);
                }
            }
        }
    }

    private static String className(Object annotationValue) {
        return annotationValue instanceof JavaClass javaClass ? javaClass.getName() : String.valueOf(annotationValue);
    }

    private static ArchCondition<JavaMethod> not_use_constructor_expressions() {
        return new ArchCondition<>("not use select new") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.tryGetAnnotationOfType("org.springframework.data.jpa.repository.Query")
                        .flatMap(annotation -> annotation.get("value"))
                        .map(String::valueOf)
                        .filter(query -> query.toLowerCase().contains("select new"))
                        .ifPresent(query -> events.add(SimpleConditionEvent.violated(method, String.format(
                                "%s builds its result with a constructor expression, which binds by position; "
                                        + "use an interface projection instead, in %s",
                                method.getFullName(), method.getSourceCodeLocation()))));
            }
        };
    }

    /** {@code @NotFound}-связи всегда грузятся жадно, независимо от fetch. */
    private static DescribedPredicate<JavaField> are_not_annotated_with_not_found() {
        return new DescribedPredicate<>("не помечены @NotFound") {
            @Override
            public boolean test(JavaField field) {
                return !field.isAnnotatedWith("org.hibernate.annotations.NotFound");
            }
        };
    }

    private static DescribedPredicate<JavaField> are_a_to_one_association() {
        return new DescribedPredicate<>("declare a @ManyToOne or @OneToOne association") {
            @Override
            public boolean test(JavaField field) {
                return TO_ONE_ANNOTATIONS.stream().anyMatch(field::isAnnotatedWith);
            }
        };
    }

    private static ArchCondition<JavaField> be_declared_lazy() {
        return new ArchCondition<>("be declared with fetch = LAZY") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                for (String annotationName : TO_ONE_ANNOTATIONS) {
                    if (!field.isAnnotatedWith(annotationName)) {
                        continue;
                    }
                    JavaAnnotation<?> annotation = field.getAnnotations().stream()
                            .filter(a -> a.getRawType().getName().equals(annotationName))
                            .findFirst()
                            .orElseThrow();
                    Optional<Object> fetch = annotation.get("fetch");
                    boolean lazy = fetch.isPresent() && String.valueOf(fetch.get()).contains("LAZY");
                    if (!lazy) {
                        String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);
                        events.add(SimpleConditionEvent.violated(field, String.format(
                                "%s is eagerly fetched: @%s %s, in %s",
                                field.getFullName(),
                                simpleName,
                                fetch.map(v -> "declares fetch = " + shortEnumName(String.valueOf(v)))
                                        .orElse("declares no fetch, so JPA defaults to EAGER"),
                                field.getSourceCodeLocation())));
                    }
                }
            }
        };
    }

    private static String shortEnumName(String value) {
        int dot = value.lastIndexOf('.');
        return dot < 0 ? value : value.substring(dot + 1);
    }
}
