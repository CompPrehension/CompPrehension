package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.SpecValue;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Response")
public class Response {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leftSpecValue")
    @Enumerated(EnumType.ORDINAL)
    private SpecValue specValue;

    @ManyToOne
    @JoinColumn(name = "leftObject_id", nullable = false)
    private AnswerObject leftAnswerObject;

    @ManyToOne
    @JoinColumn(name = "rightObject_id", nullable = false)
    private AnswerObject rightAnswerObject;

    @ManyToOne
    @JoinColumn(name = "interaction_id", nullable = false)
    private Interaction interaction;


}
