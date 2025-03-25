package org.mxwj.librarymanagement.graphql;

import java.util.concurrent.CompletableFuture;

import org.mxwj.librarymanagement.model.UserInfo;
import org.mxwj.librarymanagement.service.UserInfoService;

import graphql.schema.DataFetcher;

public class UserInfoFetcher {
    private final UserInfoService userInfoService;

    public UserInfoFetcher(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    public DataFetcher<CompletableFuture<UserInfo>> getUserInfoById() {
        return env -> {
            Long id = Long.parseLong(env.getArgument("id"));
            return userInfoService.findById(id).subscribeAsCompletionStage();
        };
    }
}
