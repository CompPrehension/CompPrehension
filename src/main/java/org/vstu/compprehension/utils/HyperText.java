package org.vstu.compprehension.utils;

public class HyperText {

    private String text;

    public HyperText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
