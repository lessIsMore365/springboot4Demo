package org.springframework.security.web.util.matcher;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;

/**
 * Spring Security 7.x compatibility bridge.
 * AntPathRequestMatcher was removed in Spring Security 7.x but is
 * still referenced by Spring Authorization Server 1.x.
 */
@Deprecated
public final class AntPathRequestMatcher implements RequestMatcher, RequestVariablesExtractor {

    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

    private final String pattern;
    private final String httpMethod;
    private final boolean caseSensitive;

    public AntPathRequestMatcher(String pattern) {
        this(pattern, null);
    }

    public AntPathRequestMatcher(String pattern, String httpMethod) {
        this(pattern, httpMethod, true);
    }

    public AntPathRequestMatcher(String pattern, String httpMethod, boolean caseSensitive) {
        this(pattern, httpMethod, caseSensitive, null);
    }

    public AntPathRequestMatcher(String pattern, String httpMethod, boolean caseSensitive,
                                  org.springframework.web.util.UrlPathHelper urlPathHelper) {
        Assert.hasText(pattern, "Pattern cannot be null or empty");
        this.pattern = pattern;
        this.httpMethod = httpMethod;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (this.httpMethod != null && !this.httpMethod.equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String url = getRequestPath(request);
        if (this.caseSensitive) {
            return matcher.match(this.pattern, url);
        }
        return matcher.match(this.pattern, url.toLowerCase());
    }

    @Override
    public MatchResult matcher(HttpServletRequest request) {
        if (!matches(request)) {
            return MatchResult.notMatch();
        }
        return MatchResult.match(extractUriTemplateVariables(request));
    }

    @Override
    public Map<String, String> extractUriTemplateVariables(HttpServletRequest request) {
        if (this.httpMethod != null && !this.httpMethod.equalsIgnoreCase(request.getMethod())) {
            return Collections.emptyMap();
        }
        String url = getRequestPath(request);
        return matcher.extractUriTemplateVariables(this.pattern, url);
    }

    public String getPattern() {
        return this.pattern;
    }

    private String getRequestPath(HttpServletRequest request) {
        String url = urlPathHelper.getPathWithinApplication(request);
        if (!this.caseSensitive) {
            url = url.toLowerCase();
        }
        return url;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AntPathRequestMatcher other)) {
            return false;
        }
        return this.pattern.equals(other.pattern)
                && (this.httpMethod == null ? other.httpMethod == null : this.httpMethod.equals(other.httpMethod))
                && this.caseSensitive == other.caseSensitive;
    }

    @Override
    public int hashCode() {
        int result = this.pattern.hashCode();
        result = 31 * result + (this.httpMethod != null ? this.httpMethod.hashCode() : 0);
        result = 31 * result + (this.caseSensitive ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ant [pattern='").append(this.pattern).append("'");
        if (this.httpMethod != null) {
            sb.append(", ").append(this.httpMethod);
        }
        sb.append("]");
        return sb.toString();
    }
}
