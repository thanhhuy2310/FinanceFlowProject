import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";

import { errorMessage } from "../api/client";
import { financeApi } from "../api/finance";
import { Button } from "../components/ui/Button";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { DataToolbar } from "../components/ui/DataToolbar";
import { InputField, SelectField } from "../components/ui/FormField";
import { Modal } from "../components/ui/Modal";
import { PageHeader } from "../components/ui/PageHeader";
import { Pagination } from "../components/ui/Pagination";
import { SortableHeader } from "../components/ui/SortableHeader";
import { EmptyState, ErrorState, TableSkeleton } from "../components/ui/States";
import { useToast } from "../components/ui/toast-context";
import { useListControls } from "../hooks/useListControls";
import type { Transaction, TransactionPayload } from "../types/api";
import { downloadBlob } from "../utils/download";
import { formatCurrency, formatDate, titleCase, toLocalDateTimeInput } from "../utils/format";

const transactionSchema = z.object({
  amount: z.coerce.number().positive("Amount must be greater than zero."),
  description: z.string().max(255).optional(),
  transactionDate: z.string().min(1, "Date is required."),
  transactionType: z.enum(["INCOME", "EXPENSE"]),
  accountId: z.coerce.number().positive("Choose an account."),
  categoryId: z.coerce.number().positive("Choose a category."),
});

type TransactionFormValues = z.infer<typeof transactionSchema>;

const PAGE_DESCRIPTION = "Review and record your financial activity.";

export function TransactionsPage() {
  const client = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [deleting, setDeleting] = useState<Transaction | null>(null);
  const [type, setType] = useState("ALL");
  const [category, setCategory] = useState("ALL");
  const [account, setAccount] = useState("ALL");
  const [exporting, setExporting] = useState<"csv" | "xlsx" | null>(null);

  // Queries
  const transactions = useQuery({
    queryKey: ["transactions"],
    queryFn: financeApi.transactions,
    placeholderData: [],
  });
  const accounts = useQuery({
    queryKey: ["accounts"],
    queryFn: financeApi.accounts,
    placeholderData: [],
  });
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: financeApi.categories,
    placeholderData: [],
  });

  // Derived values
  const controls = useListControls(
    (transactions.data ?? []).filter(
      (transaction) =>
        (type === "ALL" || transaction.transactionType === type) &&
        (category === "ALL" || transaction.categoryId === Number(category)) &&
        (account === "ALL" || transaction.accountId === Number(account)),
    ),
    {
      search: (transaction) =>
        `${transaction.description ?? ""} ${transaction.accountName} ${transaction.categoryName}`,
      sort: (left, right) =>
        new Date(right.transactionDate).getTime() - new Date(left.transactionDate).getTime(),
      columns: [
        {
          key: "description",
          compare: (left, right) => (left.description ?? "").localeCompare(right.description ?? ""),
        },
        { key: "account", compare: (left, right) => left.accountName.localeCompare(right.accountName) },
        { key: "category", compare: (left, right) => left.categoryName.localeCompare(right.categoryName) },
        { key: "amount", compare: (left, right) => left.amount - right.amount },
        {
          key: "date",
          compare: (left, right) =>
            new Date(right.transactionDate).getTime() - new Date(left.transactionDate).getTime(),
          defaultDirection: "desc",
        },
      ],
    },
  );

  // Mutations
  const refresh = () => {
    client.invalidateQueries({ queryKey: ["transactions"] });
    client.invalidateQueries({ queryKey: ["dashboard"] });
    client.invalidateQueries({ queryKey: ["accounts"] });
  };

  const create = useMutation({
    mutationFn: financeApi.createTransaction,
    onSuccess: () => {
      refresh();
      navigate("/transactions");
      toast("Transaction added.");
    },
  });

  const remove = useMutation({
    mutationFn: financeApi.deleteTransaction,
    onSuccess: () => {
      refresh();
      setDeleting(null);
      toast("Transaction deleted.");
    },
  });

  // Export
  const exportTransactions = async (format: "csv" | "xlsx") => {
    try {
      setExporting(format);
      const response = await financeApi.exportTransactions(format);
      const disposition = response.headers["content-disposition"];
      const match = typeof disposition === "string" ? disposition.match(/filename="?([^";]+)"?/) : null;
      downloadBlob(
        response.data as Blob,
        match?.[1] ?? `transactions.${format}`,
      );
      toast("Transactions exported.");
    } catch {
      toast("Could not export transactions. Please try again.", "error");
    } finally {
      setExporting(null);
    }
  };

  if (transactions.isLoading || accounts.isLoading || categories.isLoading) {
    return (
      <>
        <PageHeader title="Transactions" description={PAGE_DESCRIPTION} />
        <TableSkeleton />
      </>
    );
  }

  if (transactions.isError || accounts.isError || categories.isError) {
    return (
      <>
        <PageHeader title="Transactions" description={PAGE_DESCRIPTION} />
        <ErrorState
          retry={() => {
            transactions.refetch();
            accounts.refetch();
            categories.refetch();
          }}
        />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Transactions"
        description={PAGE_DESCRIPTION}
        actions={
          <>
            <Button
              variant="secondary"
              disabled={exporting !== null}
              onClick={() => exportTransactions("csv")}
            >
              <Download size={17} />
              {exporting === "csv" ? "Exporting…" : "CSV"}
            </Button>
            <Button
              variant="secondary"
              disabled={exporting !== null}
              onClick={() => exportTransactions("xlsx")}
            >
              <Download size={17} />
              {exporting === "xlsx" ? "Exporting…" : "Excel"}
            </Button>
            <Button onClick={() => navigate("/transactions/new")}>
              <Plus size={17} />
              Add transaction
            </Button>
          </>
        }
      />

      <DataToolbar
        value={controls.term}
        onChange={(event) => controls.setTerm(event.target.value)}
        placeholder="Search transactions"
        filters={
          <>
            <label className="filter-label">
              Type
              <select
                className="input compact-input"
                value={type}
                onChange={(event) => setType(event.target.value)}
              >
                <option value="ALL">All types</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
              </select>
            </label>
            <label className="filter-label">
              Category
              <select
                className="input compact-input"
                value={category}
                onChange={(event) => setCategory(event.target.value)}
              >
                <option value="ALL">All categories</option>
                {categories.data?.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="filter-label">
              Account
              <select
                className="input compact-input"
                value={account}
                onChange={(event) => setAccount(event.target.value)}
              >
                <option value="ALL">All accounts</option>
                {accounts.data?.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.accountName}
                  </option>
                ))}
              </select>
            </label>
          </>
        }
      />

      {controls.total === 0 ? (
        <EmptyState
          title="No transactions found"
          description={
            controls.term
              ? "Try a different search term."
              : "Record a transaction to start building your history."
          }
          action={
            <Button onClick={() => navigate("/transactions/new")}>
              <Plus size={17} />
              Add transaction
            </Button>
          }
        />
      ) : (
        <>
          <div className="card table-wrap">
            <table>
              <thead>
                <tr>
                  <th>
                    <SortableHeader
                      label="Description"
                      column="description"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <SortableHeader
                      label="Account"
                      column="account"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <SortableHeader
                      label="Category"
                      column="category"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <SortableHeader
                      label="Amount"
                      column="amount"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <SortableHeader
                      label="Date"
                      column="date"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {controls.items.map((transaction) => (
                  <tr key={transaction.id}>
                    <td>
                      <strong>{transaction.description || "Untitled transaction"}</strong>
                      <small className="row-subtitle">
                        {titleCase(transaction.transactionType)}
                      </small>
                    </td>
                    <td>{transaction.accountName}</td>
                    <td>
                      <span
                        className={`badge ${
                          transaction.transactionType === "INCOME" ? "income" : "expense"
                        }`}
                      >
                        {transaction.categoryName}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`amount ${
                          transaction.transactionType === "INCOME" ? "income" : "expense"
                        }`}
                      >
                        {formatCurrency(transaction.amount)}
                      </span>
                    </td>
                    <td>{formatDate(transaction.transactionDate)}</td>
                    <td className="table-actions">
                      <Button
                        variant="ghost"
                        aria-label={`Delete ${transaction.description || "transaction"}`}
                        onClick={() => setDeleting(transaction)}
                      >
                        <Trash2 size={16} />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination
            page={controls.page}
            pageCount={controls.pageCount}
            onPageChange={controls.setPage}
          />
        </>
      )}

      <TransactionForm
        open={location.pathname.endsWith("/new")}
        accounts={accounts.data ?? []}
        categories={categories.data ?? []}
        busy={create.isPending}
        error={create.error}
        onClose={() => navigate("/transactions")}
        onSubmit={(payload) => create.mutate(payload)}
      />

      <ConfirmDialog
        open={Boolean(deleting)}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Delete transaction?"
        description="Deleting it will also reverse its balance effect."
        busy={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </>
  );
}

interface TransactionFormProps {
  open: boolean;
  accounts: { id: number; accountName: string }[];
  categories: { id: number; name: string; type: string }[];
  busy: boolean;
  error: Error | null;
  onClose: () => void;
  onSubmit: (payload: TransactionPayload) => void;
}

function TransactionForm({
  open,
  accounts,
  categories,
  busy,
  error,
  onClose,
  onSubmit,
}: TransactionFormProps) {
  const form = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionSchema),
    defaultValues: {
      amount: 0,
      description: "",
      transactionDate: toLocalDateTimeInput(),
      transactionType: "EXPENSE",
      accountId: 0,
      categoryId: 0,
    },
  });

  // Only show categories matching the selected transaction type.
  const transactionType = useWatch({ control: form.control, name: "transactionType" });

  useEffect(() => {
    if (open) {
      form.reset({
        amount: 0,
        description: "",
        transactionDate: toLocalDateTimeInput(),
        transactionType: "EXPENSE",
        accountId: 0,
        categoryId: 0,
      });
    }
  }, [form, open]);

  return (
    <Modal
      open={open}
      onOpenChange={(value) => !value && onClose()}
      title="Add transaction"
      description="The selected account balance will update automatically."
    >
      <form
        className="form-grid modal-form"
        onSubmit={form.handleSubmit((values) =>
          onSubmit({ ...values, description: values.description || null }),
        )}
      >
        <div className="form-grid two-col">
          <InputField
            id="transactionAmount"
            label="Amount"
            type="number"
            min="0.01"
            step="0.01"
            error={form.formState.errors.amount?.message}
            {...form.register("amount")}
          />
          <SelectField
            id="transactionType"
            label="Type"
            error={form.formState.errors.transactionType?.message}
            {...form.register("transactionType")}
          >
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </SelectField>
        </div>

        <InputField
          id="transactionDescription"
          label="Description (optional)"
          error={form.formState.errors.description?.message}
          {...form.register("description")}
        />

        <InputField
          id="transactionDate"
          label="Date and time"
          type="datetime-local"
          error={form.formState.errors.transactionDate?.message}
          {...form.register("transactionDate")}
        />

        <div className="form-grid two-col">
          <SelectField
            id="transactionAccount"
            label="Account"
            error={form.formState.errors.accountId?.message}
            {...form.register("accountId")}
          >
            <option value="">Select an account</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.accountName}
              </option>
            ))}
          </SelectField>
          <SelectField
            id="transactionCategory"
            label="Category"
            error={form.formState.errors.categoryId?.message}
            {...form.register("categoryId")}
          >
            <option value="">Select a category</option>
            {categories
              .filter((category) => category.type === transactionType)
              .map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
          </SelectField>
        </div>

        {error && (
          <p className="field-error" role="alert">
            {errorMessage(error)}
          </p>
        )}

        <div className="dialog-actions">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save transaction"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
