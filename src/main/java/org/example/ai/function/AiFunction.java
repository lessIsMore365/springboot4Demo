package org.example.ai.function;

import java.util.Map;

/**
 * AI 可调用的函数定义
 */
public interface AiFunction {

    /** 函数名称（LLM 使用的标识） */
    String name();

    /** 函数描述（LLM 据此判断何时调用） */
    String description();

    /** JSON Schema 参数定义 */
    Map<String, Object> parameters();

    /** 执行函数 */
    Object execute(Map<String, Object> args);
}
