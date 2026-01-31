package org.vstu.compprehension.models.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import lombok.*;
import org.vstu.compprehension.models.entities.shared.BaseEntity;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "Feedback")
public class FeedbackEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grade")
    private float grade;

    @Column(name = "interactions_left")
    private int interactionsLeft;
}
