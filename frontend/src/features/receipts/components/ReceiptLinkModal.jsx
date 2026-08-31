import React, { useState, useEffect } from 'react';
import transactionService from '../../../services/transaction.service';
import receiptService from '../../../services/receipt.service';
import Button from '../../../components/ui/Button';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import {
  X,
  Link as LinkIcon,
  Search,
  AlertCircle,
  ArrowUpRight,
  ArrowDownRight,
  Check,
} from 'lucide-react';

export function ReceiptLinkModal({ isOpen, onClose, receipt, onSuccess }) {
  const [transactions, setTransactions] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTxId, setSelectedTxId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLinking, setIsLinking] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setIsLoading(true);
      setSelectedTxId(null);
      setError('');
      transactionService
        .getTransactions({ page: 0, size: 30, sort: 'transactionDate,desc' })
        .then((data) => {
          setTransactions(data?.content || []);
        })
        .catch((err) => {
          setError(
            err.normalizedMessage || 'Failed to load transactions list.'
          );
        })
        .finally(() => setIsLoading(false));
    }
  }, [isOpen]);

  if (!isOpen || !receipt) return null;

  const filteredTransactions = transactions.filter((tx) => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    const merchant = (tx.merchant || '').toLowerCase();
    const category = (tx.categoryName || '').toLowerCase();
    const notes = (tx.notes || '').toLowerCase();
    return (
      merchant.includes(query) ||
      category.includes(query) ||
      notes.includes(query)
    );
  });

  const handleLink = async () => {
    if (!selectedTxId) return;

    setIsLinking(true);
    setError('');

    try {
      await receiptService.linkTransaction(receipt.id, selectedTxId);
      onSuccess();
      onClose();
    } catch (err) {
      setError(
        err.normalizedMessage ||
          'Failed to link receipt to transaction. Transaction may already have a receipt.'
      );
    } finally {
      setIsLinking(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <LinkIcon className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Link to Existing Transaction
              </h3>
              <p className="text-xs text-slate-500 truncate max-w-xs">
                Receipt: {receipt.originalFileName}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{error}</span>
          </div>
        )}

        {/* Search Filter Input */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by merchant, category, or note..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="block w-full pl-9 pr-3.5 py-2 rounded-xl border border-slate-200 bg-slate-50 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 focus:bg-white transition-all"
          />
        </div>

        {/* Transaction Selection List */}
        <div className="max-h-60 overflow-y-auto space-y-2 pr-1">
          {isLoading ? (
            <div className="py-8 text-center text-xs text-slate-400">
              Loading recent transactions...
            </div>
          ) : filteredTransactions.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-400">
              No matching transactions found.
            </div>
          ) : (
            filteredTransactions.map((tx) => {
              const isSelected = selectedTxId === tx.id;
              const isIncome = tx.transactionType === 'INCOME';
              const title =
                tx.merchant || tx.notes || tx.categoryName || 'Transaction';

              return (
                <div
                  key={tx.id}
                  onClick={() => setSelectedTxId(tx.id)}
                  className={`p-3 rounded-xl border flex items-center justify-between gap-3 cursor-pointer transition-all ${
                    isSelected
                      ? 'border-indigo-600 bg-indigo-50/60 shadow-sm'
                      : 'border-slate-200/80 bg-white hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                        isIncome
                          ? 'bg-emerald-100 text-emerald-700'
                          : 'bg-rose-100 text-rose-700'
                      }`}
                    >
                      {isIncome ? (
                        <ArrowUpRight className="w-3.5 h-3.5" />
                      ) : (
                        <ArrowDownRight className="w-3.5 h-3.5" />
                      )}
                    </div>

                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-900 truncate">
                        {title}
                      </p>
                      <div className="flex items-center gap-1.5 text-[11px] text-slate-400 mt-0.5">
                        <span>{formatDate(tx.transactionDate)}</span>
                        {tx.categoryName && (
                          <>
                            <span>•</span>
                            <span className="text-slate-600 truncate">
                              {tx.categoryName}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <span className="text-xs font-bold text-slate-900 font-tabular">
                      {isIncome ? '+' : '-'}
                      {formatCurrency(tx.amount)}
                    </span>

                    <div
                      className={`w-5 h-5 rounded-full flex items-center justify-center border transition-all ${
                        isSelected
                          ? 'border-indigo-600 bg-indigo-600 text-white'
                          : 'border-slate-300 bg-white'
                      }`}
                    >
                      {isSelected && <Check className="w-3 h-3" />}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
          <Button variant="ghost" size="sm" onClick={onClose} disabled={isLinking}>
            Cancel
          </Button>
          <Button
            variant="primary"
            size="sm"
            disabled={!selectedTxId}
            isLoading={isLinking}
            onClick={handleLink}
          >
            Confirm Link
          </Button>
        </div>
      </div>
    </div>
  );
}

export default ReceiptLinkModal;
