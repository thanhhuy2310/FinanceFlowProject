package com.financeflow.dto.response.dashboard;

import com.financeflow.dto.response.transaction.TransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private Long transactionCount;
    private List<CategoryAmountResponse> incomeByCategory;
    private List<CategoryAmountResponse> expenseByCategory;
    private List<TransactionResponse> recentTransactions;
}
