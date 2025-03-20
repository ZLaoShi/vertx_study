package org.mxwj.librarymanagement.graphql;

import java.util.concurrent.CompletableFuture;

import org.mxwj.librarymanagement.model.User;
import org.mxwj.librarymanagement.service.UserService;
import org.mxwj.librarymanagement.utils.JWTUtils;

import graphql.schema.DataFetcher;
import io.vertx.core.json.JsonObject;

public class AuthFetcher {
    private final UserService userService;
    private final JWTUtils jwtUtils;

    public AuthFetcher(UserService userService, JWTUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    // public DataFetcher<CompletableFuture<JsonObject>> login() {
    //     return env -> {
    //         String email = env.getArgument("email");
    //         String password = env.getArgument("password");
            
    //         return userService.authenticate(email, password)
    //             .compose(user -> {
    //                 if (user != null) {
    //                     return JWTUtils.generateToken(user.getId().toString())
    //                         .map(token -> new JsonObject()
    //                             .put("token", token)
    //                             .put("user", JsonObject.mapFrom(user)));
    //                 }
    //                 throw new RuntimeException("Invalid credentials");
    //             })
    //             .subscribeAsCompletionStage();
    //     };
    // }

    // public DataFetcher<CompletableFuture<Boolean>> logout() {
    //     return env -> {
    //         String userId = env.getArgument("userId");
    //         return JWTUtils.revokeToken(userId)
    //             .map(v -> true)
    //             .subscribeAsCompletionStage();
    //     };
    // }
}