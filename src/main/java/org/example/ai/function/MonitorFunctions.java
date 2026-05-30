package org.example.ai.function;

import jakarta.annotation.PostConstruct;
import org.example.service.DictService;
import org.example.service.OnlineUserService;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MonitorFunctions {

    private final DictService dictService;
    private final OnlineUserService onlineUserService;
    private final AiFunctionRegistry functionRegistry;

    public MonitorFunctions(DictService dictService, OnlineUserService onlineUserService,
                            AiFunctionRegistry functionRegistry) {
        this.dictService = dictService;
        this.onlineUserService = onlineUserService;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    void registerFunctions() {
        functionRegistry.register(getJvmStatus());
        functionRegistry.register(getOnlineUsers());
        functionRegistry.register(getDictData());
    }

    public AiFunction getJvmStatus() {
        return new AiFunction() {
            @Override
            public String name() { return "get_jvm_status"; }

            @Override
            public String description() { return "获取JVM运行状态：堆内存使用量、CPU负载、线程数等"; }

            @Override
            public Map<String, Object> parameters() {
                return Map.of("type", "object", "properties", Map.of());
            }

            @Override
            public Object execute(Map<String, Object> args) {
                MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
                OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
                ThreadMXBean threads = ManagementFactory.getThreadMXBean();

                long heapUsed = memory.getHeapMemoryUsage().getUsed();
                long heapMax = memory.getHeapMemoryUsage().getMax();
                double heapPercent = heapMax > 0 ? heapUsed * 100.0 / heapMax : 0;

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("heapUsedMB", heapUsed / 1024 / 1024);
                result.put("heapMaxMB", heapMax / 1024 / 1024);
                result.put("heapUsagePercent", String.format("%.1f%%", heapPercent));
                result.put("systemLoadAverage", String.format("%.2f", os.getSystemLoadAverage()));
                result.put("availableProcessors", os.getAvailableProcessors());
                result.put("threadCount", threads.getThreadCount());
                result.put("peakThreadCount", threads.getPeakThreadCount());
                return result;
            }
        };
    }

    public AiFunction getOnlineUsers() {
        return new AiFunction() {
            @Override
            public String name() { return "get_online_users"; }

            @Override
            public String description() { return "获取当前在线用户列表"; }

            @Override
            public Map<String, Object> parameters() {
                return Map.of("type", "object", "properties", Map.of());
            }

            @Override
            public Object execute(Map<String, Object> args) {
                return Map.of("onlineUsers", onlineUserService.getOnlineUsers());
            }
        };
    }

    public AiFunction getDictData() {
        return new AiFunction() {
            @Override
            public String name() { return "get_dict_data"; }

            @Override
            public String description() {
                return "获取字典数据，dictType可选值: payment_method(支付方式), order_status(订单状态), recon_status(对帐状态), recon_diff_type(差异类型), user_status(用户状态)";
            }

            @Override
            public Map<String, Object> parameters() {
                return parseSchema("""
                    {
                      "type": "object",
                      "properties": {
                        "dictType": {"type": "string", "description": "字典类型编码"}
                      },
                      "required": ["dictType"]
                    }""");
            }

            @Override
            public Object execute(Map<String, Object> args) {
                String dictType = (String) args.get("dictType");
                var list = dictService.getDataByType(dictType);
                List<Map<String, Object>> items = list.stream().map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("label", d.getDictLabel());
                    m.put("value", d.getDictValue());
                    return m;
                }).toList();
                return Map.of("dictType", dictType, "items", items);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseSchema(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
