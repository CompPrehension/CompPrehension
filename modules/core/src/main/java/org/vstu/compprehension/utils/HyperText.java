package org.vstu.compprehension.utils;

import java.util.Objects;

public class HyperText {

    private String text;

    public HyperText(String text) {
        this.text = text;
    }

    public HyperText(StringBuilder text) {
        this.text = text.toString();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public String getText() {
        return toString();
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HyperText hyperText)) return false;
        return Objects.equals(text, hyperText.text);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }
}
