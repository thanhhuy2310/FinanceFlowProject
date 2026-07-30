package com.financeflow.entity;
import com.financeflow.enums.AccountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
@Table(name="accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="provider_id",nullable = false)
    private Provider provider;
    @Column(name="account_name",nullable = false,length = 100)
    private String accountName;
    @Column(name="account_number",nullable = false,unique = true,length = 50)
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    @Column(name="account_type",nullable = false,length = 20)
    private AccountType accountType;
    @Column(name="balance",nullable = false)
    private BigDecimal balance;
    @Column(name="is_active",nullable = false)
    private Boolean isActive;
    @Column(name="created_at",nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at",nullable = false)
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;

}
