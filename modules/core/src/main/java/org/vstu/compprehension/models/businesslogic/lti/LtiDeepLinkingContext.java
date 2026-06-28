package org.vstu.compprehension.models.businesslogic.lti;

import java.util.List;

/**
 * Данные LTI 1.3 Deep Linking запроса текущей сессии, нужные для сборки ответного
 * {@code LtiDeepLinkingResponse} и для дедупа уже добавленных активностей через AGS.
 *
 * <p>Присутствует только если запуск пришёл как {@code LtiDeepLinkingRequest}
 * (из блока или из штатного "Select content"); иначе {@code null}.
 *
 * @param platformIssuer   канонический issuer платформы (Moodle wwwroot) — идёт в {@code aud}
 *                         ответа и используется для поиска LTI-регистрации/ключа подписи
 * @param deploymentId     deployment_id из запроса
 * @param deepLinkReturnUrl URL, куда POST-ится подписанный ответ (contentitem_return.php)
 * @param data             значение из {@code deep_linking_settings.data}; обязано
 *                         вернуться без изменений (может быть {@code null})
 * @param lineitemsUrl     AGS-контейнер line items курса (для дедупа); может быть {@code null}
 * @param agsScopes        выданные AGS-скоупы (может быть {@code null})
 */
public record LtiDeepLinkingContext(
        String platformIssuer,
        String deploymentId,
        String deepLinkReturnUrl,
        String data,
        String lineitemsUrl,
        List<String> agsScopes
) {
}
