package org.vstu.compprehension.config.logs;


import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import org.vstu.compprehension.config.cache.CachedHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class LoggableDispatcherServlet extends DispatcherServlet {

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!(request instanceof CachedHttpServletRequest)) {
            request = new CachedHttpServletRequest(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }
        doDispatchInternal((CachedHttpServletRequest)request, (ContentCachingResponseWrapper)response);
    }

    private void doDispatchInternal(CachedHttpServletRequest request, ContentCachingResponseWrapper response) throws Exception {
        HandlerExecutionChain handler = getHandler(request);

        // bypass files
        val uri = request.getRequestURI();
        val isLogNeeded = !uri.endsWith(".html") && !uri.endsWith(".js") && !uri.endsWith(".css");

        try {
            if (isLogNeeded)
                logBeforeRequest(request, handler);
            super.doDispatch(request, response);
        } catch (Exception e) {
            if (isLogNeeded)
                log.error("error: ", e);
            response.sendError(500, e.getMessage() + ". For additional info see logs. CorrelationId: " + request.getSession().getId());
        } finally {
            if (isLogNeeded)
                logAfterRequest(request, response, handler);
            updateResponse(response);
        }
    }

    private void logBeforeRequest(CachedHttpServletRequest requestToCache, HandlerExecutionChain handler) throws Exception {
        // init some request context variables
        ThreadContext.put("correlationId", UUID.randomUUID().toString());
        ThreadContext.put("sessionId", requestToCache.getSession().getId());
        ThreadContext.put("userId", Optional.ofNullable(requestToCache.getSession().getAttribute("currentUserId")).map(Object::toString).orElse(null));

        val parameters = Collections.list(requestToCache.getParameterNames())
                .stream()
                .collect(Collectors.toMap(parameterName -> parameterName, requestToCache::getParameterValues));

        log.info("Start processing request: path: '{}', method: '{}', params: {}, body: {}", requestToCache.getRequestURI(), requestToCache.getMethod(), parameters, getRequestBody(requestToCache));
    }

    private void logAfterRequest(HttpServletRequest requestToCache,  HttpServletResponse responseToCache, HandlerExecutionChain handler) {
        log.info("Finish processing request: path: '{}'", requestToCache.getRequestURI());
        //log.info("Response body: {}", getResponsePayload(responseToCache));

        // clear all request context variables
        ThreadContext.clearMap();
    }

    public static String getRequestBody(HttpServletRequest request) {

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream == null) {
                return "";
            }

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            char[] charBuffer = new char[128];
            int bytesRead = -1;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                stringBuilder.append(charBuffer, 0, bytesRead);
            }
        } catch (IOException ex) {
            log.error("exception on translation request body", ex);
            return "exception on translation request body";
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    log.error("exception on translation request body", ex);
                    return "exception on translation request body";
                }
            }
        }

        return stringBuilder.toString();
    }


    private String getResponsePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {

            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, 5120);
                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                }
                catch (UnsupportedEncodingException ex) {
                    // NOOP
                }
            }
        }
        return "[unknown]";
    }

    private void updateResponse(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        responseWrapper.copyBodyToResponse();
    }
}