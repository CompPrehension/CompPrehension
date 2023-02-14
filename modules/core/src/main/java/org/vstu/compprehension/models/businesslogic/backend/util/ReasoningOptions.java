package org.vstu.compprehension.models.businesslogic.backend.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReasoningOptions {
    boolean removeInputFactsFromResult = false;
    Set<String> verbs = null;
    String uniqueSolutionKey = null;

}
