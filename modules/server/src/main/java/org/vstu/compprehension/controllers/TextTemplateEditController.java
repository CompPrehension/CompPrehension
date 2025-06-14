package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.Service.TextTemplatesService;
import org.vstu.compprehension.dto.TextTemplateDto;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("api/text-template-edit")
@Log4j2
@AllArgsConstructor
public class TextTemplateEditController {

    private final TextTemplatesService textTemplatesService;
    private final DomainFactory domainFactory;

    //FIXME костыль для поддержки единственного домена
    private String getDomainId() {
        return domainFactory.getDomainIds().stream()
            .filter(domainId -> domainFactory.getDomain(domainId) instanceof ProgrammingLanguageExpressionDTDomain)
            .findFirst()
            .orElseThrow();
    }

    @RequestMapping(
        value = {"/get-all"},
        method = {RequestMethod.GET},
        produces = "application/json",
        consumes = "application/json"
    )
    @ResponseBody
    public List<TextTemplateDto> getAll(HttpServletRequest request) throws Exception {
        return new ArrayList<>();
    }

    @RequestMapping(
        value = {"/save"},
        method = {RequestMethod.POST},
        produces = "application/json",
        consumes = "application/json"
    )
    @ResponseBody
    public List<TextTemplateDto> save(@RequestBody List<TextTemplateDto> dto, HttpServletRequest request) throws Exception {
        textTemplatesService.updateAndSave(dto, getDomainId());
        return dto;
    }

    @RequestMapping(
        method = {RequestMethod.GET},
        produces = "application/json",
        consumes = "application/json"
    )
    @ResponseBody
    public List<TextTemplateDto> find(@RequestParam String value, HttpServletRequest request) throws Exception {
        return textTemplatesService.search(value, getDomainId());
    }
}
