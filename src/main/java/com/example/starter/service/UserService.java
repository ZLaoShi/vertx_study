package com.example.starter.service;

import com.example.starter.model.User;
import org.hibernate.reactive.mutiny.Mutiny;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class UserService {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("pg-vertx-study");

    public Uni<User> findById(Integer id) {
        Mutiny.SessionFactory sessionFactory = emf.unwrap(Mutiny.SessionFactory.class);

        return sessionFactory.withSession(session -> 
        session.find(User.class, id)
        ).onFailure().invoke(error -> {
            System.err.println("数据库查询错误: " + error.getMessage());
            error.printStackTrace();
        });
    }
}