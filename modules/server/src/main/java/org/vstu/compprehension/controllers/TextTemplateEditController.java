package org.vstu.compprehension.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.vstu.compprehension.dto.TextTemplateDto;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("api/text-template-edit")
@Log4j2
public class TextTemplateEditController {

    @RequestMapping(value = {"/get-all"}, method = { RequestMethod.GET }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    public List<TextTemplateDto> getAll(HttpServletRequest request) throws Exception {
        return new ArrayList<>();
    }

    @RequestMapping(value = {"/save"}, method = { RequestMethod.POST }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    public List<TextTemplateDto> save(@RequestBody List<TextTemplateDto> dto, HttpServletRequest request) throws Exception {
        return dto;
    }

    @RequestMapping(method = { RequestMethod.GET }, produces = "application/json",
            consumes = "application/json")
    @ResponseBody
    public List<TextTemplateDto> find(@RequestParam String value, HttpServletRequest request) throws Exception {
        return new ArrayList<>();
    }
}
