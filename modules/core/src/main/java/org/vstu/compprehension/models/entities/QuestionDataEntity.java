package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
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
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "data", columnDefinition = "json", nullable = false)
    @Type(SerializableQuestionType.class)
    private SerializableQuestion data;

    @OneToOne(mappedBy = "questionData")
    private QuestionMetadataEntity questionMetadata;
}
