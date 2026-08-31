import React from 'react';
import Card from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import {
  ArrowUpRight,
  ArrowDownLeft,
  CheckCircle2,
  Calendar,
  CreditCard,
  Edit2,
  Trash2,
  Info,
  Clock,
} from 'lucide-react';

export function LoanCard({
  record,
  onRecordPayment,
  onViewDetails,
  onEdit,
  onDelete,
}) {
  const isLent = record.type === 'LENT';
  const isSettled = record.status === 'SETTLED';

  const original = Number(record.originalAmount || 0);
  const remaining = Number(record.remainingAmount || 0);
  const paid = Math.max(0, original - remaining);

  const percentPaid = original > 0 ? Math.min(100, Math.round((paid / original) * 100)) : 0;

  // Check if overdue
  const isOverdue =
    !isSettled &&
    record.dueDate &&
    new Date(record.dueDate) < new Date(new Date().setHours(0, 0, 0, 0));

  return (
    <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] p-5 flex flex-col justify-between space-y-4 hover:border-slate-300 transition-all">
      {/* Header: Person & Type Tag */}
      <div className="space-y-2">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 border ${
                isLent
                  ? 'bg-emerald-50 text-emerald-700 border-emerald-100'
                  : 'bg-amber-50 text-amber-700 border-amber-100'
              }`}
            >
              {isLent ? (
                <ArrowUpRight className="w-5 h-5" />
              ) : (
                <ArrowDownLeft className="w-5 h-5" />
              )}
            </div>

            <div className="min-w-0">
              <h4
                className="text-sm font-bold text-slate-900 truncate"
                title={record.personName}
              >
                {record.personName}
              </h4>
              <p className="text-[11px] text-slate-500 font-medium">
                {isLent ? 'Lent (Receivable)' : 'Borrowed (Payable)'}
              </p>
            </div>
          </div>

          {/* Status Badge */}
          {isSettled ? (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/70 shrink-0">
              <CheckCircle2 className="w-3 h-3" />
              Settled ✓
            </span>
          ) : (
            <span
              className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold shrink-0 border ${
                isLent
                  ? 'bg-indigo-50 text-indigo-700 border-indigo-200/70'
                  : 'bg-amber-50 text-amber-700 border-amber-200/70'
              }`}
            >
              Active
            </span>
          )}
        </div>

        {/* Description Note if available */}
        {record.description && (
          <p className="text-xs text-slate-600 line-clamp-1 bg-slate-50/70 px-2.5 py-1 rounded-lg border border-slate-100">
            {record.description}
          </p>
        )}
      </div>

      {/* Financial Amounts Breakdown */}
      <div className="space-y-2 pt-2 border-t border-slate-100">
        <div className="grid grid-cols-3 gap-2 text-xs">
          <div>
            <span className="text-[10px] text-slate-400 block font-medium">Original</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {formatCurrency(original)}
            </span>
          </div>

          <div>
            <span className="text-[10px] text-slate-400 block font-medium">Paid</span>
            <span className="font-semibold text-emerald-600 font-tabular">
              {formatCurrency(paid)}
            </span>
          </div>

          <div className="text-right">
            <span className="text-[10px] text-slate-400 block font-medium">Outstanding</span>
            <span
              className={`font-bold font-tabular text-sm ${
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

        {/* Progress Bar */}
        <div className="space-y-1">
          <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
            <div
              className={`h-1.5 rounded-full transition-all duration-300 ${
                isSettled
                  ? 'bg-emerald-500'
                  : isLent
                  ? 'bg-indigo-500'
                  : 'bg-amber-500'
              }`}
              style={{ width: `${percentPaid}%` }}
            />
          </div>
          <div className="flex items-center justify-between text-[10px] text-slate-400">
            <span>{percentPaid}% repaid</span>
            {record.dueDate && (
              <span
                className={`flex items-center gap-1 ${
                  isOverdue ? 'text-rose-600 font-semibold' : 'text-slate-500'
                }`}
              >
                <Calendar className="w-3 h-3" />
                {isOverdue ? 'Overdue: ' : 'Due: '}
                {formatDate(record.dueDate)}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Footer Actions */}
      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-1.5">
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => onViewDetails(record)}
            title="View details"
            aria-label="View details"
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <Info className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={() => onEdit(record)}
            title="Edit record"
            aria-label="Edit record"
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <Edit2 className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={() => onDelete(record)}
            title="Delete record"
            aria-label="Delete record"
            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>

        {/* Primary Action: Record Payment */}
        {!isSettled ? (
          <Button
            variant="outline"
            size="sm"
            onClick={() => onRecordPayment(record)}
            className="text-xs h-8 px-3 rounded-xl border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/50 text-indigo-700 font-semibold flex items-center gap-1.5"
          >
            <CreditCard className="w-3.5 h-3.5" />
            <span>Record Payment</span>
          </Button>
        ) : (
          <span className="text-xs text-slate-400 font-medium px-2 py-1">
            Completed
          </span>
        )}
      </div>
    </Card>
  );
}

export default LoanCard;
