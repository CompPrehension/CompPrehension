package org.vstu.compprehension.models.businesslogic.domains.terms.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.methods.FuzzyTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.methods.GrammaticalCaseTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.methods.RegexTermDetectionMethod;

import java.io.IOException;

public class DetectionMethodDeserializer extends JsonDeserializer<DomainTermDetectionMethod> {
    @Override
    public DomainTermDetectionMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        switch (value.toLowerCase()) {
            case "fuzzy": return new FuzzyTermDetectionMethod();
            case "regex": return new RegexTermDetectionMethod();
            case "case":  return new GrammaticalCaseTermDetectionMethod();
            default: throw new IllegalArgumentException("Unknown detection method: " + value);
        }
    }
}
