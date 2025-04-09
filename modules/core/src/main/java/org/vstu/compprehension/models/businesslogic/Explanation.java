package org.vstu.compprehension.models.businesslogic;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class Explanation {

    /**
     * Type of explanation
     */
    public enum Type {
        HINT,
        ERROR
    }


    private final Type type;
    private SequencedSet<Explanation> children = new LinkedHashSet<>();

    @Setter
    private @NotNull HyperText rawMessage;

    @Setter
    private String currentDomainLawName;

    public Explanation(Type t, @NotNull String message) {
        this(t, new HyperText(message));
    }

    public Explanation(Type t, @NotNull HyperText message) {
        this.type = t;
        this.rawMessage = message;
    }

    public Explanation(Type t, @NotNull String message, List<Explanation> children) {
        this(t, new HyperText(message), children);
    }

    public Explanation(Type t, @NotNull HyperText message, List<Explanation> children) {
        this.type = t;
        this.rawMessage = message;
        this.children = new LinkedHashSet<>(children);
    }

    public static Explanation empty(Type t) {
        return new Explanation(t, "");
    }

    public boolean isEmpty() {
        return rawMessage.getText().isEmpty() && children.isEmpty();
    }

    public boolean containsAggregated() {
        return !children.isEmpty();
    }

    public static Explanation aggregate(Type t, List<Explanation> explanationList) {
        return new Explanation(t, "", explanationList);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Explanation that)) return false;
        return Objects.equals(rawMessage, that.rawMessage) && type == that.type && Objects.equals(children, that.children) && Objects.equals(currentDomainLawName, that.currentDomainLawName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawMessage, type, children, currentDomainLawName);
    }

    public Set<String> getDomainLawNames() {
        return Stream.concat(
                Stream.of(currentDomainLawName),
                children.stream().map(Explanation::getDomainLawNames).flatMap(Set<String>::stream)
        ).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public HyperText toHyperText(Language lang) {
        return toHyperText(lang, false);
    }

    public HyperText toHyperText(Language lang, boolean collapse) {
        if (children.isEmpty()) {
            return new HyperText(rawMessage.getText());
        } else if (children.size() == 1 && rawMessage.getText().isEmpty()) {
            return children.getFirst().toHyperText(lang, collapse);
        } else {
            return recursiveBuildHyperText(lang, collapse, this);
        }
    }

    /**
     * Получить общий префикс для сообщений объяснений из списка
     * @param explanations список объяснений
     * @param defaultVal значение префикса, если ничего не совпало
     * @return общий префикс
     */
    public static String getCommonPrefix(Collection<Explanation> explanations, String defaultVal) {
        String commonChildrenPrefix = StringUtils.getCommonPrefix(explanations.stream().
                map(Explanation::getRawMessage).map(HyperText::getText)
                .filter(x -> !x.startsWith("<i>"))
                .toList().toArray(new String[0]));
        if (commonChildrenPrefix.isEmpty()) {
            commonChildrenPrefix = defaultVal;
        }

        String[] stopWords = {"because", "что"};
        for (String stopWord : stopWords) {
            if (commonChildrenPrefix.lastIndexOf(stopWord) != -1) {
                commonChildrenPrefix = commonChildrenPrefix.substring(0,
                        commonChildrenPrefix.lastIndexOf(stopWord) + stopWord.length()) + " ";
            }
        }
        return commonChildrenPrefix;
    }

    private HyperText recursiveBuildHyperText(Language lang, boolean collapse, Explanation parent) {
        String commonChildrenPrefix = getCommonPrefix(children, "").trim();
        HyperText details = new HyperText(String.format("<details class=\"rounded\" %s>", collapse ? "" : "open"));
        String headerPrefix = commonChildrenPrefix;
        if (rawMessage.getText().trim().startsWith(commonChildrenPrefix)) {
            headerPrefix = "";
        }
        details.append("<summary>").append(headerPrefix.concat(rawMessage.getText().trim())).append("</summary>");

        details.append("<ul>");
        for (Explanation child : children) {
            HyperText ht = child.toHyperText(lang, collapse);
            details.append("<li class=\"p-1\">").append(ht.getText().replace(commonChildrenPrefix, "")).append("</li>");
        }
        details.append("</ul>");

        details.append("</details>");
        return details;
    }
}
