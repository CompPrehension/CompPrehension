package org.vstu.compprehension.service;

/**
 * Константы LTI 1.3, используемые в контроллере и стратегии grade passback.
 */
public final class LtiConstants {
    /**
     * Key ID RSA-ключа, используемый в двух местах:
     * <ul>
     *   <li>Заголовок {@code kid} подписанного JWT-assertion, который CompPrehension отправляет
     *       на token-эндпоинт Moodle при получении access token (LTI AGS).</li>
     *   <li>Поле {@code kid} в ответе JWKS-эндпоинта ({@code /lti/jwks}), где публикуется
     *       соответствующий публичный ключ.</li>
     * </ul>
     * Moodle извлекает {@code kid} из заголовка JWT, находит ключ с таким же {@code kid} в JWKS
     * и использует его для верификации подписи. Значения должны совпадать.
     */
    public static final String KID = "compph-lti-1";

    private LtiConstants() {}
}
