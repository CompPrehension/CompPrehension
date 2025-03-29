package org.vstu.compprehension.utils;

public class HyperText {

    private StringBuilder text;

    public HyperText(String text) {
        this.text = new StringBuilder(text);
    }

    public HyperText append(String t) {
        text.append(t);
        return this;
    }

    public HyperText append(HyperText t) {
        text.append(t.getText());
        return this;
    }

    public String getText() {
        return text.toString();
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
