package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class QuestionBankSearchRequest {
    private @Nullable List<Concept> deniedConcepts;
    public long deniedConceptsBitmask() {
        return Concept.combineToBitmask(deniedConcepts);
    }

    private @Nullable List<Concept> targetConcepts;
    public long targetConceptsBitmask() {
        return Concept.combineToBitmask(targetConcepts);
    }

    private @Nullable List<Law> targetLaws;
    public long targetLawsBitmask() {
        return Law.combineToBitmask(targetLaws);
    }

    private @Nullable List<Law> deniedLaws;
    public long deniedLawsBitmask() {
        return Law.combineToBitmask(deniedLaws);
    }

    private @Nullable List<Tag> targetTags;
    public long targetTagsBitmask() {
        return Tag.combineToBitmask(targetTags);
    }

    private @Nullable List<String> deniedQuestionNames;
    private @Nullable List<Integer> deniedQuestionTemplateIds;
    private @Nullable List<Integer> deniedQuestionMetaIds;


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
