import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  description: string;
  actions?: ReactNode;
  /** Optional message rendered below the action buttons (e.g. filter errors). */
  actionRow?: ReactNode;
}

export function PageHeader({ title, description, actions, actionRow }: PageHeaderProps) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">FinanceFlow</p>
        <h1>{title}</h1>
        <p className="subtle">{description}</p>
      </div>
      {actions && (
        <div className="page-actions">
          {actions}
          {actionRow && <div className="page-action-row">{actionRow}</div>}
        </div>
      )}
    </header>
  );
}
