import React, { useState } from 'react';
import Button from '../../../components/ui/Button';
import { Trash2, X, AlertTriangle, AlertCircle } from 'lucide-react';

export function ReceiptDeleteModal({
  isOpen,
  onClose,
  receipt,
  onConfirmDelete,
  onConfirm,
}) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !receipt) return null;

  const handleDelete = async () => {
    setIsDeleting(true);
    setError('');
    try {
      const deleteFn = onConfirmDelete || onConfirm;
      if (typeof deleteFn === 'function') {
        await deleteFn(receipt.id);
      }
      onClose();
    } catch (err) {
      setError(
        err.normalizedMessage ||
          'Failed to delete receipt from vault. Please try again.'
      );
    } finally {
      setIsDeleting(false);
    }
  };

  const isLinked = Boolean(receipt.transactionId);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={isDeleting ? undefined : onClose}
        aria-hidden="true"
      />

      {/* Modal Card */}
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-4">
        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600 shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Delete Receipt
              </h3>
              <p className="text-xs text-slate-500">
                This action will remove the file from your vault
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            aria-label="Close"
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors disabled:opacity-40"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error notification */}
        {error && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{error}</span>
          </div>
        )}

        {/* Content */}
        <div className="space-y-2 text-xs text-slate-600">
          <p>
            Are you sure you want to delete{' '}
            <strong className="text-slate-900">{receipt.originalFileName}</strong>?
          </p>

          {isLinked ? (
            <p className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 text-[11px] text-slate-500 leading-relaxed">
              <strong className="text-slate-700 block mb-0.5">Linked Ledger Entry:</strong>
              This receipt is currently linked to transaction #{receipt.transactionId}. Deleting the receipt will remove the file and OCR record, but your financial transaction in the ledger will remain preserved.
            </p>
          ) : (
            <p className="text-[11px] text-slate-400">
              The stored receipt file and any extracted OCR text will be permanently deleted.
            </p>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            disabled={isDeleting}
          >
            Cancel
          </Button>

          <Button
            variant="danger"
            size="sm"
            isLoading={isDeleting}
            onClick={handleDelete}
            leftIcon={<Trash2 className="w-4 h-4" />}
          >
            Delete Receipt
          </Button>
        </div>
      </div>
    </div>
  );
}

export default ReceiptDeleteModal;
