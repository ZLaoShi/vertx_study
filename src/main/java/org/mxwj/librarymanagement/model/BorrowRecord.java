package org.mxwj.librarymanagement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "borrow_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关系：多条 BorrowRecord 对应一个 UserInfo
    @ManyToOne 
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo userInfo; // 引用您提供的 UserInfo 实体

    // 关系：多条 BorrowRecord 对应一个 Book
    @ManyToOne 
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "borrow_date", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime borrowDate;

    @Column(name = "due_date", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dueDate;

    @Column(name = "return_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime returnDate;

    @Column(name = "status", columnDefinition = "SMALLINT DEFAULT 0")
    private Short status = 0; // 直接初始化以匹配 DEFAULT

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;
}