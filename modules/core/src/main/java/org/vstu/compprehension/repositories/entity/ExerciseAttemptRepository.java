package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.enums.AttemptStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.entities.ExerciseAttemptEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttemptEntity, Long> {

    interface AttemptOwner {
        Long getUserId();
        Long getCourseId();
    }

    @Query("""
            select a from ExerciseAttemptEntity a
            inner join fetch a.exercise
            left join fetch a.user
            where a.id in (select q.exerciseAttempt.id from QuestionEntity q
                           where q.id = :questionId and q.exerciseAttempt is not null)
            """)
    Optional<ExerciseAttemptEntity> findByQuestionIdFetchingExerciseAndUser(@Param("questionId") long questionId);

    @Query("""
            select a from ExerciseAttemptEntity a
            join fetch a.exercise e
            join fetch e.domain
            where a.id = :attemptId
            """)
    Optional<ExerciseAttemptEntity> findByIdFetchingExerciseAndDomain(@Param("attemptId") long attemptId);

    @Query("""
            select a from ExerciseAttemptEntity a
            join fetch a.exercise e
            join fetch e.domain
            left join fetch a.user
            where a.id = :attemptId
            """)
    Optional<ExerciseAttemptEntity> findByIdFetchingExerciseDomainAndUser(@Param("attemptId") long attemptId);

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
