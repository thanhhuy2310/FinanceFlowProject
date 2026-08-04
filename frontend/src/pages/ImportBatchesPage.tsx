import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download, Plus, Trash2, Upload } from "lucide-react";
import { useEffect, useRef, useState } from "react";
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
import { SortableHeader } from "../components/ui/SortableHeader";
import { EmptyState, ErrorState, TableSkeleton } from "../components/ui/States";
import { useToast } from "../components/ui/toast-context";
import { useListControls } from "../hooks/useListControls";
import type { ImportBatch, ImportBatchPayload } from "../types/api";
import { downloadBlob } from "../utils/download";
import { formatDate, titleCase } from "../utils/format";

const importBatchSchema = z.object({
  fileName: z.string().trim().min(1, "File name is required.").max(255, "File name must be 255 characters or fewer."),
});

type ImportBatchFormValues = z.infer<typeof importBatchSchema>;

const PAGE_DESCRIPTION = "Upload CSV files to import transactions in bulk.";

export function ImportBatchesPage() {
  const client = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [deleting, setDeleting] = useState<ImportBatch | null>(null);
  const [uploadTarget, setUploadTarget] = useState<ImportBatch | null>(null);
  const [downloadingTemplate, setDownloadingTemplate] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Query
  const batches = useQuery({
    queryKey: ["import-batches"],
    queryFn: financeApi.importBatches,
    placeholderData: [],
  });

  // Derived values
  const controls = useListControls(batches.data, {
    search: (batch) => batch.fileName,
    sort: (left, right) =>
      new Date(right.importedAt).getTime() - new Date(left.importedAt).getTime(),
    columns: [
      { key: "fileName", compare: (left, right) => left.fileName.localeCompare(right.fileName) },
      { key: "status", compare: (left, right) => left.status.localeCompare(right.status) },
      {
        key: "importedAt",
        compare: (left, right) =>
          new Date(right.importedAt).getTime() - new Date(left.importedAt).getTime(),
        defaultDirection: "desc",
      },
    ],
  });

  // Mutations
  const refresh = () => {
    client.invalidateQueries({ queryKey: ["import-batches"] });
    client.invalidateQueries({ queryKey: ["transactions"] });
    client.invalidateQueries({ queryKey: ["dashboard"] });
  };

  const create = useMutation({
    mutationFn: financeApi.createImportBatch,
    onSuccess: () => {
      refresh();
      navigate("/imports");
      toast("Import batch created.");
    },
  });

  const remove = useMutation({
    mutationFn: financeApi.deleteImportBatch,
    onSuccess: () => {
      refresh();
      setDeleting(null);
      toast("Import batch deleted.");
    },
  });

  const upload = useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => financeApi.importCsv(id, file),
    onSuccess: () => {
      refresh();
      setUploadTarget(null);
      toast("CSV import completed.");
    },
    onError: () => {
      refresh();
      setUploadTarget(null);
    },
  });

  // Event handlers
  const pickFile = (batch: ImportBatch) => {
    setUploadTarget(batch);
    fileInputRef.current?.click();
  };

  const onFileSelected = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];

    event.target.value = "";

    if (file && uploadTarget) {
      upload.mutate({ id: uploadTarget.id, file });
    }
  };

  const downloadTemplate = async () => {
    try {
      setDownloadingTemplate(true);
      const response = await financeApi.importTemplate();
      downloadBlob(response.data as Blob, "financeflow-import-template.csv");
      toast("Template downloaded.");
    } catch {
      toast("Could not download the template. Please try again.", "error");
    } finally {
      setDownloadingTemplate(false);
    }
  };

  if (batches.isLoading) {
    return (
      <>
        <PageHeader title="Import batches" description={PAGE_DESCRIPTION} />
        <TableSkeleton />
      </>
    );
  }

  if (batches.isError) {
    return (
      <>
        <PageHeader title="Import batches" description={PAGE_DESCRIPTION} />
        <ErrorState retry={() => batches.refetch()} />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Import batches"
        description={PAGE_DESCRIPTION}
        actions={
          <>
            <Button variant="secondary" disabled={downloadingTemplate} onClick={downloadTemplate}>
              <Download size={17} />
              {downloadingTemplate ? "Downloading…" : "CSV template"}
            </Button>
            <Button onClick={() => navigate("/imports/new")}>
              <Plus size={17} />
              New import batch
            </Button>
          </>
        }
      />

      {upload.isSuccess && upload.data && (
        <ImportSummary batch={upload.data} />
      )}

      <DataToolbar
        value={controls.term}
        onChange={(event) => controls.setTerm(event.target.value)}
        placeholder="Search import batches"
      />

      {upload.error && (
        <p className="field-error" role="alert">
          {errorMessage(upload.error)}
        </p>
      )}

      {controls.total === 0 ? (
        <EmptyState
          title="No import batches found"
          description={
            controls.term
              ? "Try a different search term."
              : "Create a batch and upload a CSV to import transactions in bulk."
          }
          action={
            <Button onClick={() => navigate("/imports/new")}>
              <Plus size={17} />
              New import batch
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
                      label="File name"
                      column="fileName"
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
                    <SortableHeader
                      label="Imported at"
                      column="importedAt"
                      sortKey={controls.sortKey}
                      sortDirection={controls.sortDirection}
                      onSort={controls.toggleSort}
                    />
                  </th>
                  <th>Rows</th>
                  <th>
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {controls.items.map((batch) => (
                  <tr key={batch.id}>
                    <td>
                      <strong>{batch.fileName}</strong>
                      {batch.errorMessage && (
                        <small className="row-subtitle">{batch.errorMessage}</small>
                      )}
                      {batch.failures.length > 0 && (
                        <details className="row-details">
                          <summary>
                            {batch.failures.length} failed row{batch.failures.length > 1 ? "s" : ""}
                          </summary>
                          <ul className="failure-list">
                            {batch.failures.map((failure) => (
                              <li key={failure.id} className="failure-item">
                                <strong>Row {failure.rowNumber}</strong>
                                {failure.description && (
                                  <span>Description: {failure.description}</span>
                                )}
                                {failure.categoryName && (
                                  <span>Category: {failure.categoryName}</span>
                                )}
                                <span className="failure-reason">Reason: {failure.errorMessage}</span>
                              </li>
                            ))}
                          </ul>
                        </details>
                      )}
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          batch.status === "COMPLETED"
                            ? "income"
                            : batch.status === "FAILED"
                              ? "danger-icon"
                              : ""
                        }`}
                      >
                        {titleCase(batch.status)}
                      </span>
                    </td>
                    <td>{formatDate(batch.importedAt)}</td>
                    <td>
                      {batch.successRows} / {batch.totalRows}
                      {batch.failedRows > 0 && (
                        <small className="row-subtitle">{batch.failedRows} failed</small>
                      )}
                      {batch.skippedRows > 0 && (
                        <small className="row-subtitle">{batch.skippedRows} skipped</small>
                      )}
                    </td>
                    <td className="table-actions">
                      {batch.status === "PENDING" && (
                        <Button
                          variant="ghost"
                          disabled={upload.isPending}
                          onClick={() => pickFile(batch)}
                        >
                          {upload.isPending && uploadTarget?.id === batch.id ? (
                            "Importing…"
                          ) : (
                            <>
                              <Upload size={16} />
                              Upload CSV
                            </>
                          )}
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        aria-label={`Delete ${batch.fileName}`}
                        onClick={() => setDeleting(batch)}
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

      <input
        ref={fileInputRef}
        type="file"
        accept=".csv,text/csv"
        className="sr-only"
        onChange={onFileSelected}
      />

      <ImportBatchForm
        open={location.pathname.endsWith("/new")}
        busy={create.isPending}
        error={create.error}
        onClose={() => navigate("/imports")}
        onSubmit={(payload) => create.mutate(payload)}
      />

      <ConfirmDialog
        open={Boolean(deleting)}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Delete import batch?"
        description="This removes the batch record from your history."
        busy={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </>
  );
}

interface ImportBatchFormProps {
  open: boolean;
  busy: boolean;
  error: Error | null;
  onClose: () => void;
  onSubmit: (payload: ImportBatchPayload) => void;
}

/** Compact result summary shown right after a CSV import. */
function ImportSummary({ batch }: { batch: ImportBatch }) {
  const executionTime =
    batch.executionTimeMs == null ? null : `${Math.max(batch.executionTimeMs, 1)} ms`;

  return (
    <div className="card import-summary" role="status">
      <div className="import-summary-item success">
        <strong>{batch.successRows}</strong>
        <small>Imported</small>
      </div>
      <div className="import-summary-item danger">
        <strong>{batch.failedRows}</strong>
        <small>Failed</small>
      </div>
      <div className="import-summary-item">
        <strong>{batch.skippedRows}</strong>
        <small>Skipped (duplicates)</small>
      </div>
      {executionTime && (
        <div className="import-summary-item">
          <strong>{executionTime}</strong>
          <small>Execution time</small>
        </div>
      )}
    </div>
  );
}

function ImportBatchForm({ open, busy, error, onClose, onSubmit }: ImportBatchFormProps) {
  const form = useForm<ImportBatchFormValues>({
    resolver: zodResolver(importBatchSchema),
    defaultValues: { fileName: "" },
  });

  useEffect(() => {
    if (open) {
      form.reset({ fileName: "" });
    }
  }, [form, open]);

  return (
    <Modal
      open={open}
      onOpenChange={(value) => !value && onClose()}
      title="New import batch"
      description="Create a batch first, then upload the CSV file on the list."
    >
      <form className="form-grid modal-form" onSubmit={form.handleSubmit(onSubmit)}>
        <InputField
          id="importFileName"
          label="File name"
          placeholder="e.g. october-statement.csv"
          error={form.formState.errors.fileName?.message}
          {...form.register("fileName")}
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
            {busy ? "Saving…" : "Create batch"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
