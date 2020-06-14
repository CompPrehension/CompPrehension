package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "LawFormulation")
public class LawFormulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String formulation;

    @ManyToOne
    @JoinColumn(name = "law_id", nullable = false)
    private Law law;

    @ManyToOne
    @JoinColumn(name = "backend_id", nullable = false)
    private Backend backend;
}
