package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.LawFormulation;
import com.example.demo.models.entities.Tag;
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
