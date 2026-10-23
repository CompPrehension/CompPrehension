package org.vstu.compprehension.config.interceptors;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.services.RandomProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class RandomSeedSetInterceptor implements HandlerInterceptor {
    private final RandomProvider randomProvider;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        var parameterMap = request.getParameterMap();
        var rawSeedValue = parameterMap.getOrDefault("compph_seed", null);
        if (rawSeedValue != null && rawSeedValue.length > 0) {
            Integer intSeedValue = StringHelper.tryParseInt(rawSeedValue[0]);
            if (intSeedValue != null)
                randomProvider.reset(intSeedValue);
        }
        return true;
    }
}
