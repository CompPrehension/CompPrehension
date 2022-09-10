package org.vstu.compprehension.models.entities;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter @Setter
@IdClass(SurveyAnswerEntity.SurveyResultId.class)
@Table(name = "survey_answers")
public class SurveyAnswerEntity {
    @Getter @Setter
    @AllArgsConstructor @NoArgsConstructor
    public static class SurveyResultId implements Serializable {
        private Long surveyQuestion;
        private Long question;
        private Long user;
    }

    @Id
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "survey_question_id")
    private @NotNull SurveyQuestionEntity surveyQuestion;

    @Id
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "question_id")
    private @NotNull QuestionEntity question;

    @Id
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private @NotNull UserEntity user;

    @Column(name = "result", nullable = true, length = 255)
    private @Nullable String result;
}
