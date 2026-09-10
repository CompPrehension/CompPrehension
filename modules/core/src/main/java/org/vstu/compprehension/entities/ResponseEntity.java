package org.vstu.compprehension.entities;

import lombok.*;
import org.vstu.compprehension.enums.SpecValue;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "Response")
public class ResponseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leftSpecValue")
    @Enumerated(EnumType.ORDINAL)
    private SpecValue specValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leftObject_id")
    private AnswerObjectEntity leftAnswerObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rightObject_id")
    private AnswerObjectEntity rightAnswerObject;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_interaction_id")
    private InteractionEntity createdByInteraction;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id")
    private InteractionEntity interaction;
}
