import { ArrowDown, ArrowUp, ChevronsUpDown } from "lucide-react";

type SortDirection = "asc" | "desc";

interface SortableHeaderProps {
  label: string;
  column: string;
  sortKey: string | null;
  sortDirection: SortDirection;
  onSort: (key: string) => void;
}

export function SortableHeader({
  label,
  column,
  sortKey,
  sortDirection,
  onSort,
}: SortableHeaderProps) {
  const active = sortKey === column;

  return (
    <button
      type="button"
      className={`sort-header${active ? " active" : ""}`}
      aria-label={`Sort by ${label}`}
      onClick={() => onSort(column)}
    >
      {label}
      {active ? (
        sortDirection === "asc" ? (
          <ArrowUp size={13} />
        ) : (
          <ArrowDown size={13} />
        )
      ) : (
        <ChevronsUpDown size={13} />
      )}
    </button>
  );
}
