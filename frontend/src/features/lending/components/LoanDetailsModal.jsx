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
  PlusCircle,
  Edit2,
  CreditCard,
  History,
} from 'lucide-react';

export function LoanDetailsModal({
  isOpen,
  onClose,
  record,
  onRecordPayment,
  onAddMoney,
  onEdit,
}) {
  if (!isOpen || !record) return null;

  const isLent = record.type === 'LENT';
  const isSettled = record.status === 'SETTLED';

  const original = Number(record.originalAmount || 0);
  const additional = Number(record.additionalAmount || 0);
  const totalPrincipal = Number(record.totalPrincipal ?? (original + additional));
  const remaining = Number(record.remainingAmount || 0);
  const paid = Number(record.totalRepaid ?? Math.max(0, totalPrincipal - remaining));
  const percentPaid = totalPrincipal > 0 ? Math.min(100, Math.round((paid / totalPrincipal) * 100)) : 0;

  const isOverdue =
    !isSettled &&
    record.dueDate &&
    new Date(record.dueDate) < new Date(new Date().setHours(0, 0, 0, 0));

  const events = record.events || [];

  const getEventBadge = (eventType) => {
    switch (eventType) {
      case 'INITIAL_PRINCIPAL':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">Opening</span>;
      case 'ADDITIONAL_PRINCIPAL':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">Added Principal</span>;
      case 'PAYMENT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">Repayment</span>;
      case 'PRINCIPAL_ADJUSTMENT':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">Adjustment</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600">{eventType}</span>;
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
      <div className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-5 my-8 max-h-[90vh] overflow-y-auto">
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
              {isLent ? <ArrowUpRight className="w-5 h-5 text-emerald-600" /> : <ArrowDownLeft className="w-5 h-5 text-amber-600" />}
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

        {/* Comprehensive Financial Breakdown */}
        <div className="p-4 bg-white border border-slate-200 rounded-2xl space-y-3">
          <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
            Financial Balance Summary
          </h4>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
            <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-[10px] text-slate-400 block font-medium">Opening Principal</span>
              <span className="font-bold text-slate-800 text-sm font-tabular block">
                {formatCurrency(original)}
              </span>
            </div>

            <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-[10px] text-slate-400 block font-medium">Additional Amounts</span>
              <span className="font-bold text-indigo-700 text-sm font-tabular block">
                +{formatCurrency(additional)}
              </span>
            </div>

            <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-[10px] text-slate-400 block font-medium">Total Principal</span>
              <span className="font-bold text-slate-900 text-sm font-tabular block">
                {formatCurrency(totalPrincipal)}
              </span>
            </div>

            <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-[10px] text-slate-400 block font-medium">Total Repaid</span>
              <span className="font-bold text-emerald-600 text-sm font-tabular block">
                {formatCurrency(paid)}
              </span>
            </div>
          </div>

          <div className="flex items-center justify-between p-3 bg-slate-50/80 rounded-xl border border-slate-200/80">
            <div>
              <span className="text-xs font-bold text-slate-800 block">Current Outstanding Balance</span>
              <span className="text-[11px] text-slate-400">
                {isSettled ? 'Account completely settled' : `${percentPaid}% repaid of total principal`}
              </span>
            </div>
            <span
              className={`font-extrabold text-lg font-tabular ${
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
            <span className="text-slate-600 font-medium">Settlement Progress</span>
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

        {/* Chronological Event History */}
        {events.length > 0 && (
          <div className="space-y-2.5">
            <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700 uppercase tracking-wider">
              <History className="w-3.5 h-3.5 text-slate-400" />
              <span>Financial Event History ({events.length})</span>
            </div>

            <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
              {events.map((evt) => (
                <div
                  key={evt.id}
                  className="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-100 rounded-xl text-xs"
                >
                  <div className="space-y-0.5 min-w-0 pr-2">
                    <div className="flex items-center gap-2">
                      {getEventBadge(evt.eventType)}
                      <span className="text-[11px] text-slate-400 font-medium">
                        {evt.eventDate ? formatDateTime(evt.eventDate) : formatDateTime(evt.createdAt)}
                      </span>
                    </div>
                    {evt.notes && (
                      <p className="text-[11px] text-slate-600 truncate">{evt.notes}</p>
                    )}
                  </div>

                  <span className="font-bold font-tabular text-xs text-slate-900 shrink-0">
                    {formatCurrency(evt.amount)}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

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

        {/* Actionable Footer with Clear Options */}
        <div className="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-slate-100">
          <Button variant="ghost" size="sm" onClick={onClose}>
            Close
          </Button>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                onClose();
                if (onEdit) onEdit(record);
              }}
              leftIcon={<Edit2 className="w-3.5 h-3.5" />}
            >
              Edit Details
            </Button>

            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                onClose();
                if (onAddMoney) onAddMoney(record);
              }}
              leftIcon={<PlusCircle className="w-3.5 h-3.5 text-emerald-600" />}
            >
              Add More Money
            </Button>

            {!isSettled && (
              <Button
                variant="primary"
                size="sm"
                onClick={() => {
                  onClose();
                  onRecordPayment(record);
                }}
                leftIcon={<CreditCard className="w-3.5 h-3.5" />}
              >
                Record Payment
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default LoanDetailsModal;
