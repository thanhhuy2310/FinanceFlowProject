import { AlertCircle, FolderOpen, LoaderCircle } from "lucide-react";
import type { ReactNode } from "react";

import { Button } from "./Button";

interface LoadingStateProps {
  label?: string;
}

export function LoadingState({ label = "Loading your data" }: LoadingStateProps) {
  return (
    <section className="state-card card" aria-live="polite">
      <LoaderCircle className="spin subtle" size={28} />
      <h2>{label}</h2>
      <p className="subtle">Just a moment.</p>
    </section>
  );
}

interface EmptyStateProps {
  title: string;
  description: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <section className="state-card card">
      <FolderOpen className="subtle" size={30} />
      <h2>{title}</h2>
      <p className="subtle">{description}</p>
      {action}
    </section>
  );
}

interface ErrorStateProps {
  title?: string;
  retry?: () => void;
}

export function ErrorState({ title = "We could not load this page", retry }: ErrorStateProps) {
  return (
    <section className="state-card card" role="alert">
      <AlertCircle className="danger-icon" size={30} />
      <h2>{title}</h2>
      <p className="subtle">Check your connection and try again.</p>
      {retry && (
        <Button variant="secondary" onClick={retry}>
          Try again
        </Button>
      )}
    </section>
  );
}

interface TableSkeletonProps {
  rows?: number;
}

export function TableSkeleton({ rows = 6 }: TableSkeletonProps) {
  return (
    <div className="card table-skeleton" aria-label="Loading table">
      <div className="skeleton skeleton-line" />
      {Array.from({ length: rows }, (_, index) => (
        <div className="skeleton skeleton-row" key={index} />
      ))}
    </div>
  );
}
