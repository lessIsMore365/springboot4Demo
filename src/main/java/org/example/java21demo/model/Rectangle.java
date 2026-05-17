package org.example.java21demo.model;

public record Rectangle(double width, double height) implements Shape {
    public Rectangle {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("宽高必须大于 0");
    }
}
