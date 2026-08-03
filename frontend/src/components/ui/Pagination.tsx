import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./Button";

export function Pagination({ page, pageCount, onPageChange }: { page: number; pageCount: number; onPageChange: (page: number) => void }) {
  if (pageCount < 2) return null;
  return <nav className="pagination" aria-label="Pagination"><Button variant="secondary" aria-label="Previous page" disabled={page === 1} onClick={() => onPageChange(page - 1)}><ChevronLeft size={16}/></Button><span className="subtle">Page {page} of {pageCount}</span><Button variant="secondary" aria-label="Next page" disabled={page === pageCount} onClick={() => onPageChange(page + 1)}><ChevronRight size={16}/></Button></nav>;
}
