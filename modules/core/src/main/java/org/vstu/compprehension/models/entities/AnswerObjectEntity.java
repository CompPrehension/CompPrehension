package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Builder
@Table(
    name = "AnswerObject",
    uniqueConstraints = {
        @UniqueConstraint(
            name="uk_answerId__questionId",
            columnNames = {"answer_id", "question_id"}
        )
    }
)
@AllArgsConstructor
public class AnswerObjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "answer_id", nullable = false)
    private Integer answerId;

    @Column(name = "hyperText")
    private String hyperText;

    @Column(name = "domainInfo", length = 1000)
    private String domainInfo;

    @Column(name = "isRightCol")
    private boolean isRightCol;

    @Column(name = "concept")
    private String concept;

    @ToString.Exclude
    @OneToMany(mappedBy = "leftAnswerObject", fetch = FetchType.LAZY)
    private List<ResponseEntity> responsesLeft;

    @ToString.Exclude
    @OneToMany(mappedBy = "rightAnswerObject", fetch = FetchType.LAZY)
    private List<ResponseEntity> responsesRight;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "question_id")
    private QuestionEntity question; 
    
}
