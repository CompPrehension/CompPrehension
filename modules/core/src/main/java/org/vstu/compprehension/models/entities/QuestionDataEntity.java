package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.vstu.compprehension.models.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.utils.SerializableQuestionType;

@Getter
@Setter
@Entity
@Table(name = "questions_data")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "data", columnDefinition = "json", nullable = false)
    @Type(SerializableQuestionType.class)
    private SerializableQuestion data;
}
