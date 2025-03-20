package org.mxwj.librarymanagement.graphql;

import graphql.schema.DataFetcher;
import io.vertx.core.json.JsonObject;

import org.mxwj.librarymanagement.model.Account;
import org.mxwj.librarymanagement.service.AccountService;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthFetcher {
    private final AccountService accountService;

    public AuthFetcher(AccountService accountService) {
        this.accountService = accountService;
    }

    // public DataFetcher<CompletableFuture<JsonObject>> login() {
    //     return env -> {
    //         String username = env.getArgument("username");
    //         String password = env.getArgument("password");
    //         return accountService.login(username, password)
    //             .subscribeAsCompletionStage();
    //     };
    // }

    // public DataFetcher<CompletableFuture<JsonObject>> register() {
    //     return env -> {
    //         Map<String, Object> input = env.getArgument("input");
    //         String username = (String) input.get("username");
    //         String password = (String) input.get("password");
    //         String email = (String) input.get("email");
    //         return accountService.register(username, password, email)
    //             .subscribeAsCompletionStage();
    //     };
    // }
}