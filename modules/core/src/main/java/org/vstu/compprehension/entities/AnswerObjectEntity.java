package org.vstu.compprehension.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Builder
@Table(
    name = "answer_object",
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

    @Column(name = "hyper_text", nullable = false)
    private String hyperText;

    @Column(name = "domain_info", length = 1000)
    private String domainInfo;

    @Column(name = "is_right_col", nullable = false)
    private boolean isRightCol;

    @Column(name = "concept")
    private String concept;

    @ToString.Exclude
    @OneToMany(mappedBy = "leftAnswerObject", fetch = FetchType.LAZY)
    private List<ResponseEntity> responsesLeft;

    @ToString.Exclude
    @OneToMany(mappedBy = "rightAnswerObject", fetch = FetchType.LAZY)
    private List<ResponseEntity> responsesRight;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;
    
}
