package org.vstu.compprehension.frontend.dto.feedback;

import org.jetbrains.annotations.Nullable;

/** Объяснение заблуждения, которое студент назвал причиной ответа; пусто, если причина другая. */
public record ClarificationFeedbackDto(@Nullable String explanation) {
}
