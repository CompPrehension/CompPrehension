package org.vstu.compprehension.models.businesslogic.domains.terms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.DetectionMethodDeserializer;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.ManyLocalizedObjectDeserializer;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.SingleLocalizedObjectDeserializer;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DomainTermElement {
    @JsonDeserialize(using = ManyLocalizedObjectDeserializer.class)
    private Map<String, List<String>> pattern = new HashMap<>();

    @JsonProperty("detection_method")
    @JsonDeserialize(using = DetectionMethodDeserializer.class)
    private DomainTermDetectionMethod detectionMethod = DomainTermDictionary.DEFAULT_DETECTION_METHOD;

    @JsonProperty("threshold")
    private float detectionThreshold = DomainTermDictionary.DEFAULT_THRESHOLD;

    @JsonDeserialize(using = SingleLocalizedObjectDeserializer.class)
    private Map<String, String> explanations = new HashMap<>();

    public List<String> getPossiblePatterns(Language lang) {
        if (pattern.size() == 1) {
            return pattern.values().iterator().next();
        }
        return pattern.get(lang.toLocaleString());
    }

    public boolean isMalformed() {
        return pattern.isEmpty() || explanations.isEmpty();
    }

    public boolean isMalformed(Language lang) {
        return !pattern.containsKey(lang.toLocaleString()) || !pattern.containsKey(lang.toLocaleString());
    }

    public String getExplanation(Language lang) {
        if (explanations.size() == 1) {
            return explanations.values().iterator().next();
        }
        return explanations.get(lang.toLocaleString());
    }
}
