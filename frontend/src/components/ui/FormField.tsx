import type { InputHTMLAttributes, SelectHTMLAttributes } from "react";

interface BaseFieldProps {
  id: string;
  label: string;
  error?: string;
}

type InputFieldProps = BaseFieldProps & Omit<InputHTMLAttributes<HTMLInputElement>, keyof BaseFieldProps>;

export function InputField({ id, label, error, ...props }: InputFieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} className="input" {...props} />
      {error && <span className="field-error">{error}</span>}
    </div>
  );
}

type SelectFieldProps = BaseFieldProps &
  Omit<SelectHTMLAttributes<HTMLSelectElement>, keyof BaseFieldProps>;

export function SelectField({ id, label, error, children, ...props }: SelectFieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <select id={id} className="input" {...props}>
        {children}
      </select>
      {error && <span className="field-error">{error}</span>}
    </div>
  );
}
