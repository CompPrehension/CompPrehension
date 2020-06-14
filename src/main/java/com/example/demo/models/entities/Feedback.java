package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.FeedbackType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hyperText")
    private String hyperText;


    @Column(name = "FeedbackType")
    @Enumerated(EnumType.STRING)
    private FeedbackType feedBackType;


}
