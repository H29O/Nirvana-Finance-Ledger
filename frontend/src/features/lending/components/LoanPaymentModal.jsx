import React, { useState, useEffect } from 'react';
import lendingService from '../../../services/lending.service';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { formatCurrency } from '../../../lib/formatters';
import {
  X,
  CreditCard,
  AlertCircle,
  CheckCircle2,
  ArrowUpRight,
  ArrowDownLeft,
  Coins,
} from 'lucide-react';

export function LoanPaymentModal({
  isOpen,
  onClose,
  record,
  onSuccess,
}) {
  const [paymentAmount, setPaymentAmount] = useState('');
  const [error, setError] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen && record) {
      setPaymentAmount('');
      setError('');
      setGeneralError('');
    }
  }, [isOpen, record]);

  if (!isOpen || !record) return null;

  const isLent = record.type === 'LENT';
  const original = Number(record.originalAmount || 0);
  const remaining = Number(record.remainingAmount || 0);
  const paidSoFar = Math.max(0, original - remaining);

  const parsedPayment = parseFloat(paymentAmount) || 0;
  const newRemaining = Math.max(0, remaining - parsedPayment);
  const willBeSettled = parsedPayment > 0 && newRemaining === 0;

  const handleFillFull = () => {
    setPaymentAmount(String(remaining));
    setError('');
    setGeneralError('');
  };

  const validate = () => {
    const numeric = parseFloat(paymentAmount);
    if (!paymentAmount || isNaN(numeric) || numeric <= 0) {
      setError('Please enter a valid payment amount greater than ₹0');
      return false;
    }

    if (numeric > remaining) {
      setError(`Payment amount cannot exceed remaining balance of ${formatCurrency(remaining)}`);
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
      const updated = await lendingService.recordPayment(record.id, {
        amount: parseFloat(paymentAmount),
      });

      onSuccess(
        updated?.status === 'SETTLED'
          ? `Recorded payment of ${formatCurrency(parsedPayment)}. Loan is now fully settled ✓`
          : `Recorded payment of ${formatCurrency(parsedPayment)}. Remaining: ${formatCurrency(updated.remainingAmount)}`
      );
      onClose();
    } catch (err) {
      setGeneralError(
        err.normalizedMessage || 'Failed to record payment. Please try again.'
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
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Record Payment
              </h3>
              <p className="text-xs text-slate-500">
                {isLent ? 'Record repayment received from' : 'Record repayment made to'}{' '}
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

        {/* Current Balances Summary Card */}
        <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-xl space-y-2.5">
          <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-200/60">
            <span className="text-slate-500 font-medium">Record Type</span>
            <span
              className={`inline-flex items-center gap-1 font-semibold ${
                isLent ? 'text-emerald-700' : 'text-amber-700'
              }`}
            >
              {isLent ? <ArrowUpRight className="w-3.5 h-3.5" /> : <ArrowDownLeft className="w-3.5 h-3.5" />}
              {isLent ? 'Money Lent (Receivable)' : 'Money Borrowed (Payable)'}
            </span>
          </div>

          <div className="grid grid-cols-3 gap-2 text-xs">
            <div>
              <span className="text-[10px] text-slate-400 block font-medium">Original</span>
              <span className="font-semibold text-slate-700 font-tabular">
                {formatCurrency(original)}
              </span>
            </div>
            <div>
              <span className="text-[10px] text-slate-400 block font-medium">Paid So Far</span>
              <span className="font-semibold text-emerald-600 font-tabular">
                {formatCurrency(paidSoFar)}
              </span>
            </div>
            <div className="text-right">
              <span className="text-[10px] text-slate-400 block font-medium">Outstanding</span>
              <span className="font-bold text-slate-900 font-tabular text-sm">
                {formatCurrency(remaining)}
              </span>
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-medium text-slate-700 select-none">
                Payment Amount (₹) <span className="text-rose-500">*</span>
              </label>
              <button
                type="button"
                onClick={handleFillFull}
                className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800 hover:underline flex items-center gap-1"
              >
                <Coins className="w-3 h-3" />
                Pay Full ({formatCurrency(remaining)})
              </button>
            </div>

            <Input
              name="paymentAmount"
              type="number"
              step="0.01"
              max={remaining}
              placeholder={`Max: ${remaining}`}
              value={paymentAmount}
              onChange={(e) => {
                setPaymentAmount(e.target.value);
                if (error) setError('');
                if (generalError) setGeneralError('');
              }}
              error={error}
              leftIcon={<span className="text-sm font-bold text-slate-500">₹</span>}
              autoFocus
              required
            />
          </div>

          {/* Live Dynamic Preview */}
          {parsedPayment > 0 && parsedPayment <= remaining && (
            <div
              className={`p-3 rounded-xl border text-xs flex items-center justify-between transition-all ${
                willBeSettled
                  ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                  : 'bg-indigo-50/50 border-indigo-100 text-indigo-900'
              }`}
            >
              <span className="flex items-center gap-1.5 font-medium">
                {willBeSettled && <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />}
                {willBeSettled ? 'Will be completely settled' : 'Remaining balance after payment:'}
              </span>
              <span className="font-bold font-tabular text-sm">
                {willBeSettled ? 'Settled ✓' : formatCurrency(newRemaining)}
              </span>
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
            >
              Confirm Payment
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default LoanPaymentModal;
