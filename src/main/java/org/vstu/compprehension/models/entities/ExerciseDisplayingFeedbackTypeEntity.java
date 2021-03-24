package org.vstu.compprehension.models.entities;

import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseDisplayingFeedbackType")
public class ExerciseDisplayingFeedbackTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feedBackType")
    @Enumerated(EnumType.STRING)
    private FeedbackType feedBackType;

    @Column(name = "displayingFeedBackType")
    @Enumerated(EnumType.ORDINAL)
    private DisplayingFeedbackType displayingFeedbackType;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;
}
