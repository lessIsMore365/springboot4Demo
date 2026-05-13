package org.example.security.authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

public class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

    private static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!PASSWORD.getValue().equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null) {
            return null;
        }

        MultiValueMap<String, String> parameters = parseParameters(request);

        String username = parameters.getFirst("username");
        if (!StringUtils.hasText(username)) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "username is required", ""));
        }

        String password = parameters.getFirst("password");
        if (!StringUtils.hasText(password)) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "password is required", ""));
        }

        Set<String> scopes = Collections.emptySet();
        String scopeParam = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scopeParam)) {
            scopes = new HashSet<>(Arrays.asList(scopeParam.split("\\s+")));
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, values) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals("username") &&
                    !key.equals("password") &&
                    !key.equals(OAuth2ParameterNames.SCOPE)) {
                additionalParameters.put(key, values.getFirst());
            }
        });

        return new OAuth2PasswordAuthenticationToken(username, password, scopes, clientPrincipal, additionalParameters);
    }

    private static MultiValueMap<String, String> parseParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new org.springframework.util.LinkedMultiValueMap<>();
        parameterMap.forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }
}
