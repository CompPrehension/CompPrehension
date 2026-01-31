package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.shared.BaseEntity;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "CorrectLaw")
public class CorrectLawEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private InteractionEntity interaction;

    @Column(name = "law_name", nullable = false)
    private String lawName;
}
