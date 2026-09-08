package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.ConceptTreeItemDto;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.frontend.dto.LawTreeItemDto;
import org.vstu.compprehension.frontend.dto.SkillTreeItemDto;

import java.util.List;
import java.util.Map;

@Component
class DomainDtoMapperImpl implements DomainDtoMapper {

    @Override
    public @NotNull DomainDto map(@NotNull Domain domain, @NotNull Language language) {
        return DomainDto.builder()
                .id(domain.getDomainId())
                .displayName(domain.getDisplayName(language))
                .description(domain.getDescription(language))
                .tags(domain.getTags().keySet().stream().toList())
                .concepts(concepts(domain, language))
                .laws(laws(domain, language))
                .skills(skills(domain, language))
                .build();
    }

    private List<ConceptTreeItemDto> concepts(Domain domain, Language language) {
        Map<Concept, List<Concept>> hierarchy =
                domain.getConceptsSimplifiedHierarchy(Concept.FLAG_VISIBLE_TO_TEACHER);
        return hierarchy.entrySet().stream()
                .map(kv -> new ConceptTreeItemDto(
                        kv.getKey().getName(),
                        domain.getConceptDisplayName(kv.getKey().getName(), language),
                        kv.getKey().getBitflags(),
                        kv.getValue().stream()
                                .map(child -> new ConceptTreeItemDto(
                                        child.getName(),
                                        domain.getConceptDisplayName(child.getName(), language),
                                        child.getBitflags()))
                                .toArray(ConceptTreeItemDto[]::new)))
                .toList();
    }

    private List<LawTreeItemDto> laws(Domain domain, Language language) {
        Map<Law, List<Law>> hierarchy = domain.getLawsSimplifiedHierarchy(Law.FLAG_VISIBLE_TO_TEACHER);
        return hierarchy.entrySet().stream()
                .map(kv -> new LawTreeItemDto(
                        kv.getKey().getName(),
                        domain.getLawDisplayName(kv.getKey().getName(), language),
                        kv.getKey().getBitflags(),
                        kv.getValue().stream()
                                .map(child -> new LawTreeItemDto(
                                        child.getName(),
                                        domain.getLawDisplayName(child.getName(), language),
                                        child.getBitflags()))
                                .toArray(LawTreeItemDto[]::new)))
                .toList();
    }

    private List<SkillTreeItemDto> skills(Domain domain, Language language) {
        Map<Skill, List<Skill>> hierarchy =
                domain.getSkillSimplifiedHierarchy(Skill.FLAG_VISIBLE_TO_TEACHER);
        return hierarchy.entrySet().stream()
                .map(kv -> new SkillTreeItemDto(
                        kv.getKey().getName(),
                        domain.getSkillDisplayName(kv.getKey().getName(), language),
                        kv.getValue().stream()
                                .map(child -> new SkillTreeItemDto(
                                        child.getName(),
                                        // Историческое: подпись дочернего умения берётся из законов.
                                        domain.getLawDisplayName(child.getName(), language),
                                        child.getBitflags()))
                                .toArray(SkillTreeItemDto[]::new),
                        kv.getKey().getBitflags()))
                .toList();
    }
}
