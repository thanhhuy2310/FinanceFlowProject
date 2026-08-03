import type { ReactNode } from "react";

export function PageHeader({ title, description, actions }: { title: string; description: string; actions?: ReactNode }) {
  return <header className="page-header"><div><p className="eyebrow">FinanceFlow</p><h1>{title}</h1><p className="subtle">{description}</p></div>{actions && <div className="page-actions">{actions}</div>}</header>;
}
