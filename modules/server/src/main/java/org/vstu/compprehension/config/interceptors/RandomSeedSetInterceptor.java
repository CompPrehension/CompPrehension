package org.vstu.compprehension.config.interceptors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.utils.RandomProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RandomSeedSetInterceptor implements HandlerInterceptor {
    private final RandomProvider randomProvider;

    @Autowired
    public RandomSeedSetInterceptor(RandomProvider randomProvider) {
        this.randomProvider = randomProvider;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        var parameterMap = request.getParameterMap();
        var rawSeedValue = parameterMap.getOrDefault("compph_seed", null);
        if (rawSeedValue != null && rawSeedValue.length > 0) {
            Integer intSeedValue = StringHelper.tryParseInt(rawSeedValue[0]);
            if (intSeedValue != null)
                randomProvider.reset(intSeedValue);
        }
        return true;
        //return super.preHandle(request, response, handler);
    }
}
