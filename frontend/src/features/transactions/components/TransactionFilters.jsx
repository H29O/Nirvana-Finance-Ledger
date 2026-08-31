import React from 'react';
import { Filter, RotateCcw, ArrowUpDown } from 'lucide-react';

export function TransactionFilters({
  type,
  onTypeChange,
  paymentMethod,
  onPaymentMethodChange,
  sort,
  onSortChange,
  onResetFilters,
  isFiltered,
}) {
  return (
    <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] flex flex-col md:flex-row md:items-center md:justify-between gap-3">
      {/* Left Filter Group */}
      <div className="flex flex-wrap items-center gap-2.5">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500 mr-1">
          <Filter className="w-3.5 h-3.5" />
          <span>Filter:</span>
        </div>

        {/* Transaction Type Filter */}
        <select
          value={type}
          onChange={(e) => onTypeChange(e.target.value)}
          aria-label="Filter by transaction type"
          className="text-xs bg-slate-50 border border-slate-200/90 text-slate-800 rounded-xl px-3 py-2 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
        >
          <option value="">All Types</option>
          <option value="EXPENSE">Expenses Only</option>
          <option value="INCOME">Income Only</option>
          <option value="LENT">Lent</option>
          <option value="BORROWED">Borrowed</option>
        </select>

        {/* Payment Method Filter */}
        <select
          value={paymentMethod}
          onChange={(e) => onPaymentMethodChange(e.target.value)}
          aria-label="Filter by payment method"
          className="text-xs bg-slate-50 border border-slate-200/90 text-slate-800 rounded-xl px-3 py-2 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
        >
          <option value="">All Methods</option>
          <option value="DIGITAL">Digital (UPI/Card/Bank)</option>
          <option value="CASH">Cash</option>
        </select>

        {/* Reset Action */}
        {isFiltered && (
          <button
            type="button"
            onClick={onResetFilters}
            className="inline-flex items-center gap-1 text-xs text-rose-600 hover:text-rose-700 font-semibold px-2 py-1 rounded-lg hover:bg-rose-50 transition-colors"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Reset</span>
          </button>
        )}
      </div>

      {/* Right Sort Dropdown */}
      <div className="flex items-center gap-2 self-start md:self-auto">
        <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500">
          <ArrowUpDown className="w-3.5 h-3.5" />
          <span>Sort:</span>
        </div>

        <select
          value={sort}
          onChange={(e) => onSortChange(e.target.value)}
          aria-label="Sort transactions"
          className="text-xs bg-slate-50 border border-slate-200/90 text-slate-800 rounded-xl px-3 py-2 font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
        >
          <option value="transactionDate,desc">Newest First</option>
          <option value="transactionDate,asc">Oldest First</option>
          <option value="amount,desc">Highest Amount</option>
          <option value="amount,asc">Lowest Amount</option>
        </select>
      </div>
    </div>
  );
}

export default TransactionFilters;
