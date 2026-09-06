package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttemptEntity, Long> {

    /**
     * Владелец попытки.
     * <p>
     * Интерфейс, а не запись: оба поля — {@code Long}, и при позиционном связывании
     * их перестановка не вызвала бы ни ошибки компиляции, ни исключения.
     */
    interface AttemptOwner {
        Long getUserId();
        Long getCourseId();
    }

    /**
     * Попытка, в которой задан вопрос, вместе с упражнением и автором.
     * <p>
     * Вопросы попытки намеренно не поднимаются: у этого запроса четыре потребителя,
     * и ни одному из них они не нужны — читаются язык автора, этапы упражнения и одна
     * его настройка. Прежняя версия тянула {@code left join fetch a.questions}, то есть
     * всю попытку целиком на каждое такое обращение.
     */
    @Query("""
            select a from ExerciseAttemptEntity a
            inner join fetch a.exercise
            left join fetch a.user
            where a.id in (select q.exerciseAttempt.id from QuestionEntity q
                           where q.id = :questionId and q.exerciseAttempt is not null)
            """)
    Optional<ExerciseAttemptEntity> findByQuestionIdFetchingExerciseAndUser(@Param("questionId") long questionId);

    /**
     * Попытка вместе с упражнением и доменом — одним запросом.
     * <p>
     * join fetch обязателен: обе связи ленивые, а вызывающему нужны их поля.
     */
    @Query("""
            select a from ExerciseAttemptEntity a
            join fetch a.exercise e
            join fetch e.domain
            where a.id = :attemptId
            """)
    Optional<ExerciseAttemptEntity> findByIdFetchingExerciseAndDomain(@Param("attemptId") long attemptId);
    /**
     * Попытка в объёме, который уезжает на фронт.
     * <p>
     * Связи присоединены явными join-ами, а не путями вида {@code a.course.id}: путь
     * через to-one связь порождает <b>внутреннее</b> соединение, и попытки вне курса
     * молча выпали бы из выдачи.
     */
    interface AttemptSummaryRow {
        Long getAttemptId();
        Long getUserId();
        Long getExerciseId();
        Long getCourseId();
        AttemptStatus getStatus();
    }

    @Query("""
            select a.id as attemptId, u.id as userId, e.id as exerciseId,
                   c.id as courseId, a.attemptStatus as status
            from ExerciseAttemptEntity a
            join a.user u
            join a.exercise e
            left join a.course c
            where a.id = :attemptId
            """)
    Optional<AttemptSummaryRow> findSummaryRow(@Param("attemptId") long attemptId);

    /** Попытка пользователя по упражнению в заданном статусе, без учёта курса. */
    @Query("""
            select a.id as attemptId, u.id as userId, e.id as exerciseId,
                   c.id as courseId, a.attemptStatus as status
            from ExerciseAttemptEntity a
            join a.user u
            join a.exercise e
            left join a.course c
            where e.id = :exerciseId and u.id = :userId and a.attemptStatus = :status
            """)
    Optional<AttemptSummaryRow> findSummaryRowWithStatus(@Param("exerciseId") long exerciseId,
                                                         @Param("userId") long userId,
                                                         @Param("status") AttemptStatus status);

    /** То же, но в рамках конкретного курса. */
    @Query("""
            select a.id as attemptId, u.id as userId, e.id as exerciseId,
                   c.id as courseId, a.attemptStatus as status
            from ExerciseAttemptEntity a
            join a.user u
            join a.exercise e
            join a.course c
            where e.id = :exerciseId and c.id = :courseId and u.id = :userId
              and a.attemptStatus = :status
            """)
    Optional<AttemptSummaryRow> findSummaryRowWithStatusByCourse(@Param("exerciseId") long exerciseId,
                                                                 @Param("courseId") long courseId,
                                                                 @Param("userId") long userId,
                                                                 @Param("status") AttemptStatus status);

    /**
     * Адресат оценки за попытку.
     * <p>
     * Курс и его образовательный ресурс присоединены left join-ами: попытка может идти
     * вне курса, и внутреннее соединение молча выкинуло бы её из выдачи.
     */
    interface GradePassbackTargetRow {
        Long getAttemptId();
        Long getExerciseId();
        Long getUserId();
        String getExternalUserId();
        String getLtiLineitemUrl();
        Long getCourseId();
        String getExternalCourseId();
        Long getEducationResourceId();
        EducationResourceType getEducationResourceType();
        String getEducationResourceUrl();
    }

    @Query("""
            select a.id as attemptId, e.id as exerciseId,
                   u.id as userId, u.externalUserId as externalUserId,
                   a.ltiLineitemUrl as ltiLineitemUrl,
                   c.id as courseId, c.externalCourseId as externalCourseId,
                   er.id as educationResourceId, er.type as educationResourceType,
                   er.url as educationResourceUrl
            from ExerciseAttemptEntity a
            join a.user u
            join a.exercise e
            left join a.course c
            left join c.educationResource er
            where a.id = :attemptId
            """)
    Optional<GradePassbackTargetRow> findGradePassbackTargetRow(@Param("attemptId") long attemptId);

    /**
     * Основные вопросы попытки, в порядке выдачи.
     * <p>
     * Вспомогательные отсеиваются в БД. Раньше это делалось в памяти над поднятой
     * коллекцией вопросов: {@code !q.getQuestionDomainType().contains("Supplementary")}.
     * Отличие одно — вопрос с пустым {@code questionDomainType} теперь не попадёт
     * в выдачу вместо того, чтобы уронить маппинг на NPE.
     */
    @Query("""
            select q.id from QuestionEntity q
            where q.exerciseAttempt.id = :attemptId
              and q.questionDomainType not like '%Supplementary%'
            order by q.id
            """)
    List<Long> findNonSupplementaryQuestionIds(@Param("attemptId") long attemptId);

    @Query("""
            select a.user.id as userId, a.course.id as courseId
            from ExerciseAttemptEntity a
            where a.id = :attemptId
            """)
    Optional<AttemptOwner> findOwnerByAttemptId(@Param("attemptId") long attemptId);

    @Query("""
            select a.user.id as userId, a.course.id as courseId
            from ExerciseAttemptEntity a
            join QuestionEntity q on a.id = q.exerciseAttempt.id
            where q.id = :questionId
            """)
    Optional<AttemptOwner> findOwnerByQuestionId(@Param("questionId") long questionId);
    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user where a.exercise.id = ?1 and a.user.id = ?2 and a.attemptStatus = ?3")
    List<ExerciseAttemptEntity> getAllByStatus(Long exerciseId, Long userId, AttemptStatus status);
    @Modifying
    @Query(value = "UPDATE ExerciseAttemptEntity SET attemptStatus = :newStatus WHERE exercise.id = :exerciseId AND user.id = :userId AND attemptStatus = :oldStatus")
    int changeExistingAttemptsStatus(@Param("exerciseId") Long exerciseId, @Param("userId") Long userId, @Param("oldStatus") AttemptStatus oldStatus, @Param("newStatus") AttemptStatus newStatus);
    @Modifying
    @Query(value = "UPDATE ExerciseAttemptEntity SET attemptStatus = :newStatus WHERE exercise.id = :exerciseId AND course.id = :courseId AND user.id = :userId AND attemptStatus = :oldStatus")
    int changeExistingAttemptsStatusByCourse(@Param("exerciseId") Long exerciseId, @Param("courseId") Long courseId, @Param("userId") Long userId, @Param("oldStatus") AttemptStatus oldStatus, @Param("newStatus") AttemptStatus newStatus);

    @Query("select distinct a from ExerciseAttemptEntity a inner join fetch a.exercise left join fetch a.questions left join fetch a.user left join fetch a.course where a.exercise.id = ?1 and a.course.id = ?2")
    List<ExerciseAttemptEntity> getAllByExerciseAndCourse(Long exerciseId, Long courseId);

    @Query("""
        select cast(f.grade as double)
        from InteractionEntity i
        left join i.feedback f
        where i.id = (
            select max(i2.id) from InteractionEntity i2
            where i2.question.id = (
                select max(q.id) from QuestionEntity q
                where q.exerciseAttempt.id = :attemptId
            )
        )
        """)
    Optional<Double> calculateFinalGrade(@Param("attemptId") Long attemptId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ExerciseAttemptEntity a where a.exercise.id = :exerciseId")
    void deleteByExerciseId(@Param("exerciseId") long exerciseId);
}
