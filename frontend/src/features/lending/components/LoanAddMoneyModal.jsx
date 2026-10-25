import React, { useState, useEffect } from 'react';
import lendingService from '../../../services/lending.service';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { formatCurrency } from '../../../lib/formatters';
import {
  X,
  PlusCircle,
  AlertCircle,
  Calendar,
  FileText,
  ArrowUpRight,
  ArrowDownLeft,
  Coins,
  RefreshCw,
} from 'lucide-react';

const getTodayLocalDate = () => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export function LoanAddMoneyModal({
  isOpen,
  onClose,
  record,
  onSuccess,
}) {
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(getTodayLocalDate());
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen && record) {
      setAmount('');
      setDate(getTodayLocalDate());
      setNotes('');
      setError('');
      setGeneralError('');
    }
  }, [isOpen, record]);

  if (!isOpen || !record) return null;

  const isLent = record.type === 'LENT';
  const isSettled = record.status === 'SETTLED';

  const originalPrincipal = Number(record.originalAmount || 0);
  const additionalPrincipal = Number(record.additionalAmount || 0);
  const currentTotalPrincipal = Number(record.totalPrincipal || (originalPrincipal + additionalPrincipal));
  const currentRemaining = Number(record.remainingAmount || 0);
  const totalRepaid = Number(record.totalRepaid || Math.max(0, currentTotalPrincipal - currentRemaining));

  const parsedAmount = parseFloat(amount) || 0;
  const newTotalPrincipal = currentTotalPrincipal + parsedAmount;
  const newRemaining = currentRemaining + parsedAmount;

  const validate = () => {
    const numeric = parseFloat(amount);
    if (!amount || isNaN(numeric) || numeric <= 0) {
      setError('Please enter a valid amount greater than ₹0');
      return false;
    }
    if (!date) {
      setError('Please select an event date');
      return false;
    }
    setError('');
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setGeneralError('');

    try {
      const updated = await lendingService.addMoreMoney(record.id, {
        amount: parseFloat(amount),
        date: date || null,
        notes: notes ? notes.trim() : null,
      });

      onSuccess(
        `Added ${formatCurrency(parsedAmount)} to ${record.personName}. New outstanding balance: ${formatCurrency(updated.remainingAmount)}`
      );
      onClose();
    } catch (err) {
      setGeneralError(
        err.normalizedMessage || 'Failed to add money. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
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
              {isLent ? (
                <ArrowUpRight className="w-5 h-5 text-emerald-600" />
              ) : (
                <ArrowDownLeft className="w-5 h-5 text-amber-600" />
              )}
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {isLent ? 'Lend More Money' : 'Borrow More Money'}
              </h3>
              <p className="text-xs text-slate-500">
                {isLent ? 'Add to lent principal for ' : 'Add to loan borrowed from '}
                <span className="font-semibold text-slate-700">{record.personName}</span>
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
        {generalError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{generalError}</span>
          </div>
        )}

        {/* Settled Reactivation Notice */}
        {isSettled && (
          <div className="p-3 rounded-xl bg-indigo-50 border border-indigo-200 flex items-start gap-2 text-xs text-indigo-700">
            <RefreshCw className="w-4 h-4 shrink-0 mt-0.5 text-indigo-500" />
            <span>
              This record is currently <strong>Settled</strong>. Adding more money will safely reactivate it with the new outstanding balance while preserving full payment history.
            </span>
          </div>
        )}

        {/* Current State Summary Card */}
        <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-xl space-y-2 text-xs">
          <div className="flex items-center justify-between text-slate-500">
            <span>Opening Principal:</span>
            <span className="font-semibold text-slate-700 font-tabular">{formatCurrency(originalPrincipal)}</span>
          </div>
          {additionalPrincipal > 0 && (
            <div className="flex items-center justify-between text-slate-500">
              <span>Past Additional Principal:</span>
              <span className="font-semibold text-slate-700 font-tabular">+{formatCurrency(additionalPrincipal)}</span>
            </div>
          )}
          <div className="flex items-center justify-between text-slate-500">
            <span>Total Repaid to Date:</span>
            <span className="font-semibold text-emerald-600 font-tabular">{formatCurrency(totalRepaid)}</span>
          </div>
          <div className="flex items-center justify-between pt-1.5 border-t border-slate-200 text-slate-700">
            <span className="font-medium">Current Outstanding:</span>
            <span className="font-bold text-sm text-slate-900 font-tabular">{formatCurrency(currentRemaining)}</span>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {/* Amount Field */}
          <div className="space-y-1">
            <Input
              label={isLent ? 'Additional Amount Lent (₹)' : 'Additional Amount Borrowed (₹)'}
              name="amount"
              type="number"
              step="any"
              min="0.01"
              placeholder="0.00"
              value={amount}
              onChange={(e) => {
                setAmount(e.target.value);
                if (error) setError('');
                if (generalError) setGeneralError('');
              }}
              error={error}
              leftIcon={<span className="text-sm font-bold text-slate-500">₹</span>}
              autoFocus
              required
            />
          </div>

          {/* Date Picker */}
          <Input
            label="Transaction Date"
            name="date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            leftIcon={<Calendar className="w-4 h-4 text-slate-400" />}
            required
          />

          {/* Optional Notes */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-slate-400" />
              <span>Notes / Reason (Optional)</span>
            </label>
            <textarea
              name="notes"
              rows={2}
              placeholder="e.g. Additional loan for travel, split rent, shopping"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="block w-full rounded-xl border border-slate-200 bg-white text-sm text-slate-900 placeholder-slate-400 py-2 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          {/* Live Preview Box */}
          {parsedAmount > 0 && (
            <div className="p-3 bg-emerald-50/70 border border-emerald-200/80 rounded-xl space-y-1 text-xs">
              <div className="flex items-center justify-between text-emerald-800">
                <span className="font-medium">New Total Principal:</span>
                <span className="font-bold font-tabular">{formatCurrency(newTotalPrincipal)}</span>
              </div>
              <div className="flex items-center justify-between text-emerald-900 font-bold">
                <span>New Outstanding Balance:</span>
                <span className="text-sm font-tabular">{formatCurrency(newRemaining)}</span>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isSubmitting}
              leftIcon={<PlusCircle className="w-4 h-4" />}
            >
              {isLent ? 'Add Lent Amount' : 'Add Borrowed Amount'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default LoanAddMoneyModal;
