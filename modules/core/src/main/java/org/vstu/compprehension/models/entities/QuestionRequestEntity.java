package org.vstu.compprehension.models.entities;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "QuestionRequestLog")
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class QuestionRequestEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    Long exerciseAttemptId;

    @Type(type = "json")
    private List<String> targetConceptNames;
    @Type(type = "json")
    private List<String> targetConceptNamesInPlan;

    @Type(type = "json")
    private List<String> deniedConceptNames;

    /** Question Storage can treat this as "optional targets" or "preferred" */
    @Type(type = "json")
    private List<String> allowedConceptNames;


    @Type(type = "json")
    private List<String> targetLawNames;
    @Type(type = "json")
    private List<String> targetLawNamesInPlan;

    @Type(type = "json")
    private List<String> deniedLawNames;

    @Type(type = "json")
    private List<String> allowedLawNames;

    @Type(type = "json")
    private List<String> deniedQuestionNames;

    @Type(type = "json")
    private List<Integer> deniedQuestionTemplateIds = null;
    @Type(type = "json")
    private List<Integer> deniedQuestionMetaIds = null;  // same as deniedQuestionNames but using ids instead of names

    /**
     * Условная единица, показывающая долго или быстро решается вопрос
     * 1 - быстро, 10 - очень долго
     */
    private int solvingDuration;


    /**
     * Сложность задания [0..1]
     */
    private float complexity;

    /**
     * Направление поиска по сложности
     */
    @Enumerated(EnumType.ORDINAL)
    private SearchDirections complexitySearchDirection;

    /**
     * Направление поиска по сложности
     */
    @Enumerated(EnumType.ORDINAL)
    private SearchDirections lawsSearchDirection;

    /**
     * Probability of choosing an auto-generated question
     * Вероятность выбора авто-сгенерированного вопроса
     * range: [0..1]
     * 0: always pick manual question
     * 1: always pick auto-generated question
     */
    private double chanceToPickAutogeneratedQuestion = 1;

    private int foundCount = -1;

    Date date;


}

