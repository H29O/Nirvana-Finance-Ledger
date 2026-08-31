import React, { useState, useEffect } from 'react';
import lendingService from '../../../services/lending.service';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import {
  X,
  ArrowUpRight,
  ArrowDownLeft,
  AlertCircle,
  Calendar,
  User,
  FileText,
} from 'lucide-react';

export function LoanFormModal({
  isOpen,
  onClose,
  editingRecord,
  onSuccess,
}) {
  const isEditing = Boolean(editingRecord);

  const [formData, setFormData] = useState({
    type: 'LENT',
    personName: '',
    originalAmount: '',
    dueDate: '',
    description: '',
  });

  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      if (editingRecord) {
        setFormData({
          type: editingRecord.type || 'LENT',
          personName: editingRecord.personName || '',
          originalAmount: editingRecord.originalAmount ? String(editingRecord.originalAmount) : '',
          dueDate: editingRecord.dueDate ? editingRecord.dueDate.substring(0, 10) : '',
          description: editingRecord.description || '',
        });
      } else {
        setFormData({
          type: 'LENT',
          personName: '',
          originalAmount: '',
          dueDate: '',
          description: '',
        });
      }
      setErrors({});
      setGeneralError('');
    }
  }, [isOpen, editingRecord]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (generalError) setGeneralError('');
  };

  const validate = () => {
    const newErrors = {};

    if (!formData.personName || formData.personName.trim() === '') {
      newErrors.personName = 'Person name is required';
    }

    if (!isEditing) {
      const amountNum = parseFloat(formData.originalAmount);
      if (!formData.originalAmount || isNaN(amountNum) || amountNum <= 0) {
        newErrors.originalAmount = 'Please enter a valid amount greater than ₹0';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setGeneralError('');

    try {
      if (isEditing) {
        await lendingService.updateLoanRecord(editingRecord.id, {
          type: formData.type,
          personName: formData.personName,
          description: formData.description,
          dueDate: formData.dueDate || null,
        });
      } else {
        await lendingService.createLoanRecord({
          type: formData.type,
          personName: formData.personName,
          originalAmount: parseFloat(formData.originalAmount),
          description: formData.description,
          dueDate: formData.dueDate || null,
        });
      }

      onSuccess(
        isEditing
          ? 'Record updated successfully'
          : `Successfully recorded ${formData.type === 'LENT' ? 'lent' : 'borrowed'} money`
      );
      onClose();
    } catch (err) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      }
      setGeneralError(
        err.normalizedMessage ||
          `Failed to ${isEditing ? 'update' : 'create'} loan record. Please try again.`
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const isLent = formData.type === 'LENT';

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
          <div>
            <h3 className="text-base font-bold text-slate-900">
              {isEditing ? 'Edit Record' : 'Add Loan Record'}
            </h3>
            <p className="text-xs text-slate-500">
              {isEditing
                ? 'Update contact details, due date, or notes'
                : 'Track money you lent to someone or borrowed from someone'}
            </p>
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

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {/* Type Selector (Lent vs Borrowed) */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Record Type <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-xl">
              <button
                type="button"
                onClick={() => setFormData((p) => ({ ...p, type: 'LENT' }))}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-bold transition-all ${
                  isLent
                    ? 'bg-white text-emerald-700 shadow-sm border border-slate-200/50'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                <ArrowUpRight className="w-4 h-4 text-emerald-600" />
                <span>I Lent Money (Receivable)</span>
              </button>

              <button
                type="button"
                onClick={() => setFormData((p) => ({ ...p, type: 'BORROWED' }))}
                className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-bold transition-all ${
                  !isLent
                    ? 'bg-white text-amber-700 shadow-sm border border-slate-200/50'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                <ArrowDownLeft className="w-4 h-4 text-amber-600" />
                <span>I Borrowed (Payable)</span>
              </button>
            </div>
          </div>

          {/* Person Name */}
          <Input
            label="Person Name / Contact"
            name="personName"
            placeholder="e.g. Rahul Sharma, Priya, Colleague"
            value={formData.personName}
            onChange={handleChange}
            error={errors.personName}
            leftIcon={<User className="w-4 h-4 text-slate-400" />}
            required
          />

          {/* Original Amount (Only editable when creating) */}
          {isEditing ? (
            <div className="space-y-1">
              <label className="block text-xs font-medium text-slate-700 select-none">
                Original Amount (Fixed)
              </label>
              <div className="py-2.5 px-3.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-semibold text-slate-700">
                ₹{Number(formData.originalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </div>
              <p className="text-[11px] text-slate-400">
                To adjust balances, use the Record Payment action on the card.
              </p>
            </div>
          ) : (
            <Input
              label="Original Amount (₹)"
              name="originalAmount"
              type="number"
              step="any"
              min="0.01"
              placeholder="0.00"
              value={formData.originalAmount}
              onChange={handleChange}
              error={errors.originalAmount}
              leftIcon={<span className="text-sm font-bold text-slate-500">₹</span>}
              required
            />
          )}

          {/* Due Date (Optional) */}
          <Input
            label="Due Date (Optional)"
            name="dueDate"
            type="date"
            value={formData.dueDate}
            onChange={handleChange}
            error={errors.dueDate}
            leftIcon={<Calendar className="w-4 h-4 text-slate-400" />}
            helperText="Expected date to settle or receive this money"
          />

          {/* Description / Notes */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-slate-400" />
              <span>Notes / Reason (Optional)</span>
            </label>
            <textarea
              name="description"
              rows={2}
              placeholder="e.g. For dinner bill split, emergency medical loan, office laptop advance"
              value={formData.description}
              onChange={handleChange}
              className="block w-full rounded-xl border border-slate-200 bg-white text-sm text-slate-900 placeholder-slate-400 py-2 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

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
              {isEditing ? 'Save Changes' : 'Create Record'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default LoanFormModal;
