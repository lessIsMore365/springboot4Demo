package org.springframework.security.web.util.matcher;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Spring Security 7.x compatibility bridge.
 * RequestVariablesExtractor was removed in Spring Security 7.x but is
 * still referenced by Spring Authorization Server 1.x.
 */
@Deprecated
public interface RequestVariablesExtractor {
    Map<String, String> extractUriTemplateVariables(HttpServletRequest request);
}
