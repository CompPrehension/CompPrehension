package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Data
@NoArgsConstructor
public class Law {
    private String name;

    private boolean isPositiveLaw;

    private List<LawFormulation> lawFormulations;

    private DomainEntity domain;

    private List<Concept> concepts;
}
