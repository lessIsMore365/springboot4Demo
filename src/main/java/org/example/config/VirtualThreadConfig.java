package org.example.config;


import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@Configuration
@EnableAsync
public class VirtualThreadConfig {


    // 作为 Spring 的全局 TaskExecutor（@Async 可注入使用）
    // 使用 TaskExecutorAdapter 包装虚拟线程执行器以提供更好的 Spring 集成
    @Bean
    public AsyncTaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }


    // 将 Tomcat 的请求处理线程切换为虚拟线程池（每个请求一个虚拟线程）
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutor() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}