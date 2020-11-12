package com.example.demo.models.entities;

import lombok.Getter;

import java.util.List;

public class NegativeLaw extends Law {
    @Getter
    PositiveLaw positiveLaw;

    public NegativeLaw(String name, List<LawFormulation> lawFormulations, List<Concept> concepts, List<Tag> tags, PositiveLaw positiveLaw) {
        super(name, lawFormulations, concepts, tags);
        this.positiveLaw = positiveLaw;
    }

    @Override
    public boolean isPositiveLaw() {
        return false;
    }
}
