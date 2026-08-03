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
import type { Category, CategoryPayload } from "../types/api";
import { titleCase } from "../utils/format";

const schema = z.object({ name: z.string().trim().min(1, "Category name is required.").max(100), type: z.enum(["INCOME", "EXPENSE"]), icon: z.string().max(100).optional(), color: z.string().max(20).optional() });
type Values = z.infer<typeof schema>;

export function CategoriesPage() {
  const client = useQueryClient(); const location = useLocation(); const navigate = useNavigate(); const [editing, setEditing] = useState<Category | null>(null); const [deleting, setDeleting] = useState<Category | null>(null); const [type, setType] = useState("ALL");
  const categories = useQuery({ queryKey: ["categories"], queryFn: financeApi.categories });
  const controls = useListControls(categories.data?.filter((category) => type === "ALL" || category.type === type), { search: (category) => `${category.name} ${category.type} ${category.icon ?? ""}`, sort: (a, b) => a.name.localeCompare(b.name) });
  const close = () => { setEditing(null); if (location.pathname.endsWith("/new")) navigate("/categories"); }; const refresh = () => client.invalidateQueries({ queryKey: ["categories"] });
  const create = useMutation({ mutationFn: financeApi.createCategory, onSuccess: () => { refresh(); close(); } }); const update = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: CategoryPayload }) => financeApi.updateCategory(id, payload), onSuccess: () => { refresh(); close(); } }); const remove = useMutation({ mutationFn: financeApi.deleteCategory, onSuccess: () => { refresh(); setDeleting(null); } });
  if (categories.isLoading) return <><PageHeader title="Categories" description="Keep income and expenses consistently classified."/><TableSkeleton/></>;
  if (categories.isError) return <><PageHeader title="Categories" description="Keep income and expenses consistently classified."/><ErrorState retry={() => categories.refetch()}/></>;
  return <><PageHeader title="Categories" description="Keep income and expenses consistently classified." actions={<Button onClick={() => navigate("/categories/new")}><Plus size={17}/>Add category</Button>}/><DataToolbar value={controls.term} onChange={(event) => controls.setTerm(event.target.value)} placeholder="Search categories" filters={<label className="filter-label">Type<select className="input compact-input" value={type} onChange={(event) => setType(event.target.value)}><option value="ALL">All types</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option></select></label>}/>{controls.total === 0 ? <EmptyState title="No categories found" description={controls.term ? "Try a different search term." : "Create a category to classify your transactions."} action={<Button onClick={() => navigate("/categories/new")}><Plus size={17}/>Add category</Button>}/> : <><div className="card table-wrap"><table><thead><tr><th>Category</th><th>Type</th><th>Icon</th><th>Color</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{controls.items.map((category) => <tr key={category.id}><td><strong>{category.name}</strong></td><td><span className={`badge ${category.type.toLowerCase()}`}>{titleCase(category.type)}</span></td><td>{category.icon || "—"}</td><td>{category.color || "—"}</td><td className="table-actions"><Button variant="ghost" aria-label={`Edit ${category.name}`} onClick={() => setEditing(category)}><Pencil size={16}/></Button><Button variant="ghost" aria-label={`Delete ${category.name}`} onClick={() => setDeleting(category)}><Trash2 size={16}/></Button></td></tr>)}</tbody></table></div><Pagination page={controls.page} pageCount={controls.pageCount} onPageChange={controls.setPage}/></>}<CategoryForm open={location.pathname.endsWith("/new") || Boolean(editing)} category={editing} busy={create.isPending || update.isPending} error={create.error ?? update.error} onClose={close} onSubmit={(payload) => editing ? update.mutate({ id: editing.id, payload }) : create.mutate(payload)}/><ConfirmDialog open={Boolean(deleting)} onOpenChange={(open) => !open && setDeleting(null)} title="Delete category?" description="This cannot be undone. Categories assigned to transactions may not be deleted." busy={remove.isPending} onConfirm={() => deleting && remove.mutate(deleting.id)}/></>;
}

function CategoryForm({ open, category, busy, error, onClose, onSubmit }: { open: boolean; category: Category | null; busy: boolean; error: Error | null; onClose: () => void; onSubmit: (payload: CategoryPayload) => void }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: "", type: "EXPENSE", icon: "", color: "" } });
  useEffect(() => form.reset(category ? { name: category.name, type: category.type, icon: category.icon ?? "", color: category.color ?? "" } : { name: "", type: "EXPENSE", icon: "", color: "" }), [category, form, open]);
  return <Modal open={open} onOpenChange={(value) => !value && onClose()} title={category ? "Edit category" : "Add category"} description="Use consistent labels to keep your reports meaningful."><form className="form-grid modal-form" onSubmit={form.handleSubmit(onSubmit)}><InputField id="categoryName" label="Name" error={form.formState.errors.name?.message} {...form.register("name")}/><SelectField id="categoryType" label="Type" error={form.formState.errors.type?.message} {...form.register("type")}><option value="EXPENSE">Expense</option><option value="INCOME">Income</option></SelectField><InputField id="categoryIcon" label="Icon name (optional)" placeholder="e.g. groceries" error={form.formState.errors.icon?.message} {...form.register("icon")}/><InputField id="categoryColor" label="Color token (optional)" placeholder="e.g. blue" error={form.formState.errors.color?.message} {...form.register("color")}/>{error && <p className="field-error" role="alert">{errorMessage(error)}</p>}<div className="dialog-actions"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? "Saving…" : "Save category"}</Button></div></form></Modal>;
}
