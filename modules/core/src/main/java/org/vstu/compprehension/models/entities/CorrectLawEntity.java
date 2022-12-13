package org.vstu.compprehension.models.entities;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "CorrectLaw")
public class CorrectLawEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private InteractionEntity interaction;

    @Column(name = "law_name", nullable = false)
    private String lawName;
}
