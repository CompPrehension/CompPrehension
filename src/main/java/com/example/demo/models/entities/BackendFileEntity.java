package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "BackendFile")
public class BackendFileEntity {
    @Id//Не было ничего, добавил чтобы можно было запустить
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
}
