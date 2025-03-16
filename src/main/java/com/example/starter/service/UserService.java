package com.example.starter.service;

import com.example.starter.model.User;
// import org.hibernate.reactive.mutiny.Mutiny;
// import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;

import io.smallrye.mutiny.Uni;

import static jakarta.persistence.Persistence.createEntityManagerFactory;
import static org.hibernate.reactive.mutiny.Mutiny.SessionFactory;

public class UserService {
    SessionFactory factory =
				createEntityManagerFactory( "pg-vertx-study")
						.unwrap(SessionFactory.class);

    public Uni<User> findById(Integer id) {
        return factory.withSession(session -> 
        session.find(User.class, id)
        ).onFailure().invoke(error -> {
            System.err.println("数据库查询错误: " + error.getMessage());
            error.printStackTrace();
        });
    }
}