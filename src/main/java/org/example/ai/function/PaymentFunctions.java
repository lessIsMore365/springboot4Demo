package org.example.ai.function;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import org.example.entity.PaymentOrder;
import org.example.service.PaymentService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentFunctions {

    private static final String ORDER_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "paymentMethod": {"type": "string", "enum": ["ALIPAY", "WECHAT"], "description": "支付方式"},
                "status": {"type": "string", "enum": ["PENDING", "SUCCESS", "CLOSED", "REFUND"], "description": "订单状态"},
                "page": {"type": "integer", "description": "页码，默认1"},
                "size": {"type": "integer", "description": "每页条数，默认10"}
              }
            }""";

    private final PaymentService paymentService;
    private final AiFunctionRegistry functionRegistry;

    public PaymentFunctions(PaymentService paymentService, AiFunctionRegistry functionRegistry) {
        this.paymentService = paymentService;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    void registerFunctions() {
        functionRegistry.register(queryOrders());
        functionRegistry.register(queryOrderDetail());
    }

    public AiFunction queryOrders() {
        return new AiFunction() {
            @Override
            public String name() { return "query_payment_orders"; }

            @Override
            public String description() {
                return "查询支付订单列表，可按支付方式(ALIPAY/WECHAT)和状态(PENDING/SUCCESS/CLOSED/REFUND)筛选";
            }

            @Override
            public Map<String, Object> parameters() {
                return parseSchema(ORDER_SCHEMA);
            }

            @Override
            public Object execute(Map<String, Object> args) {
                int page = args.get("page") instanceof Number n ? n.intValue() : 1;
                int size = args.get("size") instanceof Number n ? n.intValue() : 10;
                String method = (String) args.get("paymentMethod");
                String status = (String) args.get("status");

                Page<PaymentOrder> result = paymentService.getOrdersByPage(page, size);
                List<Map<String, Object>> list = result.getRecords().stream().map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("orderNo", o.getOrderNo());
                    m.put("paymentMethod", o.getPaymentMethod());
                    m.put("amount", o.getAmount());
                    m.put("subject", o.getSubject());
                    m.put("status", o.getStatus());
                    m.put("createTime", o.getCreateTime());
                    return m;
                }).toList();

                if (method != null) {
                    list = list.stream().filter(o -> method.equals(o.get("paymentMethod"))).toList();
                }
                if (status != null) {
                    list = list.stream().filter(o -> status.equals(o.get("status"))).toList();
                }

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("total", result.getTotal());
                response.put("page", page);
                response.put("orders", list);
                return response;
            }
        };
    }

    public AiFunction queryOrderDetail() {
        return new AiFunction() {
            @Override
            public String name() { return "query_order_detail"; }

            @Override
            public String description() { return "根据订单号查询单笔支付订单详情"; }

            @Override
            public Map<String, Object> parameters() {
                return parseSchema("""
                    {
                      "type": "object",
                      "properties": {
                        "orderNo": {"type": "string", "description": "订单号"}
                      },
                      "required": ["orderNo"]
                    }""");
            }

            @Override
            public Object execute(Map<String, Object> args) {
                String orderNo = (String) args.get("orderNo");
                PaymentOrder order = paymentService.queryOrder(orderNo);
                if (order == null) return Map.of("error", "订单不存在: " + orderNo);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderNo", order.getOrderNo());
                m.put("paymentMethod", order.getPaymentMethod());
                m.put("amount", order.getAmount());
                m.put("subject", order.getSubject());
                m.put("body", order.getBody());
                m.put("status", order.getStatus());
                m.put("tradeNo", order.getTradeNo());
                m.put("paidTime", order.getPaidTime());
                m.put("refundAmount", order.getRefundAmount());
                m.put("createTime", order.getCreateTime());
                return m;
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
