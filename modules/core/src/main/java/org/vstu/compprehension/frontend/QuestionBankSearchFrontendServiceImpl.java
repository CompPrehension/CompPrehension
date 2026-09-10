package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class QuestionBankSearchFrontendServiceImpl implements QuestionBankSearchFrontendService {
    private final DomainFactory domainFactory;
    private final QuestionBank questionStorage;

    @Override
    public QuestionBankSearchStatsDto search(QuestionBankSearchRequestDto searchRequest) {
        var domain = domainFactory.getDomain(searchRequest.getDomainId());

        var targetConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var deniedConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var targetLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .map(c -> domain.getLaw(c.getName()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var deniedLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .map(c -> domain.getLaw(c.getName()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var targetTags = searchRequest.getTags().stream()
                .map(domain::getTag)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var targetSkills = searchRequest.getSkills().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .map(c -> domain.getSkill(c.getName()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var deniedSkills = searchRequest.getSkills().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .map(c -> domain.getSkill(c.getName()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        var qr = QuestionRequest.builder()
                .targetConcepts(targetConcepts)
                .deniedConcepts(deniedConcepts)
                .targetLaws(targetLaws)
                .deniedLaws(deniedLaws)
                .targetSkills(targetSkills)
                .deniedSkills(deniedSkills)
                .complexity(searchRequest.getComplexity())
                .targetTags(targetTags)
                .domainShortname(domain.getShortnameForQuestionSearch())
                .build();
        qr = domain.ensureQuestionRequestValid(qr);

        var questionsLimit = searchRequest.getLimit();
        if (questionsLimit < 0 || questionsLimit > 100) {
            throw new IllegalArgumentException("Limit must be in range [0, 100]");
        }

        return questionStorage.getStatsByQuestionRequest(qr, questionsLimit);
    }
}
