package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.InteractionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Interaction")
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderNumber")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int orderNumber;


    @Column(name = "interactionType")
    @Enumerated(EnumType.STRING)
    private InteractionType interactionType;


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "feedback_id", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Feedback feedback;

    @OneToMany(mappedBy = "interaction")
    private List<Mistake> mistakes;


    @OneToMany(mappedBy = "interaction", fetch = FetchType.LAZY)
    private List<Response> responses;


    @ManyToOne
    @JoinColumn(name = "questionAttempt_id", nullable = false)
    private QuestionAttempt questionAttempt;


}
