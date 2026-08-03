export function formatCurrency(value: number | string | null | undefined) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", maximumFractionDigits: 0 }).format(Number(value ?? 0));
}

export function formatDate(value: string | null | undefined, includeTime = false) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-US", includeTime ? { dateStyle: "medium", timeStyle: "short" } : { dateStyle: "medium" }).format(new Date(value));
}

export function titleCase(value: string) {
  return value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function toLocalDateTimeInput(value?: string) {
  const date = value ? new Date(value) : new Date();
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}
