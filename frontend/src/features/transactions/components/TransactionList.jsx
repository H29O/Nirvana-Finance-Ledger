import React, { useState } from 'react';
import Card from '../../../components/ui/Card';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import {
  ArrowUpRight,
  ArrowDownRight,
  Trash2,
  Edit2,
  CreditCard,
  Banknote,
} from 'lucide-react';

export function TransactionList({ transactions = [], onDeleteTransaction, onEditTransaction }) {
  const [deletingId, setDeletingId] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  const handleDelete = async (id) => {
    setDeletingId(id);
    try {
      await onDeleteTransaction(id);
    } finally {
      setDeletingId(null);
      setConfirmDeleteId(null);
    }
  };

  return (
    <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] overflow-hidden">
      {/* Desktop List Layout */}
      <div className="hidden md:block overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
              <th className="py-3 px-6">Transaction</th>
              <th className="py-3 px-4">Category</th>
              <th className="py-3 px-4">Method</th>
              <th className="py-3 px-4">Date</th>
              <th className="py-3 px-6 text-right">Amount</th>
              <th className="py-3 px-4 text-center w-16">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
            {transactions.map((tx) => {
              const isIncome = tx.transactionType === 'INCOME';
              const title =
                tx.merchant || tx.notes || tx.categoryName || 'Transaction';
              const isDeleting = deletingId === tx.id;
              const isConfirming = confirmDeleteId === tx.id;

              return (
                <tr
                  key={tx.id}
                  className="hover:bg-slate-50/70 transition-colors group"
                >
                  {/* Title & Icon */}
                  <td className="py-3.5 px-6">
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                          isIncome
                            ? 'bg-emerald-50 text-emerald-600'
                            : 'bg-rose-50 text-rose-600'
                        }`}
                      >
                        {isIncome ? (
                          <ArrowUpRight className="w-4 h-4" />
                        ) : (
                          <ArrowDownRight className="w-4 h-4" />
                        )}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-900 truncate">
                          {title}
                        </p>
                        {tx.notes && tx.merchant && (
                          <p className="text-[11px] text-slate-400 truncate max-w-xs">
                            {tx.notes}
                          </p>
                        )}
                      </div>
                    </div>
                  </td>

                  {/* Category */}
                  <td className="py-3.5 px-4">
                    {tx.categoryName ? (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-slate-100 text-slate-700">
                        {tx.categoryName}
                      </span>
                    ) : (
                      <span className="text-slate-400 text-[11px]">—</span>
                    )}
                  </td>

                  {/* Payment Method */}
                  <td className="py-3.5 px-4">
                    <span className="inline-flex items-center gap-1 text-[11px] font-medium text-slate-500">
                      {tx.paymentMethod === 'DIGITAL' ? (
                        <>
                          <CreditCard className="w-3.5 h-3.5 text-indigo-500" />
                          <span>Digital</span>
                        </>
                      ) : (
                        <>
                          <Banknote className="w-3.5 h-3.5 text-emerald-600" />
                          <span>Cash</span>
                        </>
                      )}
                    </span>
                  </td>

                  {/* Date */}
                  <td className="py-3.5 px-4 text-slate-500 font-medium text-xs">
                    {formatDate(tx.transactionDate)}
                  </td>

                  {/* Amount */}
                  <td className="py-3.5 px-6 text-right font-tabular">
                    <span
                      className={`text-base font-bold ${
                        isIncome ? 'text-emerald-600' : 'text-slate-900'
                      }`}
                    >
                      {isIncome ? '+' : '-'}
                      {formatCurrency(tx.amount)}
                    </span>
                  </td>

                  {/* Actions */}
                  <td className="py-3.5 px-4 text-center">
                    {isConfirming ? (
                      <div className="flex items-center justify-center gap-1">
                        <button
                          type="button"
                          onClick={() => handleDelete(tx.id)}
                          disabled={isDeleting}
                          className="px-2 py-1 bg-rose-600 text-white rounded text-[10px] font-semibold hover:bg-rose-700 transition-colors"
                        >
                          {isDeleting ? '...' : 'Yes'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmDeleteId(null)}
                          className="px-1.5 py-1 text-slate-500 hover:text-slate-700 text-[10px]"
                        >
                          Cancel
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center justify-center gap-1">
                        <button
                          type="button"
                          onClick={() => onEditTransaction && onEditTransaction(tx)}
                          title="Edit transaction"
                          aria-label="Edit transaction"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmDeleteId(tx.id)}
                          title="Delete transaction"
                          aria-label="Delete transaction"
                          className="p-1.5 rounded-lg text-slate-300 hover:text-rose-600 hover:bg-rose-50 transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Mobile Card List Layout */}
      <div className="md:hidden divide-y divide-slate-100">
        {transactions.map((tx) => {
          const isIncome = tx.transactionType === 'INCOME';
          const title =
            tx.merchant || tx.notes || tx.categoryName || 'Transaction';
          const isDeleting = deletingId === tx.id;
          const isConfirming = confirmDeleteId === tx.id;

          return (
            <div key={tx.id} className="p-4 space-y-2">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                      isIncome
                        ? 'bg-emerald-50 text-emerald-600'
                        : 'bg-rose-50 text-rose-600'
                    }`}
                  >
                    {isIncome ? (
                      <ArrowUpRight className="w-4 h-4" />
                    ) : (
                      <ArrowDownRight className="w-4 h-4" />
                    )}
                  </div>

                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-slate-900 truncate">
                      {title}
                    </p>
                    <div className="flex items-center gap-1.5 text-[11px] text-slate-400 mt-0.5">
                      <span>{formatDate(tx.transactionDate)}</span>
                      {tx.categoryName && (
                        <>
                          <span>•</span>
                          <span className="text-slate-600 font-medium truncate">
                            {tx.categoryName}
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <span
                    className={`text-base font-bold font-tabular ${
                      isIncome ? 'text-emerald-600' : 'text-slate-900'
                    }`}
                  >
                    {isIncome ? '+' : '-'}
                    {formatCurrency(tx.amount)}
                  </span>
                </div>
              </div>

              {/* Mobile actions */}
              <div className="flex items-center justify-end gap-3 pt-1">
                <button
                  type="button"
                  onClick={() => onEditTransaction && onEditTransaction(tx)}
                  className="inline-flex items-center gap-1 text-[11px] text-slate-500 hover:text-indigo-600 transition-colors"
                >
                  <Edit2 className="w-3.5 h-3.5" />
                  <span>Edit</span>
                </button>
                {isConfirming ? (
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] text-slate-500">Confirm?</span>
                    <button
                      type="button"
                      onClick={() => handleDelete(tx.id)}
                      disabled={isDeleting}
                      className="px-2 py-0.5 bg-rose-600 text-white rounded text-[10px] font-semibold"
                    >
                      {isDeleting ? 'Deleting...' : 'Delete'}
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmDeleteId(null)}
                      className="text-[10px] text-slate-500 px-1"
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmDeleteId(tx.id)}
                    className="inline-flex items-center gap-1 text-[11px] text-slate-400 hover:text-rose-600 transition-colors"
                  >
                    <Trash2 className="w-3 h-3" />
                    <span>Delete</span>
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </Card>
  );
}

export default TransactionList;
