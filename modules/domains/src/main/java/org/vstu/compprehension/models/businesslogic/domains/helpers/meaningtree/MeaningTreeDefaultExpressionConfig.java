package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import java.util.HashMap;

/**
 * Base config for Meaning Tree required for correct Expression domain functioning
 */
public class MeaningTreeDefaultExpressionConfig extends HashMap<String, String> {
    public MeaningTreeDefaultExpressionConfig() {
        super();
        put("skipErrors", "true");
        put("translationUnitMode", "false");
        put("expressionMode", "true");
        put("disableCompoundComparisonConversion", "true");
    };
}
