package org.vstu.compprehension.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class HttpRequestHelper {
    public static Map<String, String> getAllRequestParams(HttpServletRequest request) {
        var result = new HashMap<String, String>();
        for (var paramKV: request.getParameterMap().entrySet()) {
            result.put(paramKV.getKey(), paramKV.getValue().length > 0 ? paramKV.getValue()[0] : "");
        }

        try {
            var decodeCodec = new URLCodec();
            var rawFormData = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
            var parsedFormData = Arrays.stream(rawFormData.split("&"))
                    .map(x -> x.split("="))
                    .collect(Collectors.toMap(x -> x[0], x -> { try { return decodeCodec.decode(x[1]); } catch (Exception e) { return ""; }}));
            result.putAll(parsedFormData);
        } catch (Exception e) {
            log.error("Cant parse form data from request {}", request);
        }
        return result;
    }
}
