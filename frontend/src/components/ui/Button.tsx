import type { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & { children: ReactNode; variant?: "primary" | "secondary" | "danger" | "ghost" };
export function Button({ children, className = "", variant = "primary", type = "button", ...props }: ButtonProps) {
  return <button type={type} className={`button button-${variant} ${className}`.trim()} {...props}>{children}</button>;
}
