package com.example.starter.service;

import com.example.starter.model.User;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import java.util.List;
import static jakarta.persistence.Persistence.createEntityManagerFactory;

public class UserService {
    private final Mutiny.SessionFactory factory;

    public UserService() {
        factory = createEntityManagerFactory("pg-vertx-study")
                .unwrap(Mutiny.SessionFactory.class);
    }

    public Uni<User> findById(Integer id) {
        return factory.withSession(session -> 
            session.find(User.class, id)
        ).onFailure().invoke(error -> {
            System.err.println("数据库查询错误: " + error.getMessage());
            error.printStackTrace();
        });
    }

    public Uni<List<User>> findAll() {
        return factory.withSession(session -> 
            session.createQuery("FROM User", User.class).getResultList()
        ).onFailure().invoke(error -> {
            System.err.println("查询所有用户失败: " + error.getMessage());
            error.printStackTrace();
        });
    }

    public Uni<User> createUser(User user) {
        return factory.withSession(session -> 
            session.persist(user)
                .call(session::flush)  // 使用 call 替代 chain
                .replaceWith(user)     // 使用 replaceWith 替代 map
        );
    }

    public Uni<User> updateUser(Integer id, User updatedUser) {
        return factory.withSession(session -> 
            session.find(User.class, id)
                .onItem().ifNotNull().transform(existingUser -> {
                    existingUser.setName(updatedUser.getName());
                    existingUser.setEmail(updatedUser.getEmail());
                    return existingUser;
                })
                .call(() -> session.flush())
        );
    }

    public Uni<Boolean> deleteUser(Integer id) {
        return factory.withSession(session -> 
            session.find(User.class, id)
                .onItem().ifNotNull().transformToUni(user -> 
                    session.remove(user)
                        .call(session::flush)
                        .replaceWith(true)
                )
                .onItem().ifNull().continueWith(false)
        );
    }
}