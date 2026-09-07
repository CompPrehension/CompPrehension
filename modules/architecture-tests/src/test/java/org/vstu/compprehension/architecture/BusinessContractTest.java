package org.vstu.compprehension.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.ROOT;
import static org.vstu.compprehension.architecture.ArchitecturePackages.STRATEGIES;

/**
 * Контракты предметного ядра — {@code Domain}, {@code Backend}, {@code AbstractStrategy}
 * и типы, объявленные внутри них, — не должны говорить о себе JPA-сущностями.
 * <p>
 * Сущность в сигнатуре означает, что реализация обязана получить объект, живущий в сессии
 * Hibernate, и что вызывающий обязан открыть транзакцию. Это привязывает предметную логику
 * к способу хранения и делает её нетестируемой без базы. {@code Backend} показывает, что
 * так можно не делать: он параметризован {@code BackendInput}/{@code BackendOutput}
 * и о персистентности не знает вовсе.
 * <p>
 * Правило смотрит только на сигнатуры (типы параметров, возвращаемые типы, поля),
 * разворачивая дженерики: {@code List<ResponseEntity>} — такое же нарушение, как
 * {@code ResponseEntity}. Тела default-методов не проверяются: это реализация, а не контракт.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class BusinessContractTest {

    @ArchTest
    static final ArchRule domain_contracts_should_not_expose_jpa_entities =
            classes()
                    .that(are_business_logic_contracts())
                    .should(not_expose_jpa_entities_in_signatures())
                    .as("domain contracts should not expose JPA entities");

    /**
     * Стратегии работают только с данными, которые им дали сервисы.
     * <p>
     * JPA-сущность у стратегии означала бы, что она обходит ленивый граф и порождает
     * скрытые N+1, а вызывающий обязан держать открытой транзакцию. Данные для стратегий
     * собирают сервисы фиксированным числом запросов и отдают отсоединёнными
     * (см. {@code org.vstu.compprehension.data}).
     * <p>
     * Правило строгое, без заморозки: долг здесь выбран до нуля, и возвращаться нельзя.
     */
    @ArchTest
    static final ArchRule strategies_should_not_use_jpa_entities =
            noClasses()
                    .that().resideInAnyPackage(STRATEGIES)
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
                    .as("strategies should not use JPA entities");

    /** Стратегия ходит за данными в сервисы, а не в репозитории. */
    @ArchTest
    static final ArchRule strategies_should_not_use_repositories =
            noClasses()
                    .that().resideInAnyPackage(STRATEGIES)
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .as("strategies should not access repositories");

    /**
     * Контракт — это интерфейс в предметных пакетах или тип, объявленный внутри такого
     * интерфейса (например, {@code Domain.InterpretSentenceResult}: формально это класс,
     * но возвращается методом интерфейса, то есть часть контракта).
     */
    private static DescribedPredicate<JavaClass> are_business_logic_contracts() {
        return new DescribedPredicate<>("являются контрактами предметного ядра") {
            @Override
            public boolean test(JavaClass clazz) {
                if (!clazz.getPackageName().startsWith(ROOT)) {
                    return false;
                }
                if (!clazz.getPackageName().contains(".businesslogic.domains")
                        && !clazz.getPackageName().contains(".businesslogic.backend")
                        && !clazz.getPackageName().contains(".businesslogic.strategies")) {
                    return false;
                }
                for (JavaClass c = clazz; c != null; c = c.getEnclosingClass().orElse(null)) {
                    if (c.isInterface()) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private static ArchCondition<JavaClass> not_expose_jpa_entities_in_signatures() {
        return new ArchCondition<>("not expose JPA entities in signatures") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    report(events, method, method.getReturnType(), "returns");
                    for (JavaType parameter : method.getParameterTypes()) {
                        report(events, method, parameter, "takes");
                    }
                }
                for (JavaField field : clazz.getFields()) {
                    report(events, field, field.getType(), "holds");
                }
            }
        };
    }

    private static void report(ConditionEvents events, JavaMember member, JavaType type, String verb) {
        for (JavaClass referenced : flatten(type)) {
            if (referenced.isAnnotatedWith(Entity.class)) {
                events.add(SimpleConditionEvent.violated(member, String.format(
                        "%s %s JPA entity %s in (%s.java:0)",
                        member.getFullName(), verb, referenced.getSimpleName(),
                        member.getOwner().getSimpleName())));
            }
        }
    }

    /** Разворачивает дженерики: из {@code List<ViolationEntity>} достаёт и List, и сущность. */
    private static Set<JavaClass> flatten(JavaType type) {
        Set<JavaClass> result = new LinkedHashSet<>();
        collect(type, result);
        return result;
    }

    private static void collect(JavaType type, Set<JavaClass> out) {
        out.add(type.toErasure());
        if (type instanceof JavaParameterizedType parameterized) {
            for (JavaType argument : parameterized.getActualTypeArguments()) {
                collect(argument, out);
            }
        }
    }
}
