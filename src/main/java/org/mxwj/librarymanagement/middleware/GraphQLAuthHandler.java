package org.mxwj.librarymanagement.middleware;

import graphql.schema.DataFetcher;
import io.vertx.ext.auth.impl.UserImpl;

public class GraphQLAuthHandler {
    
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    // 修改为基于角色的验证
    public static DataFetcher<?> requireRole(DataFetcher<?> fetcher, int requiredRole) {
        return environment -> {
            // 从 context 中获取 user
            UserImpl user = environment.getGraphQlContext().get("user");
            if (user == null) {
                throw new AuthenticationException("未登录");
            }

            // 直接检查角色值
            int userRole = Integer.parseInt(user.principal().getString("role"));
            if (userRole < requiredRole) {
                throw new AuthenticationException("无权限执行此操作");
            }

            // 执行原始 fetcher
            return fetcher.get(environment);
        };
    }
}