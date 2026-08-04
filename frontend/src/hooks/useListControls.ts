import { useMemo, useState } from "react";

export interface SortColumn<T> {
  key: string;
  compare: (left: T, right: T) => number;
  defaultDirection?: "asc" | "desc";
}

interface UseListControlsOptions<T> {
  search: (item: T) => string;
  sort: (left: T, right: T) => number;
  pageSize?: number;
  columns?: SortColumn<T>[];
}

const DEFAULT_PAGE_SIZE = 8;

export function useListControls<T>(items: T[] | undefined, options: UseListControlsOptions<T>) {
  // Search and paging state
  const [term, setTerm] = useState("");
  const [page, setPage] = useState(1);

  // Column sorting state
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");

  const pageSize = options.pageSize ?? DEFAULT_PAGE_SIZE;
  const activeColumn = options.columns?.find((column) => column.key === sortKey) ?? null;

  const toggleSort = (key: string) => {
    if (sortKey === key) {
      setSortDirection((direction) => (direction === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDirection(options.columns?.find((column) => column.key === key)?.defaultDirection ?? "asc");
    }

    setPage(1);
  };

  // Apply search filter and the active column comparator (or the default sort).
  const filtered = useMemo(() => {
    const compare = activeColumn
      ? (left: T, right: T) => {
          const result = activeColumn.compare(left, right);

          return sortDirection === "asc" ? result : -result;
        }
      : options.sort;

    return (items ?? [])
      .filter((item) => options.search(item).toLowerCase().includes(term.toLowerCase()))
      .sort(compare);
  }, [items, options, term, activeColumn, sortDirection]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pageCount);

  const updateTerm = (value: string) => {
    setTerm(value);
    setPage(1);
  };

  return {
    term,
    setTerm: updateTerm,
    page: currentPage,
    setPage,
    pageCount,
    total: filtered.length,
    items: filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize),
    sortKey,
    sortDirection,
    toggleSort,
  };
}
