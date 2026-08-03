import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { errorMessage } from "../api/client";
import { financeApi } from "../api/finance";
import { Button } from "../components/ui/Button";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { DataToolbar } from "../components/ui/DataToolbar";
import { InputField } from "../components/ui/FormField";
import { Modal } from "../components/ui/Modal";
import { PageHeader } from "../components/ui/PageHeader";
import { Pagination } from "../components/ui/Pagination";
import { EmptyState, ErrorState, TableSkeleton } from "../components/ui/States";
import { useListControls } from "../hooks/useListControls";
import type { ImportBatch, ImportBatchPayload } from "../types/api";
import { formatDate, titleCase } from "../utils/format";

const schema = z.object({ fileName: z.string().trim().min(1, "File name is required.").max(255, "File name must be 255 characters or fewer.") });
type Values = z.infer<typeof schema>;

export function ImportBatchesPage() {
  const client = useQueryClient(); const location = useLocation(); const navigate = useNavigate(); const [deleting, setDeleting] = useState<ImportBatch | null>(null);
  const batches = useQuery({ queryKey: ["import-batches"], queryFn: financeApi.importBatches, initialData: [] });
  const controls = useListControls(batches.data, { search: (batch) => batch.fileName, sort: (a, b) => new Date(b.importedAt).getTime() - new Date(a.importedAt).getTime() });
  const refresh = () => client.invalidateQueries({ queryKey: ["import-batches"] });
  const create = useMutation({ mutationFn: financeApi.createImportBatch, onSuccess: () => { refresh(); navigate("/imports"); } });
  const remove = useMutation({ mutationFn: financeApi.deleteImportBatch, onSuccess: () => { refresh(); setDeleting(null); } });
  if (batches.isLoading) return <><PageHeader title="Import batches" description="Prepare and track CSV imports."/><TableSkeleton/></>;
  if (batches.isError) return <><PageHeader title="Import batches" description="Prepare and track CSV imports."/><ErrorState retry={() => batches.refetch()}/></>;
  return <><PageHeader title="Import batches" description="Prepare and track CSV imports." actions={<Button onClick={() => navigate("/imports/new")}><Plus size={17}/>New import batch</Button>}/><DataToolbar value={controls.term} onChange={(event) => controls.setTerm(event.target.value)} placeholder="Search import batches"/>{controls.total === 0 ? <EmptyState title="No import batches found" description={controls.term ? "Try a different search term." : "Register a file name to start a CSV import batch."} action={<Button onClick={() => navigate("/imports/new")}><Plus size={17}/>New import batch</Button>}/> : <><div className="card table-wrap"><table><thead><tr><th>File name</th><th>Status</th><th>Imported at</th><th>Rows</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{controls.items.map((batch) => <tr key={batch.id}><td><strong>{batch.fileName}</strong>{batch.errorMessage && <small className="row-subtitle">{batch.errorMessage}</small>}</td><td><span className={`badge ${batch.status === "COMPLETED" ? "income" : batch.status === "FAILED" ? "danger-icon" : ""}`}>{titleCase(batch.status)}</span></td><td>{formatDate(batch.importedAt)}</td><td>{batch.successRows} / {batch.totalRows}{batch.failedRows > 0 && <small className="row-subtitle">{batch.failedRows} failed</small>}</td><td className="table-actions"><Button variant="ghost" aria-label={`Delete ${batch.fileName}`} onClick={() => setDeleting(batch)}><Trash2 size={16}/></Button></td></tr>)}</tbody></table></div><Pagination page={controls.page} pageCount={controls.pageCount} onPageChange={controls.setPage}/></>}<ImportBatchForm open={location.pathname.endsWith("/new")} busy={create.isPending} error={create.error} onClose={() => navigate("/imports")} onSubmit={(payload) => create.mutate(payload)}/><ConfirmDialog open={Boolean(deleting)} onOpenChange={(open) => !open && setDeleting(null)} title="Delete import batch?" description="This removes the batch record from your history." busy={remove.isPending} onConfirm={() => deleting && remove.mutate(deleting.id)}/></>;
}

function ImportBatchForm({ open, busy, error, onClose, onSubmit }: { open: boolean; busy: boolean; error: Error | null; onClose: () => void; onSubmit: (payload: ImportBatchPayload) => void }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { fileName: "" } });
  useEffect(() => { if (open) form.reset({ fileName: "" }); }, [form, open]);
  return <Modal open={open} onOpenChange={(value) => !value && onClose()} title="New import batch" description="Register the CSV file you plan to import."><form className="form-grid modal-form" onSubmit={form.handleSubmit(onSubmit)}><InputField id="importFileName" label="File name" placeholder="e.g. october-statement.csv" error={form.formState.errors.fileName?.message} {...form.register("fileName")}/>{error && <p className="field-error" role="alert">{errorMessage(error)}</p>}<div className="dialog-actions"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button type="submit" disabled={busy}>{busy ? "Saving…" : "Create batch"}</Button></div></form></Modal>;
}
