package org.vstu.compprehension.controllers;

import lombok.SneakyThrows;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class Html5PathsController {

    @SneakyThrows
    @RequestMapping( method = {RequestMethod.OPTIONS, RequestMethod.GET, RequestMethod.POST}, path = {"/pages/**", "/"} )
    public String forwardPaths(HttpServletRequest request, @RequestParam Map<String, String> requestParams) {
        var decodeCodec = new URLCodec();
        var rawBody = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        var rawParams = Arrays.stream(rawBody.split("&"))
                .map(x -> x.split("="))
                .collect(Collectors.toMap(x -> x[0], x -> { try { return decodeCodec.decode(x[1]); } catch (Exception e) { return ""; }}));
        rawParams.putAll(requestParams);

        return "index.html";
    }
}
