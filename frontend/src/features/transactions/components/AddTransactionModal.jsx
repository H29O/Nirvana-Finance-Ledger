import React, { useState, useEffect } from 'react';
import categoryService from '../../../services/category.service';
import transactionService from '../../../services/transaction.service';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { X, ArrowUpRight, ArrowDownRight, AlertCircle } from 'lucide-react';

export function AddTransactionModal({ isOpen, onClose, onSuccess }) {
  const [categories, setCategories] = useState([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(false);

  const [formData, setFormData] = useState({
    amount: '',
    transactionType: 'EXPENSE',
    paymentMethod: 'DIGITAL',
    entrySource: 'MANUAL',
    merchant: '',
    notes: '',
    categoryId: '',
  });

  const [formErrors, setFormErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load categories when modal opens
  useEffect(() => {
    if (isOpen) {
      setIsLoadingCategories(true);
      categoryService
        .getAllCategories()
        .then((cats) => setCategories(cats || []))
        .catch(() => setCategories([]))
        .finally(() => setIsLoadingCategories(false));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (generalError) setGeneralError('');
  };

  const validate = () => {
    const errors = {};
    const numericAmount = parseFloat(formData.amount);

    if (!formData.amount || isNaN(numericAmount) || numericAmount <= 0) {
      errors.amount = 'Please enter a valid amount greater than ₹0';
    }

    if (!formData.merchant || formData.merchant.trim() === '') {
      errors.merchant = 'Merchant or description is required';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setGeneralError('');

    try {
      await transactionService.createTransaction({
        amount: parseFloat(formData.amount),
        transactionType: formData.transactionType,
        paymentMethod: formData.paymentMethod,
        entrySource: 'MANUAL',
        merchant: formData.merchant.trim(),
        notes: formData.notes.trim() || null,
        categoryId: formData.categoryId ? Number(formData.categoryId) : null,
      });

      // Reset form and notify parent
      setFormData({
        amount: '',
        transactionType: 'EXPENSE',
        paymentMethod: 'DIGITAL',
        entrySource: 'MANUAL',
        merchant: '',
        notes: '',
        categoryId: '',
      });
      onSuccess();
      onClose();
    } catch (err) {
      if (err.validationErrors) {
        setFormErrors(err.validationErrors);
      }
      setGeneralError(err.normalizedMessage || 'Failed to record transaction. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const isExpense = formData.transactionType === 'EXPENSE';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div>
            <h3 className="text-base font-bold text-slate-900">Add Transaction</h3>
            <p className="text-xs text-slate-500">Record a new cash or digital entry</p>
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

        {/* Error Notification */}
        {generalError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{generalError}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Type Toggle: Expense vs Income */}
          <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-xl">
            <button
              type="button"
              onClick={() => setFormData((p) => ({ ...p, transactionType: 'EXPENSE' }))}
              className={`flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-bold transition-all ${
                isExpense
                  ? 'bg-white text-rose-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <ArrowDownRight className="w-3.5 h-3.5" />
              <span>Expense</span>
            </button>

            <button
              type="button"
              onClick={() => setFormData((p) => ({ ...p, transactionType: 'INCOME' }))}
              className={`flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-bold transition-all ${
                !isExpense
                  ? 'bg-white text-emerald-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <ArrowUpRight className="w-3.5 h-3.5" />
              <span>Income</span>
            </button>
          </div>

          {/* Amount Field */}
          <Input
            label="Amount (₹)"
            name="amount"
            type="number"
            step="0.01"
            placeholder="0.00"
            value={formData.amount}
            onChange={handleChange}
            error={formErrors.amount}
            leftIcon={<span className="text-sm font-bold text-slate-500">₹</span>}
            required
          />

          {/* Merchant / Description */}
          <Input
            label={isExpense ? 'Merchant / Description' : 'Source / Description'}
            name="merchant"
            placeholder={isExpense ? 'e.g. Swiggy, Groceries, Uber' : 'e.g. Salary, Client Payout'}
            value={formData.merchant}
            onChange={handleChange}
            error={formErrors.merchant}
            required
          />

          {/* Category Dropdown */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Category
            </label>
            <select
              name="categoryId"
              value={formData.categoryId}
              onChange={handleChange}
              disabled={isLoadingCategories}
              className="block w-full rounded-xl border border-slate-200 bg-white text-sm text-slate-900 py-2.5 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            >
              <option value="">Select a category (optional)</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          {/* Payment Method Toggle */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Payment Method
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setFormData((p) => ({ ...p, paymentMethod: 'DIGITAL' }))}
                className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all ${
                  formData.paymentMethod === 'DIGITAL'
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                    : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                }`}
              >
                Digital (UPI / Card / NetBanking)
              </button>

              <button
                type="button"
                onClick={() => setFormData((p) => ({ ...p, paymentMethod: 'CASH' }))}
                className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all ${
                  formData.paymentMethod === 'CASH'
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                    : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                }`}
              >
                Cash
              </button>
            </div>
          </div>

          {/* Optional Notes */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Notes (Optional)
            </label>
            <textarea
              name="notes"
              rows={2}
              placeholder="Additional details or context..."
              value={formData.notes}
              onChange={handleChange}
              className="block w-full rounded-xl border border-slate-200 bg-white text-sm text-slate-900 placeholder-slate-400 py-2 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          {/* Submit Buttons */}
          <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
            <Button variant="ghost" size="sm" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant={isExpense ? 'danger' : 'income'}
              size="sm"
              isLoading={isSubmitting}
            >
              {isExpense ? 'Add Expense' : 'Add Income'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddTransactionModal;
