package org.vstu.compprehension.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.mappers.Mapping;
import org.vstu.compprehension.mappers.UpdateMapper;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.vstu.compprehension.architecture.ArchitecturePackages.DATA_ACCESS;
import static org.vstu.compprehension.architecture.ArchitecturePackages.DATA_MAPPERS;
import static org.vstu.compprehension.architecture.ArchitecturePackages.DTO;
import static org.vstu.compprehension.architecture.ArchitecturePackages.FRONTEND_MAPPERS;
import static org.vstu.compprehension.architecture.ArchitecturePackages.ROOT;
import static org.vstu.compprehension.architecture.ArchitecturePackages.SERVICES;

/**
 * Соглашения о маппингах — полный свод в {@code org.vstu.compprehension.mappers.package-info}.
 * <p>
 * Коротко: маппер — это бин, реализующий {@link Mapping}: либо готовый {@link Mapper}
 * (цель создаётся) или {@link UpdateMapper} (цель уже есть и меняется на месте), либо
 * собственный интерфейс, если источников больше одного. Без статики, без null в предмете
 * и результате, без коллекций и без зависимостей, кроме других мапперов. Наружу маппер
 * виден только своим интерфейсом, а класс-реализация не публичен.
 */
@AnalyzeClasses(locations = ProjectClassesLocationProvider.class, importOptions = ImportOption.DoNotIncludeTests.class)
public class MapperConventionTest {

    /**
     * Классы слоя доступа к данным, которые всё ещё держат маппинг внутри себя.
     * <p>
     * Пуст: слой доступа к данным переведён на мапперы целиком. Список оставлен как
     * посадочная полоса для случая, когда правило придётся включать над чужим кодом;
     * он может сокращаться и не может пополняться.
     */
    private static final Set<String> DATA_REPOSITORIES_WITH_INLINE_MAPPING = Set.of();

    /**
     * Классы, которые собирают DTO сами, минуя маппер.
     * <p>
     * Те же условия: только сокращается.
     */
    private static final Set<String> DTO_BUILDERS_OUTSIDE_MAPPERS = Set.of(
            ROOT + ".businesslogic.domains.DecisionTreeSupQuestionHelper",
            ROOT + ".businesslogic.domains.ProgrammingLanguageExpressionDomain",
            ROOT + ".frontend.CourseFrontendServiceImpl",
            ROOT + ".frontend.ExerciseAttemptServiceImpl",
            ROOT + ".frontend.ExerciseFrontendServiceImpl",
            ROOT + ".frontend.ReferenceDataFacadeImpl",
            ROOT + ".frontend.SurveyFrontendServiceImpl",
            ROOT + ".services.CourseDataServiceImpl",
            ROOT + ".services.ExerciseDataServiceImpl",
            ROOT + ".services.ExercisePermissionDataServiceImpl",
            ROOT + ".services.questionbank.QuestionBankImpl"
    );

    /**
     * Обитатели пакетов маппинга, которые мапперами не являются.
     * <p>
     * Те же условия: только сокращается.
     */
    private static final Set<String> NON_MAPPERS_IN_MAPPER_PACKAGES = Set.of(
            ROOT + ".frontend.mappers.LegacyDtoMappers"
    );

    // ------------------------------------------------------------------ форма маппера

    /** Всё, что названо маппером, обязано им быть. */
    @ArchTest
    static final ArchRule mappers_should_implement_a_mapping_interface =
            classes()
                    .that(are_named_like_a_mapper())
                    .and().resideInAPackage(ROOT + "..")
                    .and().areNotInterfaces()
                    .should().beAssignableTo(Mapping.class)
                    .as("classes named *Mapper or *MapperImpl should be a Mapping");

    /** Собственный интерфейс маппера тоже обязан быть виден правилам. */
    @ArchTest
    static final ArchRule mapping_interfaces_should_extend_the_marker =
            classes()
                    .that().areInterfaces()
                    .and().resideInAnyPackage(DATA_MAPPERS, FRONTEND_MAPPERS)
                    .should().beAssignableTo(Mapping.class)
                    .as("a mapper interface should extend Mapping, or the rules stop seeing it");

    /** И наоборот: маппер обязан называться маппером и лежать там, где мапперам место. */
    @ArchTest
    static final ArchRule mappers_should_be_named_and_placed_consistently =
            classes()
                    .that(are_mapper_implementations())
                    .should(be_named_and_placed_like_a_mapper())
                    .as("mapper implementations should be named *Mapper or *MapperImpl and reside in "
                            + DATA_MAPPERS + " or " + FRONTEND_MAPPERS);

    /** А в пакетах маппинга не оседает ничего постороннего. */
    @ArchTest
    static final ArchRule mapper_packages_should_hold_only_mappings =
            classes()
                    .that().resideInAnyPackage(DATA_MAPPERS, FRONTEND_MAPPERS)
                    .and(are_not_listed_in(NON_MAPPERS_IN_MAPPER_PACKAGES))
                    .should(be_a_mapping())
                    .as("mapper packages should hold mapping interfaces and their implementations");

    /** Маппер — бин: его должно быть можно внедрить и подменить, а не только позвать. */
    @ArchTest
    static final ArchRule mappers_should_be_spring_components =
            classes()
                    .that(are_mapper_implementations())
                    .should().beAnnotatedWith(Component.class)
                    .as("mappers should be Spring components");

    /** Статический маппинг — ровно то, от чего уходим: его не внедрить и не подменить. */
    @ArchTest
    static final ArchRule mappers_should_not_declare_static_methods =
            classes()
                    .that(are_mapper_implementations())
                    .should(declare_no_static_methods())
                    .as("mappers should not declare static methods");

    /**
     * Маппер не умеет ничего, кроме своего маппинга.
     * <p>
     * Публичных методов у него столько, сколько направлений он закрывает, и зовутся они
     * только {@code map} и {@code apply}. Аргументов у них столько, сколько нужно:
     * маппингу с несколькими входами для того и разрешён собственный интерфейс.
     */
    @ArchTest
    static final ArchRule mappers_should_declare_only_mapping_methods =
            classes()
                    .that(are_mapper_implementations())
                    .should(declare_only_mapping_methods())
                    .as("a mapper should declare only its mapping methods: D map(S) and/or void apply(S, D)");

    /**
     * Отсутствие значения — забота вызывающего, а не маппера.
     * <p>
     * Проверяются предмет маппинга (первый аргумент), цель у {@code apply} (последний)
     * и результат: их вызывающий умеет разложить сам через {@code Optional}. Аргумент
     * с контекстом — другое дело: связь, которой в принципе может не быть, снаружи через
     * {@code Optional} не выразить. Приватная кухня маппера правилом тоже не затрагивается.
     */
    @ArchTest
    static final ArchRule mapping_methods_should_not_deal_with_null =
            classes()
                    .that(are_mapper_implementations())
                    .should(declare_no_nullable_mapping_signature())
                    .as("mapping methods should neither accept nor return null");

    // ------------------------------------------------------------------ чистота

    /** Маппер читает только то, что ему дали. */
    @ArchTest
    static final ArchRule mappers_should_not_depend_on_repositories_services_or_domain_logic =
            noClasses()
                    .that(are_mapper_implementations())
                    .should().dependOnClassesThat().areAssignableTo(Repository.class)
                    .orShould().dependOnClassesThat().areAnnotatedWith(Service.class)
                    .orShould().dependOnClassesThat().resideInAnyPackage(SERVICES)
                    .orShould().dependOnClassesThat().resideInAPackage(ROOT + ".businesslogic..")
                    .as("mappers should depend on nothing but other mappers and their own two types");

    // ------------------------------------------------------------------ видимость

    /**
     * Реализацию маппера не видно ниоткуда: наружу он выходит только интерфейсом.
     * <p>
     * Для мапперов над сущностями это ещё и держит правило «мапит тот, кто фетчил»:
     * такой маппер лежит рядом с репозиториями, а сущность за пределы слоя доступа
     * к данным не выходит, так что снаружи его тип попросту не назвать.
     */
    @ArchTest
    static final ArchRule mapper_implementations_should_not_be_public =
            classes()
                    .that(are_mapper_implementations())
                    .should().notBePublic()
                    .as("mapper implementations should be package-private");

    /** И даже внутри своего пакета маппер берут по интерфейсу. */
    @ArchTest
    static final ArchRule mappers_should_be_consumed_through_their_interface =
            classes()
                    .that().resideInAPackage(ROOT + "..")
                    .should(not_depend_on_mapper_implementations())
                    .as("consumers should depend on Mapper/UpdateMapper, not on a mapper implementation");

    // ------------------------------------------------------------ где маппинга быть не должно

    /**
     * В репозитории маппинга нет.
     * <p>
     * Признак маппинга — пересечение границы: в сигнатуре метода встретились и
     * персистентный тип (сущность или проекция запроса), и модель данных. Методы,
     * которые работают по одну сторону границы — достать сущность, разложить сущности
     * по ключу, сгруппировать строки, — маппингом не являются и правилу не мешают.
     */
    @ArchTest
    static final ArchRule data_repositories_should_not_declare_mapping_methods =
            classes()
                    .that().resideInAPackage(DATA_ACCESS)
                    .and(are_not_listed_in(DATA_REPOSITORIES_WITH_INLINE_MAPPING))
                    .should(declare_no_methods_crossing_the_persistence_boundary())
                    .as("mapping belongs to mappers, not to data repositories");

    /** DTO собирает маппер, а не тот, кому DTO понадобилось. */
    @ArchTest
    static final ArchRule dtos_should_be_built_only_by_mappers =
            classes()
                    .that().resideOutsideOfPackages(FRONTEND_MAPPERS, DTO)
                    .and(are_not_listed_in(DTO_BUILDERS_OUTSIDE_MAPPERS))
                    .should(build_no_dtos())
                    .as("only mappers should construct web DTOs");

    // ------------------------------------------------------------------ страховки

    /**
     * Страховка от вырождения: правила выше молча зелены, если ни одного маппера не нашлось.
     * <p>
     * Так бывает, когда модуль выпал из анализа или интерфейс переехал.
     */
    @ArchTest
    static void mapper_rules_should_not_be_vacuous(JavaClasses classes) {
        long mappers = classes.stream().filter(are_mapper_implementations()).count();
        if (mappers == 0) {
            throw new AssertionError(
                    "No mapper implementations found at all, so every rule in this class passes vacuously. "
                            + "Check that the analyzed modules are built and that "
                            + Mapping.class.getName() + " is on the analysed class path.");
        }
    }

    /** Списки исключений тают вместе с рефакторингом, а не остаются жить своей жизнью. */
    @ArchTest
    static void the_not_yet_migrated_lists_should_not_rot(JavaClasses classes) {
        Set<String> present = classes.stream().map(JavaClass::getName).collect(Collectors.toSet());
        List<String> stale = Stream
                .of(DATA_REPOSITORIES_WITH_INLINE_MAPPING, DTO_BUILDERS_OUTSIDE_MAPPERS,
                        NON_MAPPERS_IN_MAPPER_PACKAGES)
                .flatMap(Set::stream)
                .filter(name -> !present.contains(name))
                .sorted()
                .toList();
        if (!stale.isEmpty()) {
            throw new AssertionError(
                    "These classes no longer exist and should be dropped from the not-yet-migrated lists: "
                            + String.join(", ", stale));
        }
    }

    // ------------------------------------------------------------------ предикаты и условия

    private static DescribedPredicate<JavaClass> are_mapper_implementations() {
        return new DescribedPredicate<>("implement a mapping interface") {
            @Override
            public boolean test(JavaClass item) {
                return !item.isInterface() && item.isAssignableTo(Mapping.class);
            }
        };
    }

    private static DescribedPredicate<JavaClass> are_not_listed_in(Set<String> frozen) {
        return new DescribedPredicate<>("are not on the not-yet-migrated list") {
            @Override
            public boolean test(JavaClass item) {
                return !frozen.contains(item.getName());
            }
        };
    }

    private static DescribedPredicate<JavaClass> are_named_like_a_mapper() {
        return new DescribedPredicate<>("are named like a mapper") {
            @Override
            public boolean test(JavaClass item) {
                String name = item.getSimpleName();
                return name.endsWith("Mapper") || name.endsWith("MapperImpl");
            }
        };
    }

    private static ArchCondition<JavaClass> be_named_and_placed_like_a_mapper() {
        return new ArchCondition<>("be named *Mapper or *MapperImpl and live in a mapper package") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!are_named_like_a_mapper().test(item)) {
                    events.add(SimpleConditionEvent.violated(item, String.format(
                            "%s is a mapper but is not named like one, in %s",
                            item.getName(), item.getSourceCodeLocation())));
                }
                String pkg = item.getPackageName();
                if (!pkg.equals(ROOT + ".repositories.mappers") && !pkg.equals(ROOT + ".frontend.mappers")) {
                    events.add(SimpleConditionEvent.violated(item, String.format(
                            "%s is a mapper but lives in %s, in %s",
                            item.getName(), pkg, item.getSourceCodeLocation())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> be_a_mapping() {
        return new ArchCondition<>("be a mapping interface or its implementation") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                // Вложенные и синтетические классы членами пакета не являются:
                // javac кладёт сюда, например, таблицу переходов switch по enum.
                if (item.getEnclosingClass().isPresent() || item.isAssignableTo(Mapping.class)) {
                    return;
                }
                events.add(SimpleConditionEvent.violated(item, String.format(
                        "%s lives among mappers but is not one, in %s",
                        item.getName(), item.getSourceCodeLocation())));
            }
        };
    }

    private static ArchCondition<JavaClass> declare_no_static_methods() {
        return new ArchCondition<>("declare no static methods") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaMethod method : item.getMethods()) {
                    if (method.getModifiers().contains(JavaModifier.STATIC) && !isCompilerGenerated(method)) {
                        events.add(SimpleConditionEvent.violated(method, String.format(
                                "%s is static; a mapper is a bean, so its mapping is an instance method, in %s",
                                method.getFullName(), method.getSourceCodeLocation())));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> declare_only_mapping_methods() {
        return new ArchCondition<>("declare only mapping methods") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                List<JavaMethod> declared = item.getMethods().stream()
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(method -> !isCompilerGenerated(method))
                        .filter(method -> !isBridge(method))
                        .toList();

                if (declared.isEmpty()) {
                    events.add(SimpleConditionEvent.violated(item, String.format(
                            "%s is a mapper but maps nothing", item.getName())));
                    return;
                }
                for (JavaMethod method : declared) {
                    checkMappingMethod(method, events);
                }
            }
        };
    }

    private static void checkMappingMethod(JavaMethod method, ConditionEvents events) {
        int arity = method.getRawParameterTypes().size();
        switch (method.getName()) {
            case "map" -> {
                if (arity < 1) {
                    events.add(SimpleConditionEvent.violated(method, String.format(
                            "%s maps from nothing, in %s",
                            method.getFullName(), method.getSourceCodeLocation())));
                }
                if (method.getRawReturnType().getName().equals("void")) {
                    events.add(SimpleConditionEvent.violated(method, String.format(
                            "%s returns nothing; a method that fills an existing target is apply(), in %s",
                            method.getFullName(), method.getSourceCodeLocation())));
                } else if (isCollectionLike(method.getRawReturnType())) {
                    events.add(SimpleConditionEvent.violated(method, String.format(
                            "%s returns a collection; a mapper knows about one value, and Mapper.mapAll "
                                    + "covers the rest, in %s",
                            method.getFullName(), method.getSourceCodeLocation())));
                }
            }
            case "apply" -> {
                if (arity < 2) {
                    events.add(SimpleConditionEvent.violated(method, String.format(
                            "%s takes %d arguments; apply() needs at least a source and a destination, in %s",
                            method.getFullName(), arity, method.getSourceCodeLocation())));
                }
                if (!method.getRawReturnType().getName().equals("void")) {
                    events.add(SimpleConditionEvent.violated(method, String.format(
                            "%s returns a value; a method that builds its result is map(), in %s",
                            method.getFullName(), method.getSourceCodeLocation())));
                }
            }
            default -> events.add(SimpleConditionEvent.violated(method, String.format(
                    "%s is public but is not a mapping method; a mapper exposes only map() and apply(), in %s",
                    method.getFullName(), method.getSourceCodeLocation())));
        }
    }

    private static ArchCondition<JavaClass> declare_no_nullable_mapping_signature() {
        return new ArchCondition<>("declare no nullable mapping signature") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaMethod method : item.getMethods()) {
                    if (!method.getModifiers().contains(JavaModifier.PUBLIC)
                            || isCompilerGenerated(method) || isBridge(method)) {
                        continue;
                    }
                    if (method.isAnnotatedWith("org.jetbrains.annotations.Nullable")) {
                        events.add(SimpleConditionEvent.violated(method, String.format(
                                "%s may return null; absence is the business of the caller, in %s",
                                method.getFullName(), method.getSourceCodeLocation())));
                    }
                    List<JavaParameter> parameters = method.getParameters();
                    int destination = method.getName().equals("apply") ? parameters.size() - 1 : -1;
                    for (JavaParameter parameter : parameters) {
                        boolean checked = parameter.getIndex() == 0 || parameter.getIndex() == destination;
                        if (checked && parameter.isAnnotatedWith("org.jetbrains.annotations.Nullable")) {
                            events.add(SimpleConditionEvent.violated(method, String.format(
                                    "argument %d of %s is nullable; the subject and the destination are never "
                                            + "absent, the caller does Optional.map(mapper::map), in %s",
                                    parameter.getIndex(), method.getFullName(), method.getSourceCodeLocation())));
                        }
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> not_depend_on_mapper_implementations() {
        return new ArchCondition<>("not depend on mapper implementations") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                DescribedPredicate<JavaClass> isMapper = are_mapper_implementations();
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass().getBaseComponentType();
                    if (target.equals(item) || !isMapper.test(target)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(item, String.format(
                            "%s depends on the mapper implementation %s; inject Mapper/UpdateMapper instead, in %s",
                            item.getName(), target.getName(), dependency.getSourceCodeLocation())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> declare_no_methods_crossing_the_persistence_boundary() {
        return new ArchCondition<>("declare no methods mentioning both persistence types and data models") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaMethod method : item.getMethods()) {
                    Set<JavaClass> mentioned = new LinkedHashSet<>();
                    method.getParameterTypes().forEach(type -> collectMentionedClasses(type, mentioned));
                    collectMentionedClasses(method.getReturnType(), mentioned);

                    var persistence = mentioned.stream().filter(MapperConventionTest::isPersistenceType)
                            .findFirst();
                    var model = mentioned.stream().filter(MapperConventionTest::isDataModel).findFirst();
                    if (persistence.isPresent() && model.isPresent()) {
                        events.add(SimpleConditionEvent.violated(method, String.format(
                                "%s mentions both %s and %s, which makes it a mapper; move it into a *Mapper "
                                        + "class in repositories.mappers, in %s",
                                method.getFullName(), persistence.get().getName(), model.get().getName(),
                                method.getSourceCodeLocation())));
                    }
                }
            }
        };
    }

    /** Разворачивает {@code List<Foo>} и прочие обобщения до самих классов. */
    private static void collectMentionedClasses(JavaType type, Set<JavaClass> collected) {
        if (type instanceof JavaParameterizedType parameterized) {
            parameterized.getActualTypeArguments().forEach(argument -> collectMentionedClasses(argument, collected));
        }
        if (type instanceof JavaWildcardType wildcard) {
            wildcard.getUpperBounds().forEach(bound -> collectMentionedClasses(bound, collected));
            wildcard.getLowerBounds().forEach(bound -> collectMentionedClasses(bound, collected));
            return;
        }
        collected.add(type.toErasure());
    }

    private static boolean isDataModel(JavaClass type) {
        String pkg = type.getPackageName();
        return pkg.equals(ROOT + ".data") || pkg.startsWith(ROOT + ".data.") || pkg.contains(".dto");
    }

    private static ArchCondition<JavaClass> build_no_dtos() {
        return new ArchCondition<>("not construct web DTOs") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Set<JavaCall<?>> calls = new LinkedHashSet<>();
                calls.addAll(item.getConstructorCallsFromSelf());
                calls.addAll(item.getMethodCallsFromSelf());
                for (JavaCall<?> call : calls) {
                    JavaClass owner = call.getTargetOwner();
                    if (!owner.getName().contains(".dto.")) {
                        continue;
                    }
                    String target = call.getTarget().getName();
                    if (target.equals("<init>") || target.equals("builder")) {
                        events.add(SimpleConditionEvent.violated(call, String.format(
                                "%s builds %s itself; DTOs are assembled by mappers, in %s",
                                item.getName(), owner.getName(), call.getSourceCodeLocation())));
                    }
                }
            }
        };
    }

    /** Мост, который javac ставит на обобщённый метод интерфейса: этого в исходнике нет. */
    private static boolean isBridge(JavaMethod method) {
        return !method.getRawParameterTypes().isEmpty()
                && method.getRawParameterTypes().stream()
                        .allMatch(type -> type.getName().equals("java.lang.Object"));
    }

    /** Лямбды и мосты доступа, которых в исходнике тоже нет. */
    private static boolean isCompilerGenerated(JavaMethod method) {
        return method.getName().startsWith("lambda$") || method.getName().startsWith("access$");
    }

    private static boolean isCollectionLike(JavaClass type) {
        return type.isArray() || type.isAssignableTo(Collection.class) || type.isAssignableTo(Map.class);
    }

    private static boolean isPersistenceType(JavaClass type) {
        String name = type.getName();
        String pkg = type.getPackageName();
        return pkg.equals(ROOT + ".entities") || pkg.startsWith(ROOT + ".entities.")
                || name.startsWith(ROOT + ".repositories.entity.");
    }
}
