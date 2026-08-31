import React, { useState } from 'react';
import receiptService from '../../../services/receipt.service';
import Button from '../../../components/ui/Button';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import {
  X,
  Link as LinkIcon,
  Sparkles,
  ArrowUpRight,
  ArrowDownRight,
  PlusCircle,
  CheckCircle2,
  Calendar,
  Tag,
  AlertCircle,
} from 'lucide-react';

export function ReceiptMatchChoiceModal({
  isOpen,
  onClose,
  receipt,
  ocrData,
  candidates = [],
  onLinkSuccess,
  onCreateNew,
}) {
  const [linkingTxId, setLinkingTxId] = useState(null);
  const [error, setError] = useState('');

  if (!isOpen || !receipt) return null;

  const handleLinkTransaction = async (txId) => {
    setLinkingTxId(txId);
    setError('');

    try {
      await receiptService.linkTransaction(receipt.id, txId);
      onLinkSuccess(`Receipt linked to existing transaction (#${txId})`);
      onClose();
    } catch (err) {
      setError(
        err.normalizedMessage ||
          'Failed to link receipt to this transaction. It may already have a receipt.'
      );
    } finally {
      setLinkingTxId(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-slate-200 p-5 sm:p-6 z-10 space-y-5 my-8">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Matching Transactions Found
              </h3>
              <p className="text-xs text-slate-500">
                We found existing transactions that match this receipt.
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

        {/* Error Banner */}
        {error && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Scanned Receipt Summary */}
        <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-xl space-y-1.5 text-xs">
          <span className="text-[11px] font-semibold text-slate-400 block uppercase tracking-wider">
            Uploaded Receipt Data
          </span>
          <div className="flex items-center justify-between font-medium">
            <span className="text-slate-800 font-bold">
              {ocrData?.detectedMerchant || receipt.originalFileName}
            </span>
            <span className="text-indigo-700 font-bold text-sm font-tabular">
              {ocrData?.detectedAmount != null
                ? formatCurrency(ocrData.detectedAmount)
                : '—'}
            </span>
          </div>
          {ocrData?.detectedDate && (
            <div className="text-[11px] text-slate-500 flex items-center gap-1">
              <Calendar className="w-3 h-3 text-slate-400" />
              <span>Receipt Date: {formatDate(ocrData.detectedDate)}</span>
            </div>
          )}
        </div>

        {/* Candidate List */}
        <div className="space-y-2.5">
          <span className="text-xs font-bold text-slate-700 block">
            Select an existing transaction to link:
          </span>

          <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
            {candidates.map(({ transaction: tx, score, reasons = [] }) => {
              const isIncome = tx.transactionType === 'INCOME';
              const isLinkingThis = linkingTxId === tx.id;

              return (
                <div
                  key={tx.id}
                  className="p-3 rounded-xl border border-slate-200/90 bg-white hover:border-indigo-300 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-[0_1px_2px_0_rgba(0,0,0,0.03)]"
                >
                  <div className="space-y-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900 text-sm">
                        {tx.merchant || 'Unlabeled Transaction'}
                      </span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 font-medium">
                        #{tx.id}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 text-xs text-slate-500">
                      <span className="font-tabular font-semibold text-slate-800">
                        {formatCurrency(tx.amount)}
                      </span>
                      <span>•</span>
                      <span>{formatDate(tx.transactionDate)}</span>
                      {tx.categoryName && (
                        <>
                          <span>•</span>
                          <span className="flex items-center gap-1">
                            <Tag className="w-3 h-3 text-slate-400" />
                            {tx.categoryName}
                          </span>
                        </>
                      )}
                    </div>

                    {/* Match Reasons */}
                    <div className="flex items-center gap-1.5 flex-wrap pt-0.5">
                      {reasons.map((reason, idx) => (
                        <span
                          key={idx}
                          className="text-[10px] px-1.5 py-0.2 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-100 font-medium"
                        >
                          ✓ {reason}
                        </span>
                      ))}
                    </div>
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleLinkTransaction(tx.id)}
                    isLoading={isLinkingThis}
                    disabled={linkingTxId !== null}
                    className="shrink-0 text-xs border-indigo-200 text-indigo-700 hover:bg-indigo-50 flex items-center gap-1"
                  >
                    <LinkIcon className="w-3.5 h-3.5" />
                    <span>Link This</span>
                  </Button>
                </div>
              );
            })}
          </div>
        </div>

        {/* Footer Actions: Option to create new transaction instead */}
        <div className="pt-3 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-2.5">
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="text-xs text-slate-500"
          >
            Cancel
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={() => {
              onClose();
              onCreateNew(receipt, ocrData);
            }}
            className="text-xs flex items-center gap-1.5 w-full sm:w-auto justify-center shadow-sm"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Create New Transaction Instead</span>
          </Button>
        </div>
      </div>
    </div>
  );
}

export default ReceiptMatchChoiceModal;
