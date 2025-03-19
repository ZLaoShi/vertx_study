package com.example.starter.lib;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.*;

public class RedisClient {
    private static RedisClient instance;
    private final Redis redis;
    private RedisConnection connection;

    // 私有构造函数
    private RedisClient(Vertx vertx) {
        this.redis = Redis.createClient(
            vertx,
            new RedisOptions()
                .setConnectionString("redis://localhost:6379")
                .setMaxPoolSize(8)
        );
    }

    // 单例获取方法
    public static synchronized RedisClient getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new RedisClient(vertx);
        }
        return instance;
    }

    // 获取Redis连接
    public Future<RedisConnection> getRedis() {
        if (connection != null) {
            return Future.succeededFuture(connection);
        }

        return redis.connect()
            .onSuccess(conn -> {
                this.connection = conn;
                
                // 设置连接关闭或错误的处理
                conn.exceptionHandler(err -> {
                    System.err.println("Redis connection error: " + err.getMessage());
                    this.connection = null;
                });
            });
    }
}