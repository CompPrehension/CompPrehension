package org.vstu.compprehension.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Проверяет, что данные, которые сервис отдаёт наружу, доступны без открытой сессии
 * Hibernate — то есть в тех же условиях, в которых работает контроллер после отключения
 * {@code spring.jpa.open-in-view}.
 * <p>
 * Класс намеренно НЕ наследует {@code AbstractAuthorizationTest}: тот помечен
 * {@code @Transactional}, из-за чего сессия живёт весь тест и ленивые связи всегда
 * доступны. Такой тест воспроизводит поведение включённого OSIV и обрыв ленивой загрузки
 * поймать не может. Здесь транзакции нет, поэтому обращение к неинициализированной связи
 * упадёт так же, как упало бы в проде.
 */
class LazyLoadingBoundaryTest extends AbstractIntegrationTest {

    @Autowired private CourseService courseService;

    /**
     * Регрессия: раньше контроллер deep linking брал у сервиса
     * {@code ExerciseCourseLinkEntity} и уже за пределами транзакции дёргал ленивое
     * {@code getExercise().getName()}. Теперь сервис отдаёт готовые ссылки.
     */
    @Test
    void exerciseRefsAreUsableWithoutOpenSession() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "Тест обязан идти без транзакции, иначе он ничего не проверяет");

        var refs = courseService.getExerciseRefsInCourseOrThrow(
                TestData.MAIN_COURSE_ID,
                List.of(TestData.MAIN_COURSE_EXERCISE_ID, TestData.INHERITED_EXERCISE_ID));

        assertEquals(
                List.of("Inherited exercise", "Main course exercise"),
                refs.stream().map(CourseService.ExerciseRef::name).sorted().toList());
    }

    /** Упражнение из другого курса не должно проходить проверку принадлежности. */
    @Test
    void exerciseFromAnotherCourseIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> courseService.getExerciseRefsInCourseOrThrow(
                TestData.MAIN_COURSE_ID,
                List.of(TestData.OTHER_COURSE_EXERCISE_ID)));
    }
}
