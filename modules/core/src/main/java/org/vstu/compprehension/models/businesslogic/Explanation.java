package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.utils.HyperText;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public class Explanation {
    /**
     * Type of explanation
     */
    public enum Type {
        HINT,
        ERROR
    }

    public final HyperText rawMessage;
    public final Type type;
    public final ArrayList<Explanation> children;

    public Explanation(String rawMessage, Type type) {
        this.rawMessage = new HyperText(rawMessage);
        this.type = type;
        this.children = new ArrayList<>();
    }

    public Explanation(HyperText rawMessage, Type type) {
        this(rawMessage.getText(), type);
    }

    public Explanation(String rawMessage, Type type, ArrayList<Explanation> explanations) {
        this(new HyperText(rawMessage), type, explanations);
    }

    public static Explanation empty(Type t) {
        return new Explanation("", t);
    }

    public boolean isEmpty() {
        return rawMessage.getText().isEmpty() && children.isEmpty();
    }

    public boolean containsAggregated() {
        return !children.isEmpty();
    }

    public static Explanation aggregate(Type t, List<Explanation> explanationList) {
        return new Explanation("", t, new ArrayList<>(explanationList));
    }

    public HyperText toHyperText(Language lang) {
        return toHyperText(lang, false);
    }

    public HyperText toHyperText(Language lang, boolean collapse) {
        if (children.isEmpty()) {
            return new HyperText(rawMessage.getText());
        } else if (children.size() == 1) {
            return children.getFirst().toHyperText(lang, collapse);
        } else {
            return _recursiveToHyperText(lang, collapse, this);
        }
    }

    private HyperText _recursiveToHyperText(Language lang, boolean collapse, Explanation parent) {
        HyperText details = new HyperText(String.format("<details class=\"rounded\" %s>", collapse ? "" : "open"));
        details.append("<summary>").append(rawMessage).append("</summary>");

        details.append("<ul>");
        for (Explanation child : children) {
            HyperText ht = child.toHyperText(lang, collapse);
            details.append("<li class=\"p-1\">").append(ht.getText().replace(parent.rawMessage.getText(), "")).append("</li>");
        }
        details.append("</ul>");

        details.append("</details>");
        return details;
    }
}
