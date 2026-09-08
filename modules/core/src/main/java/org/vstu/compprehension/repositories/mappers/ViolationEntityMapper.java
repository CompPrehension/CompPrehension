package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.common.Utils;
import org.vstu.compprehension.data.question.ExplanationTemplateInfoData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.ExplanationTemplateInfoEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.mappers.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
class ViolationEntityMapper implements Mapper<ViolationData, ViolationEntity> {

    @Override
    public @NotNull ViolationEntity map(@NotNull ViolationData source) {
        var entity = new ViolationEntity();
        entity.setId(source.getId());
        entity.setLawName(source.getLawName());
        entity.setDetailedLawName(source.getDetailedLawName());
        entity.setViolationFacts(Utils.copy(source.getViolationFacts()));
        entity.setExplanationTemplateInfo(toEntities(source.getExplanationTemplateInfo(), entity));
        return entity;
    }

    private @NotNull List<ExplanationTemplateInfoEntity> toEntities(
            @Nullable List<ExplanationTemplateInfoData> source, @NotNull ViolationEntity owner) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().map(info -> {
            var entity = new ExplanationTemplateInfoEntity();
            entity.setId(info.getId());
            entity.setFieldName(info.getFieldName());
            entity.setValue(info.getValue());
            entity.setViolation(owner);
            return entity;
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}
