package org.vstu.compprehension.config;


import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class LoggableDispatcherServlet extends DispatcherServlet {

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }
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
                log.error(e.toString());
        } finally {
            if (isLogNeeded)
                logAfterRequest(request, response, handler);
            updateResponse(response);
        }
    }

    private void logBeforeRequest(HttpServletRequest requestToCache, HandlerExecutionChain handler) {
        ThreadContext.put("correlationId", UUID.randomUUID().toString());
        ThreadContext.put("sessionId", requestToCache.getSession().getId());

        val parameters = Collections.list(requestToCache.getParameterNames())
                .stream()
                .collect(Collectors.toMap(parameterName -> parameterName, requestToCache::getParameterValues));

        log.info("Start processing request: path: '{}', params: {}",
                requestToCache.getRequestURI(),
                parameters);
    }

    private void logAfterRequest(HttpServletRequest requestToCache,  HttpServletResponse responseToCache, HandlerExecutionChain handler) {
        log.info("Finish processing request: path: '{}'",
                requestToCache.getRequestURI()/*,
                getResponsePayload(responseToCache)*/);

        ThreadContext.clearMap();
    }

    /*
    private void log(HttpServletRequest requestToCache, HttpServletResponse responseToCache, HandlerExecutionChain handler) {
        LogMessage log = new LogMessage();
        log.setHttpStatus(responseToCache.getStatus());
        log.setHttpMethod(requestToCache.getMethod());
        log.setPath(requestToCache.getRequestURI());
        log.setClientIp(requestToCache.getRemoteAddr());
        log.setJavaMethod(handler.toString());
        log.setResponse(getResponsePayload(responseToCache));
        logger.info(log);
    }*/

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