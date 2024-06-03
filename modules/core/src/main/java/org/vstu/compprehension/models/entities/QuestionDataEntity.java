package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

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
    private Long id;

    @Column(name = "data", columnDefinition = "json", nullable = false)
    @Type(JsonType.class)
    private String data;

    @OneToOne(mappedBy = "questionData")
    private QuestionMetadataEntity questionMetadata;
}
