import type { InputHTMLAttributes, SelectHTMLAttributes } from "react";

type Base = { label: string; error?: unknown; id: string };
export function InputField({ label, error, id, ...props }: Base & Omit<InputHTMLAttributes<HTMLInputElement>, keyof Base>) {
  return <div className="field"><label htmlFor={id}>{label}</label><input id={id} className="input" {...props}/>{Boolean(error) && <span className="field-error">{String(error)}</span>}</div>;
}
export function SelectField({ label, error, id, children, ...props }: Base & Omit<SelectHTMLAttributes<HTMLSelectElement>, keyof Base>) {
  return <div className="field"><label htmlFor={id}>{label}</label><select id={id} className="input" {...props}>{children}</select>{Boolean(error) && <span className="field-error">{String(error)}</span>}</div>;
}
