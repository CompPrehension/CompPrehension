package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Mistake")
public class Mistake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "interaction_id", referencedColumnName = "id", nullable = false)
    private Interaction interaction;


    @OneToMany(mappedBy = "mistake", fetch = FetchType.LAZY)
    private List<ExplanationTemplateInfo> explanationTemplateInfo;


    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "law_id", nullable = false)
    private Law law;
}
