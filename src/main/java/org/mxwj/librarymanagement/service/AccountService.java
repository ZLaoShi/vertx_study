package org.mxwj.librarymanagement.service;

import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.mxwj.librarymanagement.lib.DatabaseManager;
import org.mxwj.librarymanagement.model.Account;
import org.mxwj.librarymanagement.utils.JWTUtils;

import io.vertx.core.Future; 
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import java.time.OffsetDateTime;

public class AccountService {
    private final Mutiny.SessionFactory factory;
    private final JWTUtils jwtUtils;
    
    public AccountService(Vertx vertx) {
        this.factory = DatabaseManager.getSessionFactory();
        this.jwtUtils = new JWTUtils(vertx);
    }

    
}
