package com.example.demo.models.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public abstract class Law {
    @Getter
    String name;
    @Getter
    List<LawFormulation> lawFormulations;
    @Getter
    List<Concept> concepts;
    @Getter
    List<Tag> tags;

    public abstract boolean isPositiveLaw();
}
