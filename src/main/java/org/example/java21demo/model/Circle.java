package org.example.java21demo.model;

public record Circle(double radius) implements Shape {
    public Circle {
        if (radius <= 0) throw new IllegalArgumentException("半径必须大于 0");
    }
}
