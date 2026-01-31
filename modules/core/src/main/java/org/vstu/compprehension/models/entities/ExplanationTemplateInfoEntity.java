package org.vstu.compprehension.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;
import lombok.*;
import org.vstu.compprehension.models.entities.shared.BaseEntity;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExplanationTemplateInfo")
public class ExplanationTemplateInfoEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fieldName")
    private String fieldName;

    @Column(name = "value")
    private String value;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "violation_id", nullable = false)
    private ViolationEntity violation;
}
