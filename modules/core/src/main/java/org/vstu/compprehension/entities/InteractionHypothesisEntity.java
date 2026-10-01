package org.vstu.compprehension.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Гипотеза о рассуждении студента, объяснившая его ответ во взаимодействии. */
@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "interaction_hypothesis")
public class InteractionHypothesisEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private InteractionEntity interaction;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;
}
