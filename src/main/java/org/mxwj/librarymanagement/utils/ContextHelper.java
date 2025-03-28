package org.mxwj.librarymanagement.utils;

import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ContextHelper {

    public static class ContextHelperException extends RuntimeException {
        public ContextHelperException(String message) {
            super(message);
        }
    }

   // 获取用户信息对象
    public static JsonObject getUserPrincipal(DataFetchingEnvironment env) {
        RoutingContext routingContext = env.getGraphQlContext().get(RoutingContext.class);
        return routingContext.get("userPrincipal");
    }

    public static Long getAccountId(DataFetchingEnvironment env) {
        String _acocountId = getUserPrincipal(env).getString("sub");
        return Long.parseLong(_acocountId);
    }

    public static Short getUserRole(DataFetchingEnvironment env) {
        String _role = getUserPrincipal(env).getString("role");
        return Short.parseShort(_role);
    }

}
