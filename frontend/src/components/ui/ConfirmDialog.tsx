import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { Button } from "./Button";

type ConfirmDialogProps = { open: boolean; title: string; description: string; busy?: boolean; onOpenChange: (open: boolean) => void; onConfirm: () => void };
export function ConfirmDialog({ open, title, description, busy, onOpenChange, onConfirm }: ConfirmDialogProps) {
  return <Dialog.Root open={open} onOpenChange={onOpenChange}><Dialog.Portal><Dialog.Overlay className="dialog-overlay"/><Dialog.Content className="dialog-content"><div className="dialog-heading"><div><Dialog.Title>{title}</Dialog.Title><Dialog.Description>{description}</Dialog.Description></div><Dialog.Close className="icon-button" aria-label="Close dialog"><X size={18}/></Dialog.Close></div><div className="dialog-actions"><Button variant="secondary" onClick={() => onOpenChange(false)}>Cancel</Button><Button variant="danger" disabled={busy} onClick={onConfirm}>{busy ? "Deleting…" : "Delete"}</Button></div></Dialog.Content></Dialog.Portal></Dialog.Root>;
}
