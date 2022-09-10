package org.vstu.compprehension.models.entities;

import lombok.*;
import org.vstu.compprehension.models.entities.EnumData.SpecValue;

import javax.persistence.*;

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

    @ManyToOne
    @JoinColumn(name = "leftObject_id")
    private AnswerObjectEntity leftAnswerObject;

    @ManyToOne
    @JoinColumn(name = "rightObject_id")
    private AnswerObjectEntity rightAnswerObject;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "created_by_interaction_id")
    private InteractionEntity createdByInteraction;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "interaction_id")
    private InteractionEntity interaction;
}
