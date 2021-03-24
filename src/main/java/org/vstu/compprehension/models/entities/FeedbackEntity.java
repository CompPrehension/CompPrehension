package org.vstu.compprehension.models.entities;

import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Feedback")
public class FeedbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hyperText")
    private String hyperText;


    @Column(name = "FeedbackType")
    @Enumerated(EnumType.STRING)
    private FeedbackType feedBackType;


}
