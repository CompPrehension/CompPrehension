package com.example.demo.models.entities;

import com.example.demo.models.businesslogic.Law;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Tag {
    private String name;

    private int leftKey;

    private int rightKey;

    private int level;

    private List<Law> laws;
}
