package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Backend")
public class BackendEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Делаю так, что Backend не может знать об Exercise, LawFormulation и questionConceptChoice/Order/Match.
    // Чтобы изменить, надо сделать OneToMany связь и добавить списки сюда
    // См.Пример в Question
}
