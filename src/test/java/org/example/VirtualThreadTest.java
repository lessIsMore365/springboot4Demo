package org.example;

import java.util.concurrent.Executors;

public class VirtualThreadTest {
    public static void main(String[] args) {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                System.out.println("Hello Virtual Thread");
            });
        }
    }
}
