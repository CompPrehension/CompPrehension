package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "Violation")
public class ViolationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @ToString.Exclude
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private InteractionEntity interaction;

    @OneToMany(mappedBy = "violation", fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ExplanationTemplateInfoEntity> explanationTemplateInfo;

    @Column(name = "law_name", nullable = false)
    private String lawName;

    @Column(name = "detailed_law_name")
    private String detailedLawName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "violation_facts", nullable = false)
    private List<BackendFactEntity> violationFacts;
}
