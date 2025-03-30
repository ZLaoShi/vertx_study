package org.mxwj.librarymanagement.service;

import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.mxwj.librarymanagement.lib.DatabaseManager;
import org.mxwj.librarymanagement.model.Account;
import org.mxwj.librarymanagement.model.dto.LoginDTO;
import org.mxwj.librarymanagement.model.dto.RegisterDTO;
import org.mxwj.librarymanagement.model.vo.LoginVO;
import org.mxwj.librarymanagement.utils.JWTUtils;

import io.vertx.core.Future; 
import io.vertx.core.Vertx;
import jakarta.persistence.NoResultException;

import org.mindrot.jbcrypt.BCrypt;
import java.time.OffsetDateTime;

public class AccountService {
    private final Mutiny.SessionFactory factory;
    private final JWTUtils jwtUtils;
    
    public AccountService(Vertx vertx) {
        factory = DatabaseManager.getSessionFactory();
        this.jwtUtils = new JWTUtils(vertx);
    }

    public Uni<LoginVO> login(LoginDTO loginDTO) {
        return factory.withSession(session -> {
            // 先获取查询结果
            return session.createQuery("FROM Account WHERE username = :username", Account.class)
                .setParameter("username", loginDTO.getUsername())
                .getSingleResultOrNull()
                .onItem().ifNull().failWith(() -> 
                    new NoResultException("用户不存在"))
                .onItem().transformToUni(account -> {
                    // 密码验证
                    if (!BCrypt.checkpw(loginDTO.getPassword(), account.getPassword())) {
                        return Uni.createFrom().failure(
                            new NoResultException("密码错误"));
                    }
                    
                    // 更新最后登录时间
                    account.setLastLogin(OffsetDateTime.now());

                    // 合并更新并生成 token
                    return session.merge(account)
                        .call(session::flush)
                        .chain(() -> {
                            Future<String> tokenFuture = jwtUtils.generateToken(account.getId().toString(), account.getUserType().toString());
                            // 将 Vert.x Future 转换为 Mutiny Uni
                            return Uni.createFrom().completionStage(tokenFuture.toCompletionStage());
                        })
                        .map(token -> LoginVO.builder()
                            .token(token)
                            .username(account.getUsername())
                            .build());
                });
        });
    }

    public Uni<Account> register(RegisterDTO registerDTO) {
        return factory.withSession(session -> {
            return session.createQuery("FROM Account WHERE username = :username", Account.class)
                .setParameter("username", registerDTO.getUsername())
                .getSingleResultOrNull()
                .onItem().ifNotNull().failWith(() -> 
                    new IllegalArgumentException("用户名已存在"))
                .onItem().transformToUni(v -> {
                    // 创建新账户
                    Account account = new Account();
                    account.setUsername(registerDTO.getUsername());
                    account.setPassword(BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt()));
                    account.setEmail(registerDTO.getEmail());
                    account.setCreatedAt(OffsetDateTime.now());
                    account.setStatus((short) 1);
                    
                    return session.persist(account)
                        .call(session::flush)
                        .replaceWith(account);
                });
        });
    }

    public Uni<Void> logout(String token) {
        return Uni.createFrom().completionStage(
            jwtUtils.revokeToken(token).toCompletionStage()
        ).onFailure().invoke(error -> {
            System.err.println("注销失败: " + error.getMessage());
            error.printStackTrace();
        });
    }

}
