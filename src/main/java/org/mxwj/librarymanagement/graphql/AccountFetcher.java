package org.mxwj.librarymanagement.graphql;

import org.mxwj.librarymanagement.service.AccountService;

public class AccountFetcher {
    private final AccountService accountService;

    public AccountFetcher(AccountService accountService) {
        this.accountService = accountService;
    }
    
}
