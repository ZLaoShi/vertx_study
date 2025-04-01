package org.mxwj.librarymanagement.graphql;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.mxwj.librarymanagement.model.BorrowRecord;
import org.mxwj.librarymanagement.model.BorrowRecordsPage;
import org.mxwj.librarymanagement.service.BorrowService;
import org.mxwj.librarymanagement.utils.ContextHelper;

import graphql.schema.DataFetcher;

public class BorrowFetcher {
    private final BorrowService borrowService;

    public BorrowFetcher(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    // 借书
    public DataFetcher<CompletableFuture<BorrowRecord>> borrowBook() {
        return env -> {
            Map<String, Object> input = env.getArgument("input");
            Long bookId = Long.parseLong((String) input.get("bookId"));
            String remarks = (String) input.get("remarks");
            Long userId = ContextHelper.getAccountId(env);

            return borrowService.borrowBook(userId, bookId, remarks)
                .subscribeAsCompletionStage();
        };
    }

    // 还书
    public DataFetcher<CompletableFuture<BorrowRecord>> returnBook() {
        return env -> {
            Map<String, Object> input = env.getArgument("input");
            Long recordId = Long.parseLong((String) input.get("recordId"));
            String remarks = (String) input.get("remarks");

            return borrowService.returnBook(recordId, remarks)
                .subscribeAsCompletionStage();
        };
    }

    // 查询用户的借阅记录
    public DataFetcher<CompletableFuture<BorrowRecordsPage>> getMyBorrowRecords() {
        return env -> {
            int page = env.getArgumentOrDefault("page", 1);
            int size = env.getArgumentOrDefault("size", 10);
            Long userId = ContextHelper.getAccountId(env);

            return borrowService.findUserBorrowRecords(userId, page, size)
                .subscribeAsCompletionStage();
        };
    }

    // 查询所有借阅记录(管理员)
    public DataFetcher<CompletableFuture<BorrowRecordsPage>> getAllBorrowRecords() {
        return env -> {
            int page = env.getArgumentOrDefault("page", 1);
            int size = env.getArgumentOrDefault("size", 10);

            return borrowService.findAllBorrowRecords(page, size)
                .subscribeAsCompletionStage();
        };
    }
}