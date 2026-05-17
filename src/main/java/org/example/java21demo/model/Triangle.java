package org.example.java21demo.model;

public record Triangle(double base, double height) implements Shape {
    public Triangle {
        if (base <= 0 || height <= 0) throw new IllegalArgumentException("底和高必须大于 0");
    }
}
