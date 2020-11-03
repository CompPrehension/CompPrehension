package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Domain")
public class Domain {
    @Id
    private String name;

    private String version;
    
    @OneToMany(mappedBy = "domain", fetch = FetchType.LAZY)
    private List<Law> laws;

    @OneToMany(mappedBy = "domain", fetch = FetchType.LAZY)
    private List<Concept> concepts;

}
