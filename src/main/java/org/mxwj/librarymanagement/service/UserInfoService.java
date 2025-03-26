package org.mxwj.librarymanagement.service;

import org.hibernate.reactive.mutiny.Mutiny;
import org.mxwj.librarymanagement.lib.DatabaseManager;
import org.mxwj.librarymanagement.model.UserInfo;

import io.smallrye.mutiny.Uni;

public class UserInfoService {
    private final Mutiny.SessionFactory factory;

    public UserInfoService() {
        factory = DatabaseManager.getSessionFactory();
    }

    public Uni<UserInfo> findById(Long id) {
        return factory.withSession(session ->
            session.find(UserInfo.class, id)
        ).onFailure().invoke(error -> {
               System.out.println("数据库查询错误: " + error.getMessage());
               error.printStackTrace();
        });
    }
}

