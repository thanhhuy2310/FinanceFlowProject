package com.financeflow.entity;

import com.financeflow.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="account_id",nullable = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="category_id",nullable = false)
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="import_batch_id")
    private ImportBatch importBatch;
    @Column(name="amount",nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name="transaction_type",nullable = false,length = 20)
    private TransactionType transactionType;
    @Column(name="transaction_date",nullable = false)
    private LocalDateTime transactionDate;
    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;
    @Column(name="description",length = 255)
    private String description;
    @Column(name="reference",length = 100)
    private String reference;
    @Column(name="attachment_url")
    private String attachmentUrl;
}
