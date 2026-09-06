package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Ответы одного взаимодействия.
 * <p>
 * Идентификатор взаимодействия нужен, чтобы сослаться на эти ответы при записи
 * следующего: показ правильного ответа переносит их в новое взаимодействие.
 */
public record InteractionResponsesData(long interactionId, @NotNull List<ResponseData> responses) {
}
