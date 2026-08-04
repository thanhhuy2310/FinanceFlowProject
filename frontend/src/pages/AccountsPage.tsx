import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
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
import type { Account, AccountPayload, AccountType } from "../types/api";
import { formatCurrency, titleCase } from "../utils/format";

const ACCOUNT_TYPES: AccountType[] = ["BANK", "CASH", "EWALLET", "CREDIT_CARD"];

const accountSchema = z.object({
  accountName: z.string().trim().min(1, "Account name is required.").max(100),
  accountNumber: z.string().trim().min(1, "Account number is required.").max(50),
  accountType: z.enum(["BANK", "CASH", "EWALLET", "CREDIT_CARD"]),
  providerId: z.coerce.number().positive("Choose a provider."),
  balance: z.coerce.number().min(0, "Initial balance cannot be negative."),
});

type AccountFormValues = z.infer<typeof accountSchema>;

const PAGE_DESCRIPTION = "Manage the places your money lives.";

export function AccountsPage() {
  const client = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [editing, setEditing] = useState<Account | null>(null);
  const [deleting, setDeleting] = useState<Account | null>(null);
  const [type, setType] = useState("ALL");

  // Queries
  const accounts = useQuery({
    queryKey: ["accounts"],
    queryFn: financeApi.accounts,
    placeholderData: [],
  });
  const providers = useQuery({
    queryKey: ["providers"],
    queryFn: financeApi.providers,
    placeholderData: [],
  });

  // Derived values
  const formOpen = location.pathname.endsWith("/new") || Boolean(editing);
  const controls = useListControls(
    accounts.data?.filter((account) => type === "ALL" || account.accountType === type),
    {
      search: (account) => `${account.accountName} ${account.accountNumber} ${account.accountType}`,
      sort: (left, right) => left.accountName.localeCompare(right.accountName),
      columns: [
        { key: "name", compare: (left, right) => left.accountName.localeCompare(right.accountName) },
        { key: "type", compare: (left, right) => left.accountType.localeCompare(right.accountType) },
        { key: "balance", compare: (left, right) => left.balance - right.balance },
      ],
    },
  );

  // Mutations
  const refresh = () => client.invalidateQueries({ queryKey: ["accounts"] });

  const closeForm = () => {
    setEditing(null);

    if (location.pathname.endsWith("/new")) {
      navigate("/accounts");
    }
  };

  const create = useMutation({
    mutationFn: financeApi.createAccount,
    onSuccess: () => {
      refresh();
      closeForm();
      toast("Account created.");
    },
  });

  const update = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AccountPayload }) =>
      financeApi.updateAccount(id, payload),
    onSuccess: () => {
      refresh();
      closeForm();
      toast("Account updated.");
    },
  });

  const remove = useMutation({
    mutationFn: financeApi.deleteAccount,
    onSuccess: () => {
      refresh();
      setDeleting(null);
      toast("Account deleted.");
    },
  });

  if (accounts.isLoading || providers.isLoading) {
    return (
      <>
        <PageHeader title="Accounts" description={PAGE_DESCRIPTION} />
        <TableSkeleton />
      </>
    );
  }

  if (accounts.isError || providers.isError) {
    return (
      <>
        <PageHeader title="Accounts" description={PAGE_DESCRIPTION} />
        <ErrorState
          retry={() => {
            accounts.refetch();
            providers.refetch();
          }}
        />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Accounts"
        description={PAGE_DESCRIPTION}
        actions={
          <Button onClick={() => navigate("/accounts/new")}>
            <Plus size={17} />
            Add account
          </Button>
        }
      />

      <DataToolbar
        value={controls.term}
        onChange={(event) => controls.setTerm(event.target.value)}
        placeholder="Search accounts"
        filters={
          <label className="filter-label">
            Type
            <select
              className="input compact-input"
              value={type}
              onChange={(event) => setType(event.target.value)}
            >
              <option value="ALL">All types</option>
              {ACCOUNT_TYPES.map((value) => (
                <option key={value} value={value}>
                  {titleCase(value)}
                </option>
              ))}
            </select>
          </label>
        }
      />

      {controls.total === 0 ? (
        <EmptyState
          title="No accounts found"
          description={
            controls.term ? "Try a different search term." : "Add your first account to start tracking balances."
          }
          action={
            <Button onClick={() => navigate("/accounts/new")}>
              <Plus size={17} />
              Add account
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
                      label="Account"
                      column="name"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <SortableHeader
                      label="Type"
                      column="type"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>Number</th>
                  <th>
                    <SortableHeader
                      label="Balance"
                      column="balance"
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
                {controls.items.map((account) => (
                  <tr key={account.id}>
                    <td>
                      <strong>{account.accountName}</strong>
                    </td>
                    <td>
                      <span className="badge">{titleCase(account.accountType)}</span>
                    </td>
                    <td>{account.accountNumber}</td>
                    <td className="amount">{formatCurrency(account.balance)}</td>
                    <td className="table-actions">
                      <Button
                        variant="ghost"
                        aria-label={`Edit ${account.accountName}`}
                        onClick={() => setEditing(account)}
                      >
                        <Pencil size={16} />
                      </Button>
                      <Button
                        variant="ghost"
                        aria-label={`Delete ${account.accountName}`}
                        onClick={() => setDeleting(account)}
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

      <AccountForm
        open={formOpen}
        account={editing}
        providers={providers.data ?? []}
        busy={create.isPending || update.isPending}
        error={create.error ?? update.error}
        onClose={closeForm}
        onSubmit={(payload) =>
          editing ? update.mutate({ id: editing.id, payload }) : create.mutate(payload)
        }
      />

      <ConfirmDialog
        open={Boolean(deleting)}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Delete account?"
        description="This cannot be undone, and the account history will be lost."
        busy={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </>
  );
}

interface AccountFormProps {
  open: boolean;
  account: Account | null;
  providers: { id: number; name: string }[];
  busy: boolean;
  error: Error | null;
  onClose: () => void;
  onSubmit: (payload: AccountPayload) => void;
}

const EMPTY_VALUES: AccountFormValues = {
  accountName: "",
  accountNumber: "",
  accountType: "BANK",
  providerId: 0,
  balance: 0,
};

function AccountForm({
  open,
  account,
  providers,
  busy,
  error,
  onClose,
  onSubmit,
}: AccountFormProps) {
  const form = useForm<AccountFormValues>({
    resolver: zodResolver(accountSchema),
    defaultValues: EMPTY_VALUES,
  });

  // Reset the form whenever the dialog opens or the target account changes.
  useEffect(() => {
    form.reset(
      account
        ? {
            accountName: account.accountName,
            accountNumber: account.accountNumber,
            accountType: account.accountType,
            providerId: account.providerId,
            balance: account.balance,
          }
        : EMPTY_VALUES,
    );
  }, [account, form, open]);

  return (
    <Modal
      open={open}
      onOpenChange={(value) => !value && onClose()}
      title={account ? "Edit account" : "Add account"}
      description="Account ownership is always tied to your signed-in profile."
    >
      <form className="form-grid modal-form" onSubmit={form.handleSubmit(onSubmit)}>
        <div className="form-grid two-col">
          <InputField
            id="accountName"
            label="Account name"
            error={form.formState.errors.accountName?.message}
            {...form.register("accountName")}
          />
          <InputField
            id="accountNumber"
            label="Account number"
            error={form.formState.errors.accountNumber?.message}
            {...form.register("accountNumber")}
          />
          <SelectField
            id="accountType"
            label="Account type"
            error={form.formState.errors.accountType?.message}
            {...form.register("accountType")}
          >
            {ACCOUNT_TYPES.map((type) => (
              <option key={type} value={type}>
                {titleCase(type)}
              </option>
            ))}
          </SelectField>
          <SelectField
            id="providerId"
            label="Provider"
            error={form.formState.errors.providerId?.message}
            {...form.register("providerId")}
          >
            <option value="">Select a provider</option>
            {providers.map((provider) => (
              <option key={provider.id} value={provider.id}>
                {provider.name}
              </option>
            ))}
          </SelectField>
        </div>

        <InputField
          id="balance"
          label="Initial balance"
          type="number"
          min="0"
          step="0.01"
          error={form.formState.errors.balance?.message}
          {...form.register("balance")}
        />

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
            {busy ? "Saving…" : "Save account"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
