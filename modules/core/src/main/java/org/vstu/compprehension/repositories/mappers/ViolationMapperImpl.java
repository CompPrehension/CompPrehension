package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.question.ExplanationTemplateInfoData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.repositories.Facts;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
class ViolationMapperImpl implements ViolationMapper {

    @Override
    public @NotNull ViolationData map(@NotNull ViolationEntity violation,
                                      @NotNull InteractionEntity owner) {
        var data = new ViolationData();
        data.setId(violation.getId());
        data.setLawName(violation.getLawName());
        data.setDetailedLawName(violation.getDetailedLawName());
        data.setInteractionType(owner.getInteractionType());
        data.setViolationFacts(Facts.copy(violation.getViolationFacts()));
        data.setExplanationTemplateInfo(violation.getExplanationTemplateInfo().stream()
                .map(t -> new ExplanationTemplateInfoData(t.getId(), t.getFieldName(), t.getValue()))
                .collect(Collectors.toCollection(ArrayList::new)));
        return data;
    }
}
