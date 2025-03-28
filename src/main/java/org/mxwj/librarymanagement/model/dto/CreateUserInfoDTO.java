package org.mxwj.librarymanagement.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserInfoDTO {
    private Long id;
    private Long accountId;
    private String fullName;
    private String phone;
    private String address;
    private Integer maxBorrowBooks;
}
