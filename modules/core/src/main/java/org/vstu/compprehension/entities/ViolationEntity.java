package org.vstu.compprehension.entities;

import org.vstu.compprehension.data.question.BackendFactData;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "violation")
public class ViolationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private InteractionEntity interaction;

    @OneToMany(mappedBy = "violation", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ExplanationTemplateInfoEntity> explanationTemplateInfo;

    @Column(name = "law_name", nullable = false)
    private String lawName;

    @Immutable
    @Type(JsonType.class)
    @Column(name = "violation_facts", nullable = false)
    private List<BackendFactData> violationFacts;
}
