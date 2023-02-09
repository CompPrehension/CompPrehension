package org.vstu.compprehension.models.businesslogic.backend.util;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class ReasoningOptions {
    boolean removeInputFactsFromResult = false;
    Set<String> verbs = null;

}
