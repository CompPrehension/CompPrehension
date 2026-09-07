package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record InteractionResponsesData(long interactionId, @NotNull List<ResponseData> responses) {
}
