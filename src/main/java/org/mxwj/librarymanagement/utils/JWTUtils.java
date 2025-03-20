package org.mxwj.librarymanagement.utils;

import java.util.Arrays;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

public class JWTUtils {
    private final JWTAuth jwtAuth;
    private final RedisAPI redis;
    private static final int TOKEN_EXPIRES = 3600 * 24; // 24小时过期
    private static final String SECRET = "dhiauwyhdiuahwiduhaiuwd";

    public JWTUtils(Vertx vertx, Redis redisClient) {
        // 配置JWT
        JWTAuthOptions config = new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")      
                .setBuffer(SECRET));     
                

        this.jwtAuth = JWTAuth.create(vertx, config);
        this.redis = RedisAPI.api(redisClient);
    }

    public Future<String> generateToken(String userId) {
        String token = jwtAuth.generateToken(
            new JsonObject()
                .put("sub", userId),
            new JWTOptions()
                .setExpiresInSeconds(TOKEN_EXPIRES)
        );

        // 将token存入Redis，使用userId作为key
        return redis.setex("token:" + userId, String.valueOf(TOKEN_EXPIRES), token)
            .map(res -> token);
    }

    public Future<Boolean> validateToken(String token) {
        TokenCredentials credentials = new TokenCredentials(token);

        return Future.future(promise ->
            jwtAuth.authenticate(credentials)
                .onSuccess(user -> {
                    String userId = user.principal().getString("sub");
                    // 检查Redis中是否存在该token
                    redis.get("token:" + userId)
                        .onSuccess(storedToken -> {
                            if (token.equals(storedToken.toString())) {
                                promise.complete(true);
                            } else {
                                promise.complete(false);
                            }
                        })
                        .onFailure(err -> promise.complete(false));
                })
                .onFailure(err -> promise.complete(false))
        );
    }

    public Future<Void> revokeToken(String userId) {
        return redis.del(Arrays.asList("token:" + userId))
            .map(res -> null);
    }
}
