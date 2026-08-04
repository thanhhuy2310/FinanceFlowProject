// API envelope

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// Auth

export interface User {
  id?: number;
  fullName: string;
  email: string;
  role?: string;
  createdAt?: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

// Shared enums

export type AccountType = "BANK" | "CASH" | "EWALLET" | "CREDIT_CARD";
export type CategoryType = "INCOME" | "EXPENSE";
export type TransactionType = CategoryType;
export type ImportBatchStatus = "PENDING" | "COMPLETED" | "FAILED";

// Providers

export interface Provider {
  id: number;
  name: string;
  logoUrl?: string | null;
}

// Accounts

export interface Account {
  id: number;
  providerId: number;
  accountName: string;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type AccountPayload = Pick<
  Account,
  "accountName" | "accountNumber" | "accountType" | "providerId" | "balance"
>;

// Categories

export interface Category {
  id: number;
  name: string;
  type: CategoryType;
  icon?: string | null;
  color?: string | null;
  createdAt: string;
  updatedAt: string;
}

export type CategoryPayload = Pick<Category, "name" | "type" | "icon" | "color">;

// Transactions

export interface Transaction {
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
}

export type TransactionPayload = Pick<
  Transaction,
  "amount" | "description" | "transactionDate" | "transactionType" | "accountId" | "categoryId"
>;

// Rules

export interface Rule {
  id: number;
  keyword: string;
  categoryId: number;
  categoryName: string;
  priority: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type RulePayload = Pick<Rule, "keyword" | "categoryId" | "priority" | "isActive">;

// Dashboard

export interface CategoryAmount {
  categoryName: string;
  totalAmount: number;
}

export interface Dashboard {
  totalBalance: number;
  totalIncome: number;
  totalExpense: number;
  transactionCount: number;
  incomeByCategory: CategoryAmount[];
  expenseByCategory: CategoryAmount[];
  recentTransactions: Transaction[];
}

// Import batches

export interface ImportRowFailure {
  id: number;
  rowNumber: number;
  errorMessage: string;
  /** Description found in the CSV row, when available. */
  description?: string | null;
  /** Category name found in the CSV row, when available. */
  categoryName?: string | null;
  createdAt: string;
}

export interface ImportBatch {
  id: number;
  fileName: string;
  importedAt: string;
  totalRows: number;
  successRows: number;
  failedRows: number;
  skippedRows: number;
  /** Import duration in milliseconds, populated right after an upload. */
  executionTimeMs?: number | null;
  status: ImportBatchStatus;
  errorMessage?: string | null;
  failures: ImportRowFailure[];
}

export interface ImportBatchPayload {
  fileName: string;
}

// Rule preview

export interface RulePreview {
  matched: boolean;
  ruleId?: number | null;
  keyword?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
}
