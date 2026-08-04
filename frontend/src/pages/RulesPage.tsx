import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDown, Pencil, Plus, Trash2 } from "lucide-react";
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
import type { Rule, RulePayload, RulePreview } from "../types/api";

const ruleSchema = z.object({
  keyword: z.string().trim().min(1, "Keyword is required.").max(100),
  categoryId: z.coerce.number().positive("Choose a category."),
  priority: z.coerce.number().int().min(1, "Priority must be at least 1."),
  isActive: z.boolean(),
});

type RuleFormValues = z.infer<typeof ruleSchema>;

const PAGE_DESCRIPTION = "Set simple keyword-based categorisation rules.";

export function RulesPage() {
  const client = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [editing, setEditing] = useState<Rule | null>(null);
  const [deleting, setDeleting] = useState<Rule | null>(null);
  const [status, setStatus] = useState("ALL");

  // Queries
  const rules = useQuery({
    queryKey: ["rules"],
    queryFn: financeApi.rules,
    placeholderData: [],
  });
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: financeApi.categories,
    placeholderData: [],
  });

  // Derived values
  const controls = useListControls(
    (rules.data ?? []).filter((rule) => status === "ALL" || String(rule.isActive) === status),
    {
      search: (rule) => `${rule.keyword} ${rule.categoryName}`,
      sort: (left, right) => left.priority - right.priority,
      columns: [
        { key: "keyword", compare: (left, right) => left.keyword.localeCompare(right.keyword) },
        { key: "category", compare: (left, right) => left.categoryName.localeCompare(right.categoryName) },
        { key: "priority", compare: (left, right) => left.priority - right.priority },
        { key: "status", compare: (left, right) => Number(right.isActive) - Number(left.isActive) },
      ],
    },
  );

  // Mutations
  const refresh = () => client.invalidateQueries({ queryKey: ["rules"] });

  const closeForm = () => {
    setEditing(null);

    if (location.pathname.endsWith("/new")) {
      navigate("/rules");
    }
  };

  const create = useMutation({
    mutationFn: financeApi.createRule,
    onSuccess: () => {
      refresh();
      closeForm();
      toast("Rule created.");
    },
  });

  const update = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: RulePayload }) =>
      financeApi.updateRule(id, payload),
    onSuccess: () => {
      refresh();
      closeForm();
      toast("Rule updated.");
    },
  });

  const remove = useMutation({
    mutationFn: financeApi.deleteRule,
    onSuccess: () => {
      refresh();
      setDeleting(null);
      toast("Rule deleted.");
    },
  });

  if (rules.isLoading || categories.isLoading) {
    return (
      <>
        <PageHeader title="Rules" description={PAGE_DESCRIPTION} />
        <TableSkeleton />
      </>
    );
  }

  if (rules.isError || categories.isError) {
    return (
      <>
        <PageHeader title="Rules" description={PAGE_DESCRIPTION} />
        <ErrorState
          retry={() => {
            rules.refetch();
            categories.refetch();
          }}
        />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Rules"
        description={PAGE_DESCRIPTION}
        actions={
          <Button onClick={() => navigate("/rules/new")}>
            <Plus size={17} />
            Add rule
          </Button>
        }
      />

      <section className="rule-help" aria-label="How rules work">
        <div className="card card-pad">
          <h2>How rules work</h2>
          <p>
            Rules automatically assign categories to imported transactions. If a
            transaction description contains a keyword, FinanceFlow assigns the
            selected category. When several rules match, the one with the lowest
            priority number wins.
          </p>
          <div className="rule-example">
            <code>Keyword: Starbucks</code>
            <ArrowDown size={16} className="rule-arrow" />
            <code>Category: Food</code>
          </div>
        </div>
        <RulePreviewCard />
      </section>

      <DataToolbar
        value={controls.term}
        onChange={(event) => controls.setTerm(event.target.value)}
        placeholder="Search rules"
        filters={
          <label className="filter-label">
            Status
            <select
              className="input compact-input"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="ALL">All rules</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </label>
        }
      />

      {controls.total === 0 ? (
        <EmptyState
          title="No rules found"
          description={
            controls.term
              ? "Try a different keyword."
              : "Create a rule to categorise matching transactions automatically."
          }
          action={
            <Button onClick={() => navigate("/rules/new")}>
              <Plus size={17} />
              Add rule
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
                      label="Keyword"
                      column="keyword"
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
                      label="Priority"
                      column="priority"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>
                    <SortableHeader
                      label="Status"
                      column="status"
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
                {controls.items.map((rule) => (
                  <tr key={rule.id}>
                    <td>
                      <strong>{rule.keyword}</strong>
                    </td>
                    <td>{rule.categoryName}</td>
                    <td>{rule.priority}</td>
                    <td>
                      <span className={`badge ${rule.isActive ? "income" : "expense"}`}>
                        {rule.isActive ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td className="table-actions">
                      <Button
                        variant="ghost"
                        aria-label={`Edit ${rule.keyword}`}
                        onClick={() => setEditing(rule)}
                      >
                        <Pencil size={16} />
                      </Button>
                      <Button
                        variant="ghost"
                        aria-label={`Delete ${rule.keyword}`}
                        onClick={() => setDeleting(rule)}
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

      <RuleForm
        open={location.pathname.endsWith("/new") || Boolean(editing)}
        rule={editing}
        categories={categories.data ?? []}
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
        title="Delete rule?"
        description="This will stop automatic matching for the keyword."
        busy={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </>
  );
}

interface RuleFormProps {
  open: boolean;
  rule: Rule | null;
  categories: { id: number; name: string }[];
  busy: boolean;
  error: Error | null;
  onClose: () => void;
  onSubmit: (payload: RulePayload) => void;
}

/** Lets users test how a description would be categorised, before saving. */
function RulePreviewCard() {
  const [description, setDescription] = useState("");
  const [result, setResult] = useState<RulePreview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const test = async () => {
    if (!description.trim() || busy) {
      return;
    }

    setBusy(true);
    setError(null);
    try {
      setResult(await financeApi.rulePreview(description));
    } catch (err) {
      setError(errorMessage(err));
      setResult(null);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card card-pad">
      <h2>Test a description</h2>
      <p>Paste a transaction description to see which rule would match it.</p>
      <div className="form-grid">
        <label className="field">
          <span className="field-label" id="rulePreviewLabel">
            Description
          </span>
          <input
            className="input"
            aria-labelledby="rulePreviewLabel"
            placeholder="e.g. Starbucks Hanoi #102"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                test();
              }
            }}
          />
        </label>
        <Button onClick={test} disabled={busy || !description.trim()}>
          {busy ? "Testing…" : "Preview match"}
        </Button>
      </div>

      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}

      {result && (
        <div className="preview-result" role="status">
          <span>{description}</span>
          <ArrowDown size={16} className="rule-arrow" />
          {result.matched ? (
            <span className="preview-match">
              {result.keyword} → {result.categoryName}
            </span>
          ) : (
            <span className="preview-no-match">No rule matched</span>
          )}
        </div>
      )}
    </div>
  );
}

const EMPTY_VALUES: RuleFormValues = {
  keyword: "",
  categoryId: 0,
  priority: 1,
  isActive: true,
};

function RuleForm({ open, rule, categories, busy, error, onClose, onSubmit }: RuleFormProps) {
  const form = useForm<RuleFormValues>({
    resolver: zodResolver(ruleSchema),
    defaultValues: EMPTY_VALUES,
  });

  useEffect(() => {
    form.reset(
      rule
        ? {
            keyword: rule.keyword,
            categoryId: rule.categoryId,
            priority: rule.priority,
            isActive: rule.isActive,
          }
        : EMPTY_VALUES,
    );
  }, [form, open, rule]);

  return (
    <Modal
      open={open}
      onOpenChange={(value) => !value && onClose()}
      title={rule ? "Edit rule" : "Add rule"}
      description="The first matching active keyword is used for automatic categorisation."
    >
      <form className="form-grid modal-form" onSubmit={form.handleSubmit(onSubmit)}>
        <InputField
          id="ruleKeyword"
          label="Keyword"
          error={form.formState.errors.keyword?.message}
          {...form.register("keyword")}
        />

        <SelectField
          id="ruleCategory"
          label="Category"
          error={form.formState.errors.categoryId?.message}
          {...form.register("categoryId")}
        >
          <option value="">Select a category</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </SelectField>

        <div className="form-grid two-col">
          <InputField
            id="rulePriority"
            label="Priority"
            type="number"
            min="1"
            error={form.formState.errors.priority?.message}
            {...form.register("priority")}
          />
          <label className="checkbox-field">
            <input type="checkbox" {...form.register("isActive")} />
            <span>Rule is active</span>
          </label>
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
            {busy ? "Saving…" : "Save rule"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
