import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export function ReceiptPagination({
  currentPage = 0,
  totalPages = 1,
  totalElements = 0,
  onPageChange,
  isFirst = true,
  isLast = true,
}) {
  if (totalPages <= 1 && totalElements === 0) {
    return null;
  }

  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-2 text-xs text-slate-500">
      <div className="font-medium">
        Total <span className="font-bold text-slate-800 font-tabular">{totalElements}</span>{' '}
        {totalElements === 1 ? 'receipt' : 'receipts'} stored
      </div>

      <div className="flex items-center gap-2 self-center sm:self-auto">
        <span className="font-medium mr-2">
          Page <span className="font-bold text-slate-800 font-tabular">{currentPage + 1}</span> of{' '}
          <span className="font-bold text-slate-800 font-tabular">{Math.max(1, totalPages)}</span>
        </span>

        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={isFirst || currentPage === 0}
          aria-label="Previous Page"
          className="p-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-sm"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={isLast || currentPage >= totalPages - 1}
          aria-label="Next Page"
          className="p-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-sm"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}

export default ReceiptPagination;
