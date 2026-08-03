import { api, unwrap } from "./client";
import type { Account, AccountPayload, Category, CategoryPayload, Dashboard, ImportBatch, ImportBatchPayload, Provider, Rule, RulePayload, Transaction, TransactionPayload } from "../types/api";

export const financeApi = {
  dashboard: () => unwrap<Dashboard>(api.get("/api/dashboard")),
  providers: () => unwrap<Provider[]>(api.get("/api/providers")),
  accounts: () => unwrap<Account[]>(api.get("/api/accounts")),
  createAccount: (payload: AccountPayload) => unwrap<Account>(api.post("/api/accounts", payload)),
  updateAccount: (id: number, payload: AccountPayload) => unwrap<Account>(api.put(`/api/accounts/${id}`, payload)),
  deleteAccount: (id: number) => unwrap<void>(api.delete(`/api/accounts/${id}`)),
  categories: () => unwrap<Category[]>(api.get("/api/categories")),
  createCategory: (payload: CategoryPayload) => unwrap<Category>(api.post("/api/categories", payload)),
  updateCategory: (id: number, payload: CategoryPayload) => unwrap<Category>(api.put(`/api/categories/${id}`, payload)),
  deleteCategory: (id: number) => unwrap<void>(api.delete(`/api/categories/${id}`)),
  transactions: () => unwrap<Transaction[]>(api.get("/api/transactions")),
  createTransaction: (payload: TransactionPayload) => unwrap<Transaction>(api.post("/api/transactions", payload)),
  deleteTransaction: (id: number) => unwrap<void>(api.delete(`/api/transactions/${id}`)),
  rules: () => unwrap<Rule[]>(api.get("/api/rules")),
  createRule: (payload: RulePayload) => unwrap<Rule>(api.post("/api/rules", payload)),
  updateRule: (id: number, payload: RulePayload) => unwrap<Rule>(api.put(`/api/rules/${id}`, payload)),
  deleteRule: (id: number) => unwrap<void>(api.delete(`/api/rules/${id}`)),
  importBatches: () => unwrap<ImportBatch[]>(api.get("/api/import-batches")),
  createImportBatch: (payload: ImportBatchPayload) => unwrap<ImportBatch>(api.post("/api/import-batches", payload)),
  deleteImportBatch: (id: number) => unwrap<void>(api.delete(`/api/import-batches/${id}`)),
  importCsv: (id: number, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return unwrap<ImportBatch>(api.post(`/api/import-batches/${id}/import`, formData));
  },
};
