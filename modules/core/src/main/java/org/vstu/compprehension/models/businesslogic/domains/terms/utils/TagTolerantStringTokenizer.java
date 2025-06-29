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

    public static int[] findWordPosition(String text, int wordIndex) {
        if (text == null || wordIndex < 0) {
            return null;
        }

        int currentWordIndex = 0;
        int i = 0;
        int textLength = text.length();

        while (i < textLength) {
            // Пропускаем теги
            if (text.charAt(i) == '<') {
                while (i < textLength && text.charAt(i) != '>') {
                    i++;
                }
                if (i < textLength) {
                    i++; // пропускаем '>'
                }
                continue;
            }

            // Пропускаем пробелы
            if (Character.isWhitespace(text.charAt(i))) {
                i++;
                continue;
            }

            // Нашли начало слова
            int wordStart = i;

            // Найдем конец слова
            while (i < textLength && !Character.isWhitespace(text.charAt(i)) && text.charAt(i) != '<') {
                i++;
            }

            int wordEnd = i;

            // Если это нужное нам слово
            if (currentWordIndex == wordIndex) {
                return new int[]{wordStart, wordEnd};
            }

            currentWordIndex++;
        }

        if (wordIndex >= currentWordIndex) {
            return new int[] {text.length(), text.length()};
        }

        // Слово не найдено
        return new int[]{-1, -1};
    }
}