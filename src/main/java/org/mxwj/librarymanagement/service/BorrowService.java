package org.mxwj.librarymanagement.service;

import java.time.OffsetDateTime;
import org.hibernate.reactive.mutiny.Mutiny;
import org.mxwj.librarymanagement.lib.DatabaseManager;
import org.mxwj.librarymanagement.model.Book;
import org.mxwj.librarymanagement.model.BorrowRecord;
import org.mxwj.librarymanagement.model.BorrowRecordsPage;
import org.mxwj.librarymanagement.model.PageInfo;
import org.mxwj.librarymanagement.model.UserInfo;
import io.smallrye.mutiny.Uni;

public class BorrowService {
    private final Mutiny.SessionFactory factory;
    private static final int DEFAULT_BORROW_DAYS = 30; // 默认借阅期限30天

    public BorrowService() {
        factory = DatabaseManager.getSessionFactory();
    }

    // 借书
    public Uni<BorrowRecord> borrowBook(Long userId, Long bookId, String remarks) {
        return factory.withTransaction((session, tx) -> 
            // 1. 检查用户信息
            session.find(UserInfo.class, userId)
                .onItem().ifNull().failWith(() -> 
                    new IllegalArgumentException("用户信息不存在"))
                .flatMap(userInfo -> {
                    // 2. 检查用户借阅权限
                    return session.createQuery(
                        "SELECT COUNT(br) FROM BorrowRecord br " +
                        "WHERE br.userInfo = :userInfo AND br.status = 0", Long.class)
                        .setParameter("userInfo", userInfo)
                        .getSingleResult()
                        .flatMap(currentBorrowCount -> {
                            if (currentBorrowCount >= userInfo.getMaxBorrowBooks()) {
                                return Uni.createFrom().failure(
                                    new IllegalStateException("超出最大借阅数量限制")
                                );
                            }
                            
                            // 3. 检查图书是否可借
                            return session.find(Book.class, bookId)
                                .onItem().ifNull().failWith(() -> 
                                    new IllegalArgumentException("图书不存在"))
                                .flatMap(book -> {
                                    if (book.getAvailableCopies() <= 0) {
                                        return Uni.createFrom().failure(
                                            new IllegalStateException("图书已全部借出")
                                        );
                                    }

                                    // 4. 创建借阅记录
                                    BorrowRecord record = new BorrowRecord();
                                    record.setUserInfo(userInfo);
                                    record.setBook(book);
                                    record.setBorrowDate(OffsetDateTime.now());
                                    record.setDueDate(OffsetDateTime.now().plusDays(DEFAULT_BORROW_DAYS));
                                    record.setStatus((short) 0);
                                    record.setRemarks(remarks);
                                    record.setCreatedAt(OffsetDateTime.now());
                                    record.setUpdatedAt(OffsetDateTime.now());

                                    // 5. 更新图书可用数量
                                    book.setAvailableCopies(book.getAvailableCopies() - 1);

                                    return session.persist(record)
                                        .chain(() -> session.flush())
                                        .replaceWith(record);
                                });
                        });
                })
        ).onFailure().invoke(error -> {
            System.err.println("借书失败: " + error.getMessage());
            error.printStackTrace();
        });
    }

    // 还书
    public Uni<BorrowRecord> returnBook(Long recordId, String remarks) {
        return factory.withTransaction((session, tx) ->
            session.find(BorrowRecord.class, recordId)
                .onItem().ifNull().failWith(() -> 
                    new IllegalArgumentException("借阅记录不存在"))
                .flatMap(record -> {
                    if (record.getStatus() != 0) {
                        return Uni.createFrom().failure(
                            new IllegalStateException("该记录已完成还书")
                        );
                    }

                    // 更新借阅记录
                    record.setReturnDate(OffsetDateTime.now());
                    record.setStatus((short) 1);
                    if (remarks != null) {
                        record.setRemarks(remarks);
                    }
                    record.setUpdatedAt(OffsetDateTime.now());

                    // 更新图书可用数量
                    Book book = record.getBook();
                    book.setAvailableCopies(book.getAvailableCopies() + 1);

                    return session.flush()
                        .replaceWith(record);
                })
        ).onFailure().invoke(error -> {
            System.err.println("还书失败: " + error.getMessage());
            error.printStackTrace();
        });
    }

    // 查询用户的借阅记录
    public Uni<BorrowRecordsPage> findUserBorrowRecords(Long userId, int page, int size) {
        return factory.withSession(session -> 
            session.find(UserInfo.class, userId)
                .onItem().ifNull().failWith(() -> 
                    new IllegalArgumentException("用户不存在"))
                .flatMap(userInfo -> {
                    String countQuery = "SELECT COUNT(br) FROM BorrowRecord br WHERE br.userInfo = :userInfo";
                    String listQuery = "FROM BorrowRecord br WHERE br.userInfo = :userInfo ORDER BY br.createdAt DESC";

                    return session.createQuery(countQuery, Long.class)
                        .setParameter("userInfo", userInfo)
                        .getSingleResult()
                        .flatMap(total -> 
                            session.createQuery(listQuery, BorrowRecord.class)
                                .setParameter("userInfo", userInfo)
                                .setFirstResult((page - 1) * size)
                                .setMaxResults(size)
                                .getResultList()
                                .map(records -> {
                                    int totalPages = (int) Math.ceil((double) total / size);
                                    PageInfo pageInfo = PageInfo.builder()
                                        .currentPage(page)
                                        .pageSize(size)
                                        .totalPages(totalPages)
                                        .totalElements(total.intValue())
                                        .hasNext(page < totalPages)
                                        .build();

                                    return BorrowRecordsPage.builder()
                                        .content(records)
                                        .pageInfo(pageInfo)
                                        .build();
                                })
                        );
                })
        ).onFailure().invoke(error -> {
            System.err.println("查询借阅记录失败: " + error.getMessage());
            error.printStackTrace();
        });
    }

    // 查询所有借阅记录(管理员)
    public Uni<BorrowRecordsPage> findAllBorrowRecords(int page, int size) {
        return factory.withSession(session -> {
            String countQuery = "SELECT COUNT(br) FROM BorrowRecord br";
            String listQuery = "FROM BorrowRecord br ORDER BY br.createdAt DESC";

            return session.createQuery(countQuery, Long.class)
                .getSingleResult()
                .flatMap(total -> 
                    session.createQuery(listQuery, BorrowRecord.class)
                        .setFirstResult((page - 1) * size)
                        .setMaxResults(size)
                        .getResultList()
                        .map(records -> {
                            int totalPages = (int) Math.ceil((double) total / size);
                            PageInfo pageInfo = PageInfo.builder()
                                .currentPage(page)
                                .pageSize(size)
                                .totalPages(totalPages)
                                .totalElements(total.intValue())
                                .hasNext(page < totalPages)
                                .build();

                            return BorrowRecordsPage.builder()
                                .content(records)
                                .pageInfo(pageInfo)
                                .build();
                        })
                );
        }).onFailure().invoke(error -> {
            System.err.println("查询所有借阅记录失败: " + error.getMessage());
            error.printStackTrace();
        });
    }
}