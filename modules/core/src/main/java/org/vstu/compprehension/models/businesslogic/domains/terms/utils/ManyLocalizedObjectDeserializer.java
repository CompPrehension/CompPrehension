package org.vstu.compprehension.models.businesslogic.domains.terms.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.*;

public class ManyLocalizedObjectDeserializer extends JsonDeserializer<Map<String, List<String>>> {
    private final String defaultLang = "EN";

    @Override
    public Map<String, List<String>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        Map<String, List<String>> result = new LinkedHashMap<>();

        if (node.isTextual()) {
            result.put(defaultLang, List.of(node.textValue()));
        } else if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode element : node) {
                if (element.isTextual()) {
                    values.add(element.textValue());
                } else {
                    throw JsonMappingException.from(p, "Expected array of strings");
                }
            }
            result.put(defaultLang, values);
        } else if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String lang = entry.getKey();
                JsonNode valueNode = entry.getValue();

                if (valueNode.isTextual()) {
                    result.put(lang, List.of(valueNode.textValue()));
                } else if (valueNode.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode element : valueNode) {
                        if (element.isTextual()) {
                            values.add(element.textValue());
                        } else {
                            throw JsonMappingException.from(p, "Expected string in array for key: " + lang);
                        }
                    }
                    result.put(lang, values);
                } else {
                    throw JsonMappingException.from(p, "Expected string or array for key: " + lang);
                }
            }
        } else {
            throw JsonMappingException.from(p, "Expected string, array, or object for 'pattern'");
        }

        return result;
    }
}
