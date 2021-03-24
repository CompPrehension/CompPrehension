package org.vstu.compprehension.models.entities;

import org.vstu.compprehension.models.entities.EnumData.ActionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "UserAction")
public class UserActionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(name = "time")
    private Date time;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "userActionExercise_id", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    private UserActionExerciseEntity userActionExercise;
}
