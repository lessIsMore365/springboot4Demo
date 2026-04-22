package org.example.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis Token Store 配置
 * 将OAuth2授权信息存储到Redis中
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisTokenStoreConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;

    /**
     * 创建RedisTemplate用于存储OAuth2授权信息
     */
    @Bean
    public RedisTemplate<String, OAuth2Authorization> authorizationRedisTemplate() {
        Thread currentThread = Thread.currentThread();
        log.info("创建OAuth2授权RedisTemplate - 当前线程: {}, 是否虚拟线程: {}",
                currentThread, currentThread.isVirtual());

        RedisTemplate<String, OAuth2Authorization> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置key序列化器
        template.setKeySerializer(new StringRedisSerializer());

        // 设置value序列化器（使用Jackson）
        Jackson2JsonRedisSerializer<OAuth2Authorization> serializer =
                new Jackson2JsonRedisSerializer<>(OAuth2Authorization.class);

        // 配置ObjectMapper以支持OAuth2Authorization序列化
        ObjectMapper mapper = objectMapper.copy();
        mapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        mapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        serializer.setObjectMapper(mapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        log.info("OAuth2授权RedisTemplate创建完成");
        return template;
    }

    /**
     * 创建RedisOAuth2AuthorizationService
     */
    @Bean
    public OAuth2AuthorizationService oAuth2AuthorizationService(
            RedisTemplate<String, OAuth2Authorization> authorizationRedisTemplate,
            StringRedisTemplate stringRedisTemplate) {
        Thread currentThread = Thread.currentThread();
        log.info("创建RedisOAuth2AuthorizationService - 当前线程: {}, 是否虚拟线程: {}",
                currentThread, currentThread.isVirtual());

        return new RedisOAuth2AuthorizationService(authorizationRedisTemplate, stringRedisTemplate);
    }

    /**
     * Redis OAuth2授权服务实现
     */
    public static class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

        private static final String OAUTH2_AUTHORIZATION_KEY_PREFIX = "oauth2:authorization:";
        private static final String OAUTH2_AUTHORIZATION_CODE_KEY_PREFIX = "oauth2:authorization_code:";
        private static final String OAUTH2_ACCESS_TOKEN_KEY_PREFIX = "oauth2:access_token:";
        private static final String OAUTH2_REFRESH_TOKEN_KEY_PREFIX = "oauth2:refresh_token:";
        private static final String OAUTH2_ID_TOKEN_KEY_PREFIX = "oauth2:id_token:";

        private final RedisTemplate<String, OAuth2Authorization> redisTemplate;
        private final StringRedisTemplate stringRedisTemplate;

        public RedisOAuth2AuthorizationService(
                RedisTemplate<String, OAuth2Authorization> redisTemplate,
                StringRedisTemplate stringRedisTemplate) {
            this.redisTemplate = redisTemplate;
            this.stringRedisTemplate = stringRedisTemplate;
        }

        @Override
        public void save(OAuth2Authorization authorization) {
            Thread currentThread = Thread.currentThread();
            log.info("保存OAuth2授权信息到Redis - 授权ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                    authorization.getId(), currentThread, currentThread.isVirtual());

            String authorizationKey = OAUTH2_AUTHORIZATION_KEY_PREFIX + authorization.getId();
            redisTemplate.opsForValue().set(authorizationKey, authorization);

            // 设置过期时间（基于access token过期时间）
            OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
            if (accessToken != null) {
                Instant expiresAt = accessToken.getToken().getExpiresAt();
                if (expiresAt != null) {
                    long ttl = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
                    if (ttl > 0) {
                        redisTemplate.expire(authorizationKey, ttl, TimeUnit.SECONDS);
                    }
                }
            }

            // 保存索引以便通过token查找
            saveAuthorizationByToken(authorization);
        }

        @Override
        public OAuth2Authorization findById(String id) {
            String key = OAUTH2_AUTHORIZATION_KEY_PREFIX + id;
            OAuth2Authorization authorization = redisTemplate.opsForValue().get(key);

            if (authorization != null) {
                Thread currentThread = Thread.currentThread();
                log.info("从Redis查找OAuth2授权信息 - 授权ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                        id, currentThread, currentThread.isVirtual());
            }

            return authorization;
        }

        @Override
        public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
            String tokenKey = getTokenKey(token, tokenType);
            String authorizationId = stringRedisTemplate.opsForValue().get(tokenKey);

            if (authorizationId == null) {
                return null;
            }

            return findById(authorizationId);
        }

        @Override
        public void remove(OAuth2Authorization authorization) {
            Thread currentThread = Thread.currentThread();
            log.info("从Redis删除OAuth2授权信息 - 授权ID: {}, 当前线程: {}, 是否虚拟线程: {}",
                    authorization.getId(), currentThread, currentThread.isVirtual());

            String authorizationKey = OAUTH2_AUTHORIZATION_KEY_PREFIX + authorization.getId();
            redisTemplate.delete(authorizationKey);

            // 删除token索引
            removeAuthorizationByToken(authorization);
        }

        /**
         * 保存token索引
         */
        private void saveAuthorizationByToken(OAuth2Authorization authorization) {
            // 保存授权码索引
            OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                    authorization.getToken(OAuth2AuthorizationCode.class);
            if (authorizationCode != null) {
                String codeKey = OAUTH2_AUTHORIZATION_CODE_KEY_PREFIX + authorizationCode.getToken().getTokenValue();
                stringRedisTemplate.opsForValue().set(codeKey, authorization.getId());

                // 设置过期时间（授权码通常很短）
                stringRedisTemplate.expire(codeKey, 5, TimeUnit.MINUTES);
            }

            // 保存access token索引
            OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
            if (accessToken != null) {
                String accessTokenKey = OAUTH2_ACCESS_TOKEN_KEY_PREFIX + accessToken.getToken().getTokenValue();
                stringRedisTemplate.opsForValue().set(accessTokenKey, authorization.getId());

                // 设置过期时间
                Instant expiresAt = accessToken.getToken().getExpiresAt();
                if (expiresAt != null) {
                    long ttl = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
                    if (ttl > 0) {
                        stringRedisTemplate.expire(accessTokenKey, ttl, TimeUnit.SECONDS);
                    }
                }
            }

            // 保存refresh token索引
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
            if (refreshToken != null) {
                String refreshTokenKey = OAUTH2_REFRESH_TOKEN_KEY_PREFIX + refreshToken.getToken().getTokenValue();
                stringRedisTemplate.opsForValue().set(refreshTokenKey, authorization.getId());

                // refresh token过期时间较长
                Instant expiresAt = refreshToken.getToken().getExpiresAt();
                if (expiresAt != null) {
                    long ttl = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
                    if (ttl > 0) {
                        stringRedisTemplate.expire(refreshTokenKey, ttl, TimeUnit.SECONDS);
                    }
                }
            }

            // 保存ID token索引（OpenID Connect）
            OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(OidcIdToken.class);
            if (idToken != null) {
                String idTokenKey = OAUTH2_ID_TOKEN_KEY_PREFIX + idToken.getToken().getTokenValue();
                stringRedisTemplate.opsForValue().set(idTokenKey, authorization.getId());
            }
        }

        /**
         * 删除token索引
         */
        private void removeAuthorizationByToken(OAuth2Authorization authorization) {
            // 删除授权码索引
            OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                    authorization.getToken(OAuth2AuthorizationCode.class);
            if (authorizationCode != null) {
                String codeKey = OAUTH2_AUTHORIZATION_CODE_KEY_PREFIX + authorizationCode.getToken().getTokenValue();
                stringRedisTemplate.delete(codeKey);
            }

            // 删除access token索引
            OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
            if (accessToken != null) {
                String accessTokenKey = OAUTH2_ACCESS_TOKEN_KEY_PREFIX + accessToken.getToken().getTokenValue();
                stringRedisTemplate.delete(accessTokenKey);
            }

            // 删除refresh token索引
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
            if (refreshToken != null) {
                String refreshTokenKey = OAUTH2_REFRESH_TOKEN_KEY_PREFIX + refreshToken.getToken().getTokenValue();
                stringRedisTemplate.delete(refreshTokenKey);
            }

            // 删除ID token索引
            OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(OidcIdToken.class);
            if (idToken != null) {
                String idTokenKey = OAUTH2_ID_TOKEN_KEY_PREFIX + idToken.getToken().getTokenValue();
                stringRedisTemplate.delete(idTokenKey);
            }
        }

        /**
         * 根据token类型获取对应的Redis key前缀
         */
        private String getTokenKey(String token, OAuth2TokenType tokenType) {
            if (tokenType == null) {
                return OAUTH2_ACCESS_TOKEN_KEY_PREFIX + token;
            }

            return switch (tokenType.getValue()) {
                case OAuth2ParameterNames.CODE -> OAUTH2_AUTHORIZATION_CODE_KEY_PREFIX + token;
                case OAuth2ParameterNames.ACCESS_TOKEN -> OAUTH2_ACCESS_TOKEN_KEY_PREFIX + token;
                case OAuth2ParameterNames.REFRESH_TOKEN -> OAUTH2_REFRESH_TOKEN_KEY_PREFIX + token;
                case "id_token" -> OAUTH2_ID_TOKEN_KEY_PREFIX + token;
                default -> OAUTH2_ACCESS_TOKEN_KEY_PREFIX + token;
            };
        }
    }
}