package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class QuestionBankSearchRequest {
    private List<Concept> deniedConcepts;
    public long deniedConceptsBitmask() {
        return Concept.combineToBitmask(deniedConcepts);
    }

    private List<Concept> targetConcepts;
    public long targetConceptsBitmask() {
        return Concept.combineToBitmask(targetConcepts);
    }

    private List<Law> targetLaws;
    public long targetLawsBitmask() {
        return Law.combineToBitmask(targetLaws);
    }

    private List<Law> deniedLaws;
    public long deniedLawsBitmask() {
        return Law.combineToBitmask(deniedLaws);
    }

    @Builder.Default
    private List<String> deniedQuestionNames = List.of("");

    @Builder.Default
    private List<Integer> deniedQuestionTemplateIds = List.of(-1);

    @Builder.Default
    private List<Integer> deniedQuestionMetaIds = List.of(-1);


    private String domainShortname;

    /**
     * Сложность задания [0..1]
     */
    private float complexity;

    /** минимум шагов в решении */
    private int stepsMin;

    /** максимум шагов в решении */
    private int stepsMax;

    /**
     * Направление поиска по сложности
     */
    private SearchDirections lawsSearchDirection;
}
