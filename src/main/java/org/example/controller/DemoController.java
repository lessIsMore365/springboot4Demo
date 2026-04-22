package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/demo")
public class DemoController {


    @GetMapping("/hello")
    public String hello() {
// 打印线程信息，确认是虚拟线程
        System.out.println("Handling request in: " + Thread.currentThread());
        return "hello virtual thread";
    }
}
