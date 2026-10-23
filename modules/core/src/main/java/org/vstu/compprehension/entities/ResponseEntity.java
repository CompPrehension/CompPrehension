package org.vstu.compprehension.entities;

import lombok.*;

import jakarta.persistence.*;
import org.jetbrains.annotations.Nullable;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "response")
public class ResponseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_object_id", nullable = false)
    private AnswerObjectEntity leftAnswerObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "right_object_id")
    private AnswerObjectEntity rightAnswerObject;

    @Nullable
    @Column(name = "answer_value", nullable = true)
    private Integer value;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_interaction_id", nullable = false)
    private InteractionEntity createdByInteraction;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", nullable = false)
    private InteractionEntity interaction;
}
