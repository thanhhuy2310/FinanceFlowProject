import { AlertCircle, FolderOpen, LoaderCircle } from "lucide-react";
import { Button } from "./Button";

export function LoadingState({ label = "Loading your data" }: { label?: string }) {
  return <section className="state-card card" aria-live="polite"><LoaderCircle className="spin subtle" size={28}/><h2>{label}</h2><p className="subtle">Just a moment.</p></section>;
}

export function EmptyState({ title, description, action }: { title: string; description: string; action?: React.ReactNode }) {
  return <section className="state-card card"><FolderOpen className="subtle" size={30}/><h2>{title}</h2><p className="subtle">{description}</p>{action}</section>;
}

export function ErrorState({ title = "We could not load this page", retry }: { title?: string; retry?: () => void }) {
  return <section className="state-card card" role="alert"><AlertCircle className="danger-icon" size={30}/><h2>{title}</h2><p className="subtle">Check your connection and try again.</p>{retry && <Button variant="secondary" onClick={retry}>Try again</Button>}</section>;
}

export function TableSkeleton({ rows = 6 }: { rows?: number }) {
  return <div className="card table-skeleton" aria-label="Loading table"><div className="skeleton skeleton-line" />{Array.from({ length: rows }, (_, index) => <div className="skeleton skeleton-row" key={index} />)}</div>;
}
