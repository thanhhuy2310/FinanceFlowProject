import { useMemo, useState } from "react";

export function useListControls<T>(items: T[] | undefined, options: { search: (item: T) => string; sort: (left: T, right: T) => number; pageSize?: number }) {
  const [term, setTerm] = useState("");
  const [page, setPage] = useState(1);
  const pageSize = options.pageSize ?? 8;
  const filtered = useMemo(() => (items ?? []).filter((item) => options.search(item).toLowerCase().includes(term.toLowerCase())).sort(options.sort), [items, options, term]);
  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  return { term, setTerm: (value: string) => { setTerm(value); setPage(1); }, page: currentPage, setPage, pageCount, total: filtered.length, items: filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize) };
}
