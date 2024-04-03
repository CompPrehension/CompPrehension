package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.dto.QuestionBankSearchRequestDto;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

@Controller
@RequestMapping("api/question-bank")
@Log4j2
public class QuestionBankController {
    private final QuestionMetadataRepository metadataRepository;
    private final DomainFactory domainFactory;
    private final AbstractRdfStorage questionStorage;

    @Autowired
    public QuestionBankController(QuestionMetadataRepository metadataRepository, DomainFactory domainFactory, AbstractRdfStorage questionStorage) {
        this.metadataRepository = metadataRepository;
        this.domainFactory      = domainFactory;
        this.questionStorage = questionStorage;
    }

    @RequestMapping(value = {"count"}, method = { RequestMethod.POST }, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public Integer getQuestionsCount(@RequestBody QuestionBankSearchRequestDto searchRequest, HttpServletRequest request) throws Exception {
        var domain = domainFactory.getDomain(searchRequest.getDomainId());

        var targetConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .distinct()
                .toList();
        var deniedConcepts = searchRequest.getConcepts().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .flatMap(c -> domain.getConceptWithChildren(c.getName()).stream())
                .distinct()
                .toList();
        var targetLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.TARGETED))
                .map(c -> domain.getLaw(c.getName()))
                .distinct()
                .toList();
        var deniedLaws = searchRequest.getLaws().stream()
                .filter(c -> c.getKind().equals(RoleInExercise.FORBIDDEN))
                .map(c -> domain.getLaw(c.getName()))
                .distinct()
                .toList();
        var targetTags = searchRequest.getTags().stream()
                .map(domain::getTag)
                .distinct()
                .toList();

        var qr = QuestionRequest.builder()
                .targetConcepts(targetConcepts)
                .deniedConcepts(deniedConcepts)
                .targetLaws(targetLaws)
                .deniedLaws(deniedLaws)
                .complexity(searchRequest.getComplexity())
                .targetTags(targetTags)
                .domainShortname(domain.getShortName())
                .build();
        qr = domain.ensureQuestionRequestValid(qr);
        return questionStorage.countQuestions(qr);
    }
}
