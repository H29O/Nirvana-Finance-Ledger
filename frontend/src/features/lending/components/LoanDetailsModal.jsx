import React from 'react';
import Button from '../../../components/ui/Button';
import { formatCurrency, formatDate, formatDateTime } from '../../../lib/formatters';
import {
  X,
  Info,
  Calendar,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  ArrowDownLeft,
  FileText,
  User,
} from 'lucide-react';

export function LoanDetailsModal({
  isOpen,
  onClose,
  record,
  onRecordPayment,
}) {
  if (!isOpen || !record) return null;

  const isLent = record.type === 'LENT';
  const isSettled = record.status === 'SETTLED';

  const original = Number(record.originalAmount || 0);
  const remaining = Number(record.remainingAmount || 0);
  const paid = Math.max(0, original - remaining);
  const percentPaid = original > 0 ? Math.min(100, Math.round((paid / original) * 100)) : 0;

  const isOverdue =
    !isSettled &&
    record.dueDate &&
    new Date(record.dueDate) < new Date(new Date().setHours(0, 0, 0, 0));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-5 my-8">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div
              className={`w-10 h-10 rounded-xl flex items-center justify-center border ${
                isLent
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-100'
                  : 'bg-amber-50 text-amber-700 border-amber-100'
              }`}
            >
              {isLent ? <ArrowUpRight className="w-5 h-5" /> : <ArrowDownLeft className="w-5 h-5" />}
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Loan Details
              </h3>
              <p className="text-xs text-slate-500">
                Record #{record.id} • {isLent ? 'Money Lent (Receivable)' : 'Money Borrowed (Payable)'}
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

        {/* Status + Person Overview */}
        <div className="flex items-center justify-between p-4 bg-slate-50 border border-slate-200/70 rounded-2xl">
          <div className="flex items-center gap-2.5">
            <User className="w-5 h-5 text-slate-400" />
            <div>
              <span className="text-[11px] text-slate-400 font-medium block">
                {isLent ? 'Borrower (Owes You)' : 'Lender (You Owe)'}
              </span>
              <span className="text-sm font-bold text-slate-900">
                {record.personName}
              </span>
            </div>
          </div>

          {isSettled ? (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              Settled ✓
            </span>
          ) : (
            <span
              className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${
                isLent
                  ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                  : 'bg-amber-50 text-amber-700 border-amber-200'
              }`}
            >
              <Clock className="w-3.5 h-3.5" />
              Active
            </span>
          )}
        </div>

        {/* Financial Breakdown Table */}
        <div className="grid grid-cols-3 gap-3 p-4 bg-white border border-slate-200 rounded-2xl">
          <div>
            <span className="text-[11px] text-slate-400 block font-medium">Original Principal</span>
            <span className="font-bold text-slate-900 text-base font-tabular">
              {formatCurrency(original)}
            </span>
          </div>

          <div>
            <span className="text-[11px] text-slate-400 block font-medium">Total Repaid</span>
            <span className="font-bold text-emerald-600 text-base font-tabular">
              {formatCurrency(paid)}
            </span>
          </div>

          <div className="text-right">
            <span className="text-[11px] text-slate-400 block font-medium">Outstanding Balance</span>
            <span
              className={`font-bold text-base font-tabular ${
                isSettled
                  ? 'text-slate-400'
                  : isLent
                  ? 'text-indigo-700'
                  : 'text-rose-600'
              }`}
            >
              {formatCurrency(remaining)}
            </span>
          </div>
        </div>

        {/* Repayment Progress */}
        <div className="space-y-1.5 p-3.5 bg-slate-50/70 border border-slate-100 rounded-xl">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-600 font-medium">Repayment Progress</span>
            <span className="font-bold text-slate-800">{percentPaid}%</span>
          </div>
          <div className="w-full bg-slate-200/80 rounded-full h-2 overflow-hidden">
            <div
              className={`h-2 rounded-full transition-all duration-300 ${
                isSettled
                  ? 'bg-emerald-500'
                  : isLent
                  ? 'bg-indigo-600'
                  : 'bg-amber-500'
              }`}
              style={{ width: `${percentPaid}%` }}
            />
          </div>
        </div>

        {/* Dates & Timeline */}
        <div className="space-y-2 text-xs">
          {record.dueDate && (
            <div className="flex items-center justify-between py-1.5 px-3 bg-white border border-slate-100 rounded-xl">
              <span className="text-slate-500 flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-slate-400" />
                Due Date
              </span>
              <span
                className={`font-semibold ${
                  isOverdue ? 'text-rose-600 font-bold' : 'text-slate-800'
                }`}
              >
                {formatDate(record.dueDate)} {isOverdue && '(Overdue)'}
              </span>
            </div>
          )}

          <div className="flex items-center justify-between py-1.5 px-3 bg-white border border-slate-100 rounded-xl">
            <span className="text-slate-500">Created At</span>
            <span className="text-slate-700 font-medium">{formatDateTime(record.createdAt)}</span>
          </div>

          {record.updatedAt && (
            <div className="flex items-center justify-between py-1.5 px-3 bg-white border border-slate-100 rounded-xl">
              <span className="text-slate-500">Last Updated</span>
              <span className="text-slate-700 font-medium">{formatDateTime(record.updatedAt)}</span>
            </div>
          )}
        </div>

        {/* Notes */}
        {record.description && (
          <div className="space-y-1 p-3 bg-slate-50 border border-slate-100 rounded-xl text-xs">
            <span className="text-[11px] font-semibold text-slate-500 flex items-center gap-1">
              <FileText className="w-3.5 h-3.5" />
              Notes / Description
            </span>
            <p className="text-slate-700 whitespace-pre-wrap">{record.description}</p>
          </div>
        )}

        {/* Footer Actions */}
        <div className="flex items-center justify-between pt-3 border-t border-slate-100">
          <Button variant="ghost" size="sm" onClick={onClose}>
            Close
          </Button>

          {!isSettled && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                onClose();
                onRecordPayment(record);
              }}
            >
              Record Payment
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

export default LoanDetailsModal;
