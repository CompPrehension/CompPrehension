package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExplanationTemplateInfo")
public class ExplanationTemplateInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fieldName")
    private String fieldName;

    @Column(name = "value")
    private String value;


    @ManyToOne
    @JoinColumn(name = "violation_id", nullable = false)
    private MistakeEntity mistake;
}
