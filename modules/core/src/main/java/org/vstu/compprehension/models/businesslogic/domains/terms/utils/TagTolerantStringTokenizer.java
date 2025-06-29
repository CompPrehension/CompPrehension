package org.vstu.compprehension.models.businesslogic.domains.terms.utils;

import java.util.ArrayList;
import java.util.List;

public class TagTolerantStringTokenizer {
    public static String[] tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        StringBuilder tagBuffer = new StringBuilder();
        boolean insideTag = false;
        String pendingTag = null;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '<') {
                insideTag = true;
                tagBuffer.setLength(0);
                tagBuffer.append(c);
            } else if (c == '>' && insideTag) {
                insideTag = false;
                tagBuffer.append(c);
                pendingTag = tagBuffer.toString(); // запомнили <...>
            } else if (insideTag) {
                tagBuffer.append(c);
            } else if (Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    // если есть слово — добавляем и прицепляем тег, если был
                    tokens.add(token + (pendingTag != null ? pendingTag : ""));
                    pendingTag = null;
                    token.setLength(0);
                }
                // иначе просто игнорируем пробел
            } else {
                token.append(c);
            }
        }

        // последний токен
        if (token.length() > 0) {
            tokens.add(token + (pendingTag != null ? pendingTag : ""));
        }

        return tokens.toArray(String[]::new);
    }
}
