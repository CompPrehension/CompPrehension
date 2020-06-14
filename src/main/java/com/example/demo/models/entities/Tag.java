package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Tag")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "left_key")
    private int leftKey;

    @Column(name = "right_key")
    private int rightKey;

    private int level;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "LawTag",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = @JoinColumn(name = "law_id"))
    private List<Law> laws;

}
