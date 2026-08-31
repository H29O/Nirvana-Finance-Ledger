import React, { useState } from 'react';
import lendingService from '../../../services/lending.service';
import Button from '../../../components/ui/Button';
import { formatCurrency } from '../../../lib/formatters';
import { AlertTriangle, X } from 'lucide-react';

export function LoanDeleteModal({
  isOpen,
  onClose,
  record,
  onSuccess,
}) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen || !record) return null;

  const isLent = record.type === 'LENT';

  const handleDelete = async () => {
    setIsDeleting(true);
    setError('');

    try {
      await lendingService.deleteLoanRecord(record.id);
      onSuccess(`Deleted ${isLent ? 'lent' : 'borrowed'} record for ${record.personName}`);
      onClose();
    } catch (err) {
      setError(
        err.normalizedMessage || 'Failed to delete loan record. Please try again.'
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-5 my-8">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-rose-50 text-rose-600 border border-rose-100">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Delete Loan Record
              </h3>
              <p className="text-xs text-slate-500">
                This action cannot be undone
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

        {error && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700">
            {error}
          </div>
        )}

        <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1.5 text-xs text-slate-700">
          <p>
            Are you sure you want to delete this record for{' '}
            <strong className="text-slate-900">{record.personName}</strong>?
          </p>
          <div className="flex justify-between pt-2 border-t border-slate-200/60 font-medium">
            <span>Outstanding Balance:</span>
            <span className="font-bold text-slate-900">
              {formatCurrency(record.remainingAmount)}
            </span>
          </div>
        </div>

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
            type="button"
            variant="danger"
            size="sm"
            onClick={handleDelete}
            isLoading={isDeleting}
          >
            Delete Record
          </Button>
        </div>
      </div>
    </div>
  );
}

export default LoanDeleteModal;
