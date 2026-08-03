export type ApiResponse<T> = { success: boolean; message: string; data: T };

export type User = {
  id?: number;
  fullName: string;
  email: string;
  role?: string;
  createdAt?: string;
};

export type LoginResponse = { token: string; user: User };

export type AccountType = "BANK" | "CASH" | "EWALLET" | "CREDIT_CARD";
export type CategoryType = "INCOME" | "EXPENSE";
export type TransactionType = CategoryType;

export type Provider = { id: number; name: string; logoUrl?: string | null };
export type Account = {
  id: number;
  providerId: number;
  accountName: string;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};
export type AccountPayload = Pick<Account, "accountName" | "accountNumber" | "accountType" | "providerId" | "balance">;

export type Category = {
  id: number;
  name: string;
  type: CategoryType;
  icon?: string | null;
  color?: string | null;
  createdAt: string;
  updatedAt: string;
};
export type CategoryPayload = Pick<Category, "name" | "type" | "icon" | "color">;

export type Transaction = {
  id: number;
  amount: number;
  description?: string | null;
  transactionDate: string;
  transactionType: TransactionType;
  accountId: number;
  accountName: string;
  categoryId: number;
  categoryName: string;
  createdAt: string;
};
export type TransactionPayload = Pick<Transaction, "amount" | "description" | "transactionDate" | "transactionType" | "accountId" | "categoryId">;

export type Rule = {
  id: number;
  keyword: string;
  categoryId: number;
  categoryName: string;
  priority: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};
export type RulePayload = Pick<Rule, "keyword" | "categoryId" | "priority" | "isActive">;

export type CategoryAmount = { categoryName: string; totalAmount: number };
export type Dashboard = {
  totalBalance: number;
  totalIncome: number;
  totalExpense: number;
  transactionCount: number;
  incomeByCategory: CategoryAmount[];
  expenseByCategory: CategoryAmount[];
  recentTransactions: Transaction[];
};

export type ImportBatchStatus = "PENDING" | "COMPLETED" | "FAILED";
export type ImportBatch = {
  id: number;
  fileName: string;
  importedAt: string;
  totalRows: number;
  successRows: number;
  failedRows: number;
  status: ImportBatchStatus;
  errorMessage?: string | null;
};
export type ImportBatchPayload = { fileName: string };
