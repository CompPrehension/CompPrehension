package org.vstu.compprehension.mappers;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.PositiveLaw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class MapperContractTest {

    private static final MapperContract CONTRACT = MapperContract
            .forPackages("org.vstu.compprehension.frontend.mappers", "org.vstu.compprehension.repositories.mappers",
                    "org.vstu.compprehension.services.mappers")
            .subtype(Law.class, PositiveLaw.class)
            .ignore("CourseRoleAssignmentMapper", "role")
            .ignore("DomainDtoMapperImpl", "tags", "concepts", "laws", "skills")
            .ignore("QuestionEntityMapperImpl", "id", "createdAt", "interactions")
            .ignore("QuestionMapperImpl", "id")
            .unordered("QuestionMapperImpl", "interactions")
            .ignore("QuestionMetadataEntityMapper",
                    "conceptBitsInPlan", "conceptBitsInRequest", "skillBitsInPlan",
                    "violationBitsInPlan", "violationBitsInRequest")
            .ignore("QuestionRequestLogMapper", "targetTags")
            .ignore("UserInfoDtoMapperImpl", "language");

    /** Исключения выше ссылаются на существующие мапперы. */
    @Test
    void exceptionsReferToExistingMappers() {
        assertEquals(List.of(), CONTRACT.staleExceptions());
    }

    /** Одноимённые поля источника попадают в результат с теми же значениями. */
    @TestFactory
    Stream<DynamicNode> mapCarriesSameNamedProperties() {
        return CONTRACT.forEachMap((mapping, seed) -> {
            // Arrange.
            var args = mapping.arguments(seed);

            // Act.
            var result = mapping.invoke(args);

            // Assert.
            assertEquals(List.of(), mapping.carriedProperties(args, result));
        });
    }

    /** Каждый вызов отдаёт новый объект, а не аргумент и не результат прошлого вызова. */
    @TestFactory
    Stream<DynamicNode> mapReturnsNewInstance() {
        return CONTRACT.forEachMap((mapping, seed) -> {
            // Arrange.
            var args = mapping.arguments(seed);

            // Act.
            var first = mapping.invoke(args);
            var second = mapping.invoke(args);

            // Assert.
            assertNotSame(first, second, "two calls returned the same instance");
            for (var arg : args) {
                assertNotSame(arg, first, "result is one of the arguments");
            }
        });
    }

    /** Один и тот же источник даёт структурно равные результаты. */
    @TestFactory
    Stream<DynamicNode> mapIsDeterministic() {
        return CONTRACT.forEachMap((mapping, seed) -> {
            // Arrange.
            var args = mapping.arguments(seed);

            // Act.
            var first = mapping.invoke(args);
            var second = mapping.invoke(args);

            // Assert.
            assertEquals(List.of(), mapping.differences(first, second));
        });
    }

    /** Маппер не изменяет источник. */
    @TestFactory
    Stream<DynamicNode> mapLeavesSourceUntouched() {
        return CONTRACT.forEachMap((mapping, seed) -> {
            // Arrange.
            var args = mapping.arguments(seed);
            var untouched = mapping.arguments(seed);

            // Act.
            mapping.invoke(args);

            // Assert.
            var differences = new ArrayList<Structure.Mismatch>();
            for (int i = 0; i < args.length; i++) {
                differences.addAll(mapping.differences(untouched[i], args[i]));
            }
            assertEquals(List.of(), differences);
        });
    }

    /** Источник с null во всех @Nullable-полях не роняет маппер. */
    @TestFactory
    Stream<DynamicNode> mapToleratesNullsInNullableFields() {
        return CONTRACT.forEachMap((mapping, seed) -> mapping.invoke(mapping.argumentsWithNulls(seed)));
    }

    /** Одноимённые поля источника попадают в заполняемый объект с теми же значениями. */
    @TestFactory
    Stream<DynamicNode> applyFillsDestinationWithSameNamedProperties() {
        return CONTRACT.forEachApply((mapping, seed) -> {
            // Arrange.
            var args = mapping.arguments(seed);
            var last = args.length - 1;

            // Act.
            mapping.invoke(args);

            // Assert.
            assertEquals(List.of(), mapping.carriedProperties(Arrays.copyOf(args, last), args[last]));
        });
    }

    /** Повторное применение ничего не меняет. */
    @TestFactory
    Stream<DynamicNode> applyIsIdempotent() {
        return CONTRACT.forEachApply((mapping, seed) -> {
            // Arrange.
            var once = mapping.arguments(seed);
            var twice = mapping.arguments(seed);
            var last = once.length - 1;

            // Act.
            mapping.invoke(once);
            mapping.invoke(twice);
            mapping.invoke(twice);

            // Assert.
            assertEquals(List.of(), mapping.differences(once[last], twice[last]));
        });
    }

    /** Источник с null во всех @Nullable-полях не роняет маппер. */
    @TestFactory
    Stream<DynamicNode> applyToleratesNullsInNullableFields() {
        return CONTRACT.forEachApply((mapping, seed) -> mapping.invoke(mapping.argumentsWithNulls(seed)));
    }
}
