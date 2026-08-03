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
import { EmptyState, ErrorState, TableSkeleton } from "../components/ui/States";
import { useListControls } from "../hooks/useListControls";
import type { Rule, RulePayload } from "../types/api";

const schema = z.object({ keyword: z.string().trim().min(1, "Keyword is required.").max(100), categoryId: z.coerce.number().positive("Choose a category."), priority: z.coerce.number().int().min(1, "Priority must be at least 1."), isActive: z.boolean() });
type Values = z.infer<typeof schema>;

export function RulesPage() {
  const client = useQueryClient(); const location = useLocation(); const navigate = useNavigate();
  const [editing, setEditing] = useState<Rule | null>(null); const [deleting, setDeleting] = useState<Rule | null>(null); const [status, setStatus] = useState("ALL");
  const rules = useQuery({ queryKey: ["rules"], queryFn: financeApi.rules, initialData: [] }); const categories = useQuery({ queryKey: ["categories"], queryFn: financeApi.categories, initialData: [] });
  const controls = useListControls(rules.data.filter((rule) => status === "ALL" || String(rule.isActive) === status), { search: (rule) => `${rule.keyword} ${rule.categoryName}`, sort: (a, b) => a.priority - b.priority });
  const close = () => { setEditing(null); if (location.pathname.endsWith("/new")) navigate("/rules"); }; const refresh = () => client.invalidateQueries({ queryKey: ["rules"] });
  const create = useMutation({ mutationFn: financeApi.createRule, onSuccess: () => { refresh(); close(); } }); const update = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: RulePayload }) => financeApi.updateRule(id, payload), onSuccess: () => { refresh(); close(); } }); const remove = useMutation({ mutationFn: financeApi.deleteRule, onSuccess: () => { refresh(); setDeleting(null); } });
  if (rules.isLoading || categories.isLoading) return <><PageHeader title="Rules" description="Set simple keyword-based categorisation rules."/><TableSkeleton/></>;
  if (rules.isError || categories.isError) return <><PageHeader title="Rules" description="Set simple keyword-based categorisation rules."/><ErrorState retry={() => { rules.refetch(); categories.refetch(); }}/></>;
  return <><PageHeader title="Rules" description="Set simple keyword-based categorisation rules." actions={<Button onClick={() => navigate("/rules/new")}><Plus size={17}/>Add rule</Button>}/><DataToolbar value={controls.term} onChange={(event) => controls.setTerm(event.target.value)} placeholder="Search rules" filters={<label className="filter-label">Status<select className="input compact-input" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All rules</option><option value="true">Active</option><option value="false">Inactive</option></select></label>}/>{controls.total === 0 ? <EmptyState title="No rules found" description={controls.term ? "Try a different keyword." : "Create a rule to categorise matching transactions automatically."} action={<Button onClick={() => navigate("/rules/new")}><Plus size={17}/>Add rule</Button>}/> : <><div className="card table-wrap"><table><thead><tr><th>Keyword</th><th>Category</th><th>Priority</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{controls.items.map((rule) => <tr key={rule.id}><td><strong>{rule.keyword}</strong></td><td>{rule.categoryName}</td><td>{rule.priority}</td><td><span className={`badge ${rule.isActive ? "income" : ""}`}>{rule.isActive ? "Active" : "Inactive"}</span></td><td className="table-actions"><Button variant="ghost" aria-label={`Edit ${rule.keyword}`} onClick={() => setEditing(rule)}><Pencil size={16}/></Button><Button variant="ghost" aria-label={`Delete ${rule.keyword}`} onClick={() => setDeleting(rule)}><Trash2 size={16}/></Button></td></tr>)}</tbody></table></div><Pagination page={controls.page} pageCount={controls.pageCount} onPageChange={controls.setPage}/></>}<RuleForm open={location.pathname.endsWith("/new") || Boolean(editing)} rule={editing} categories={categories.data} busy={create.isPending || update.isPending} error={create.error ?? update.error} onClose={close} onSubmit={(payload) => editing ? update.mutate({ id: editing.id, payload }) : create.mutate(payload)}/><ConfirmDialog open={Boolean(deleting)} onOpenChange={(open) => !open && setDeleting(null)} title="Delete rule?" description="This will stop automatic matching for the keyword." busy={remove.isPending} onConfirm={() => deleting && remove.mutate(deleting.id)}/></>;
}

function RuleForm({ open, rule, categories, busy, error, onClose, onSubmit }: { open: boolean; rule: Rule | null; categories: { id: number; name: string }[]; busy: boolean; error: Error | null; onClose: () => void; onSubmit: (payload: RulePayload) => void }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { keyword: "", categoryId: 0, priority: 1, isActive: true } });
  useEffect(() => form.reset(rule ? { keyword: rule.keyword, categoryId: rule.categoryId, priority: rule.priority, isActive: rule.isActive } : { keyword: "", categoryId: 0, priority: 1, isActive: true }), [form, open, rule]);
  return <Modal open={open} onOpenChange={(value) => !value && onClose()} title={rule ? "Edit rule" : "Add rule"} description="The first matching active keyword is used for automatic categorisation."><form className="form-grid modal-form" onSubmit={form.handleSubmit(onSubmit)}><InputField id="ruleKeyword" label="Keyword" error={form.formState.errors.keyword?.message} {...form.register("keyword")}/><SelectField id="ruleCategory" label="Category" error={form.formState.errors.categoryId?.message} {...form.register("categoryId")}><option value="">Select a category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</SelectField><InputField id="rulePriority" label="Priority" type="number" min="1" error={form.formState.errors.priority?.message} {...form.register("priority")}/><label className="checkbox-field"><input type="checkbox" {...form.register("isActive")}/> <span>Rule is active</span></label>{error && <p className="field-error" role="alert">{errorMessage(error)}</p>}<div className="dialog-actions"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? "Saving…" : "Save rule"}</Button></div></form></Modal>;
}
