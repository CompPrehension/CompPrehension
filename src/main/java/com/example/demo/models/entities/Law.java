package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Law")
public class Law {
    @Id
    private String name;

    @Column(name = "is_positive_law")
    private boolean isPositiveLaw;

    @OneToMany(mappedBy = "law")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<LawFormulation> lawFormulations;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;

    @ManyToMany
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinTable(
            name = "ConceptLaw",
            joinColumns = @JoinColumn(name = "law_name"),
            inverseJoinColumns = @JoinColumn(name = "concept_name"))
    private List<Concept> concepts;
}
