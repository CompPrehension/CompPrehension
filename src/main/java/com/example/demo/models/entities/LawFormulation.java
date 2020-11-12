package com.example.demo.models.entities;

import com.example.demo.models.businesslogic.Law;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LawFormulation {
    private String formulation;
    private Law law;
    private Backend backend;
}
