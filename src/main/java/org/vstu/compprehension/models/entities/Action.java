package org.vstu.compprehension.models.entities;

import lombok.Data;

import java.util.Date;

@Data
public class Action {

    /**
     * Имя и фамилия пользователя
     */
    private String userName;


    /**
     * Время, в которое произошло действие
     */
    private Date actionTime;


    /**
     * Тип действия в виде строки
     */
    private String actionType;


    /**
     * Название упражнения
     */
    private String exerciseName;
    
    
}
