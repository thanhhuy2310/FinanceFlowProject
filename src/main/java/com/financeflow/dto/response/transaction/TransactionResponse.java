package com.financeflow.dto.response.transaction;

import com.financeflow.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private TransactionType transactionType;
    private Long accountId;
    private String accountName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
}
