package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Concept {
    private String name;

    private DomainEntity domain;

    private List<Law> laws;
}
