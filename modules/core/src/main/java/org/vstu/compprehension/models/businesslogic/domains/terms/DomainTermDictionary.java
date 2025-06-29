package org.vstu.compprehension.models.businesslogic.domains.terms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.domains.terms.methods.FuzzyTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.DetectionMethodDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Log4j2
@Getter
public class DomainTermDictionary implements Iterable<DomainTermElement> {
    static final float DEFAULT_THRESHOLD = 0.8f;
    static final DomainTermDetectionMethod DEFAULT_DETECTION_METHOD = new FuzzyTermDetectionMethod();

    @JsonProperty("detection_method")
    @JsonDeserialize(using = DetectionMethodDeserializer.class)
    private DomainTermDetectionMethod detectionMethod = DEFAULT_DETECTION_METHOD;

    @JsonProperty("threshold")
    private float detectionThreshold = DEFAULT_THRESHOLD;

    private List<DomainTermElement> terms = new ArrayList<>();

    public static DomainTermDictionary fromString(String s) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            var result = mapper.readValue(s, DomainTermDictionary.class);
            if (result.terms.isEmpty()) {
                log.warn("Loaded TermDictionary from string has no terms, so cannot be used in domain annotating");
            }
            return result;
        } catch (JsonProcessingException e) {
            log.error("YAML parsing failed: e");
            return new DomainTermDictionary();
        }
    }

    public static DomainTermDictionary fromURL(URL url) {
        try (InputStream is = url.openStream()) {
            if (is == null) {
                log.error("Could not load resource: {}", url);
                return new DomainTermDictionary();
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            var result = mapper.readValue(is, DomainTermDictionary.class);
            if (result.terms.isEmpty()) {
                log.warn("Loaded TermDictionary from %s has no terms, so cannot be used in domain annotating".formatted(url));
            }
            return result;
        } catch (IOException e) {
            log.error("Error reading YAML file: {}", e.getMessage());
            return new DomainTermDictionary();
        }
    }

    public float getDetectionThreshold(DomainTermElement term) {
        return term.getDetectionThreshold() != DEFAULT_THRESHOLD ?
                term.getDetectionThreshold() : this.detectionThreshold;
    }

    public DomainTermDetectionMethod getDetectionMethod(DomainTermElement term) {
        return term.getDetectionMethod() != DEFAULT_DETECTION_METHOD ? term.getDetectionMethod() : this.detectionMethod;
    }

    @NotNull
    @Override
    public Iterator<DomainTermElement> iterator() {
        return terms.iterator();
    }
}
