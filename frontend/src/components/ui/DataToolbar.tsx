import { Search, SlidersHorizontal } from "lucide-react";
import type { ChangeEvent, ReactNode } from "react";

interface DataToolbarProps {
  value: string;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
  placeholder: string;
  filters?: ReactNode;
}

export function DataToolbar({ value, onChange, placeholder, filters }: DataToolbarProps) {
  return (
    <div className="data-toolbar">
      <label className="search-field">
        <Search size={17} />
        <span className="sr-only">{placeholder}</span>
        <input className="input" value={value} onChange={onChange} placeholder={placeholder} />
      </label>

      {filters && (
        <div className="filter-group">
          <SlidersHorizontal className="subtle" size={17} />
          {filters}
        </div>
      )}
    </div>
  );
}
