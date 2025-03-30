package org.mxwj.librarymanagement.service;

import java.time.OffsetDateTime;

import org.hibernate.reactive.mutiny.Mutiny;
import org.mxwj.librarymanagement.lib.DatabaseManager;
import org.mxwj.librarymanagement.model.Account;
import org.mxwj.librarymanagement.model.UserInfo;
import org.mxwj.librarymanagement.model.dto.CreateUserInfoDTO;
import org.mxwj.librarymanagement.model.dto.UpdateUserInfoDTO;

import io.smallrye.mutiny.Uni;
import jakarta.persistence.NoResultException;

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

    public Uni<UserInfo> createUserInfo(CreateUserInfoDTO crateUserInfoDto) {
        return factory.withSession(session -> {
            return session.find(Account.class, crateUserInfoDto.getAccountId())
                    .onItem().ifNull().failWith(() -> new NoResultException("Account not found with id: " + crateUserInfoDto.getAccountId()))
                    .flatMap(account -> {
                        UserInfo userInfo = new UserInfo();

                        userInfo.setAccount(account);
                        userInfo.setFullName(crateUserInfoDto.getFullName());
                        userInfo.setPhone(crateUserInfoDto.getPhone());
                        userInfo.setAddress(crateUserInfoDto.getAddress());
                        if (crateUserInfoDto.getMaxBorrowBooks() != null) {
                            userInfo.setMaxBorrowBooks(crateUserInfoDto.getMaxBorrowBooks());
                        }
                        userInfo.setCreatedAt(OffsetDateTime.now());
                        userInfo.setUpdatedAt(OffsetDateTime.now());

                        return session.persist(userInfo)
                                .call(session::flush)
                                .replaceWith(userInfo);
                    });
        })
        .onFailure().invoke(error -> {
            System.err.println("创建用户信息失败");
            error.printStackTrace();
        });
    }

    public Uni<UserInfo> updateUserInfo(UpdateUserInfoDTO updateUserInfoDto) {
        return factory.withSession(session -> {
            return    session.find(UserInfo.class, updateUserInfoDto.getAccountId())
                        .onItem().ifNull().failWith(() -> new NoResultException("更新 UserInfo 失败：未找到 ID 为 " + updateUserInfoDto.getAccountId() + " 的 UserInfo"))
                        .flatMap(userInfo -> {
                            boolean updated = false;
                            if (updateUserInfoDto.getFullName() != null) {
                                userInfo.setFullName(updateUserInfoDto.getFullName());
                                updated = true;
                            }
                            if (updateUserInfoDto.getPhone() != null) {
                                userInfo.setPhone(updateUserInfoDto.getPhone());
                                updated = true;
                            }
                            if (updateUserInfoDto.getAddress() != null) {
                                userInfo.setAddress(updateUserInfoDto.getAddress());
                                updated = true;
                            }
                            if (updated) {
                                userInfo.setUpdatedAt(OffsetDateTime.now());
                            }

                            return session.flush()
                                    .replaceWith(userInfo);
                        });
                    })
            .onFailure().invoke(error -> {
                System.out.println("更新用户信息失败");
                error.printStackTrace();
            });
    }

    public Uni<Void> deleteUserInfo(Long userInfoId) {
        return factory.withSession(session -> {
            return session.find(UserInfo.class, userInfoId)
                .onItem().ifNull().failWith(() -> new NoResultException("删除 UserInfo 失败：未找到 ID 为 " + userInfoId + " 的 UserInfo"))
                .flatMap(userInfo -> {
                    return session.remove(userInfoId)
                            .call(session::flush);  
                });
        }).
        onFailure().invoke(error -> {
            System.out.println("删除用户信息失败");
            error.printStackTrace();
        });
    }

}

