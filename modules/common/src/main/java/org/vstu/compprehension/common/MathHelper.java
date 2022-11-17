package org.vstu.compprehension.common;

public class MathHelper {

    /**
     * Линейно преобразовывает значения одного множество в значение другого множества
     */
    public static float linearInterpolateToNewRange(float value,
                                                    float valueRangeStart,
                                                    float valueRangeEnd,
                                                    float newRangeStart,
                                                    float newRangeEnd) {
        float m = (newRangeEnd - newRangeStart) / (valueRangeEnd - valueRangeStart);
        return newRangeStart + m * (value - valueRangeStart);
    }
}
