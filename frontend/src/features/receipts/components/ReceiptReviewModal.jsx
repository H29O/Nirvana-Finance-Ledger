import React, { useState, useEffect } from 'react';
import categoryService from '../../../services/category.service';
import transactionService from '../../../services/transaction.service';
import receiptService from '../../../services/receipt.service';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import {
  X,
  Sparkles,
  ArrowDownRight,
  ArrowUpRight,
  AlertCircle,
  FileText,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Tag,
} from 'lucide-react';

export function ReceiptReviewModal({
  isOpen,
  onClose,
  receipt,
  ocrData,
  onSuccess,
}) {
  const [categories, setCategories] = useState([]);
  const [fileBlobUrl, setFileBlobUrl] = useState(null);
  const [isPdf, setIsPdf] = useState(false);
  const [showRawText, setShowRawText] = useState(false);

  const [formData, setFormData] = useState({
    amount: '',
    merchant: '',
    transactionType: 'EXPENSE',
    paymentMethod: 'DIGITAL',
    categoryId: '',
    notes: '',
  });

  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Initialize form and fetch file blob & categories when modal opens
  useEffect(() => {
    if (isOpen && receipt) {
      // Prefill from OCR data if available
      setFormData({
        amount: ocrData?.detectedAmount != null ? String(ocrData.detectedAmount) : '',
        merchant: ocrData?.detectedMerchant || '',
        transactionType: 'EXPENSE',
        paymentMethod: 'DIGITAL',
        categoryId: ocrData?.suggestedCategoryId ? String(ocrData.suggestedCategoryId) : '',
        notes: `Scanned from receipt: ${receipt.originalFileName}`,
      });
      setErrors({});
      setGeneralError('');

      // Load categories
      categoryService
        .getAllCategories()
        .then((cats) => setCategories(cats || []))
        .catch(() => setCategories([]));

      // Load file preview blob
      receiptService
        .getReceiptBlob(receipt.id)
        .then(({ objectUrl, contentType }) => {
          setFileBlobUrl(objectUrl);
          setIsPdf(contentType === 'application/pdf');
        })
        .catch(() => {
          setFileBlobUrl(null);
        });
    }
  }, [isOpen, receipt, ocrData]);

  if (!isOpen || !receipt) return null;

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
    const numericAmount = parseFloat(formData.amount);

    if (!formData.amount || isNaN(numericAmount) || numericAmount <= 0) {
      newErrors.amount = 'Please enter a valid amount greater than ₹0';
    }

    if (!formData.merchant || formData.merchant.trim() === '') {
      newErrors.merchant = 'Merchant or description is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleCreateAndLink = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setGeneralError('');

    try {
      // 1. Create transaction with entrySource: 'RECEPT'
      const createdTx = await transactionService.createTransaction({
        amount: parseFloat(formData.amount),
        transactionType: formData.transactionType,
        paymentMethod: formData.paymentMethod,
        entrySource: 'RECEPT',
        merchant: formData.merchant.trim(),
        notes: formData.notes.trim() || null,
        categoryId: formData.categoryId ? Number(formData.categoryId) : null,
      });

      // 2. Automatically link receipt to new transaction
      if (createdTx?.id) {
        await receiptService.linkTransaction(receipt.id, createdTx.id);
      }

      onSuccess();
      onClose();
    } catch (err) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      }
      setGeneralError(
        err.normalizedMessage || 'Failed to create and link transaction. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const isExpense = formData.transactionType === 'EXPENSE';
  const isCategorySuggested =
    ocrData?.suggestedCategoryId &&
    String(ocrData.suggestedCategoryId) === formData.categoryId;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-3xl bg-white rounded-2xl shadow-2xl border border-slate-200 p-5 sm:p-6 z-10 space-y-5 my-8">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Review Extracted Receipt
              </h3>
              <p className="text-xs text-slate-500">
                Verify detected values and suggested category before saving to ledger
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

        {/* Error Notification */}
        {generalError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{generalError}</span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Left Column: Receipt Visual Preview */}
          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-700">
              <span>Receipt Preview</span>
              <span className="text-[11px] text-slate-400 font-normal">
                {receipt.originalFileName}
              </span>
            </div>

            <div className="h-64 sm:h-80 rounded-2xl border border-slate-200 bg-slate-50 overflow-hidden flex items-center justify-center p-2">
              {fileBlobUrl ? (
                isPdf ? (
                  <iframe
                    src={fileBlobUrl}
                    title="Receipt PDF"
                    className="w-full h-full rounded-xl"
                  />
                ) : (
                  <img
                    src={fileBlobUrl}
                    alt="Receipt preview"
                    className="max-h-full max-w-full object-contain rounded-xl"
                  />
                )
              ) : (
                <div className="text-center space-y-2 text-slate-400">
                  <FileText className="w-8 h-8 mx-auto" />
                  <p className="text-xs">Preview loading...</p>
                </div>
              )}
            </div>

            {/* Extracted Raw OCR Text Accordion */}
            {ocrData?.rawText && (
              <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-2.5 text-xs">
                <button
                  type="button"
                  onClick={() => setShowRawText(!showRawText)}
                  className="flex items-center justify-between w-full font-medium text-slate-700 hover:text-slate-900"
                >
                  <span className="flex items-center gap-1.5 text-[11px]">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    <span>View raw OCR text output</span>
                  </span>
                  {showRawText ? (
                    <ChevronUp className="w-3.5 h-3.5" />
                  ) : (
                    <ChevronDown className="w-3.5 h-3.5" />
                  )}
                </button>

                {showRawText && (
                  <pre className="mt-2 p-2 rounded-lg bg-white border border-slate-200 text-[10px] text-slate-600 font-mono overflow-x-auto max-h-32 whitespace-pre-wrap">
                    {ocrData.rawText}
                  </pre>
                )}
              </div>
            )}
          </div>

          {/* Right Column: Editable Confirmation Form */}
          <form onSubmit={handleCreateAndLink} className="space-y-4">
            {/* Type Switch */}
            <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-xl">
              <button
                type="button"
                onClick={() =>
                  setFormData((p) => ({ ...p, transactionType: 'EXPENSE' }))
                }
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
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
                onClick={() =>
                  setFormData((p) => ({ ...p, transactionType: 'INCOME' }))
                }
                className={`flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
                  !isExpense
                    ? 'bg-white text-emerald-600 shadow-sm'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                <ArrowUpRight className="w-3.5 h-3.5" />
                <span>Income</span>
              </button>
            </div>

            {/* Merchant / Description */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="text-xs font-medium text-slate-700">
                  Merchant / Description <span className="text-rose-500">*</span>
                </label>
                {ocrData?.detectedMerchant && (
                  <span className="text-[10px] font-semibold text-indigo-600 bg-indigo-50 border border-indigo-100 px-1.5 py-0.5 rounded">
                    Detected
                  </span>
                )}
              </div>
              <Input
                name="merchant"
                placeholder="e.g. Apollo Pharmacy, DMart, Starbucks"
                value={formData.merchant}
                onChange={handleChange}
                error={errors.merchant}
                required
              />
            </div>

            {/* Amount in INR */}
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="text-xs font-medium text-slate-700">
                  Total Amount <span className="text-rose-500">*</span>
                </label>
                {ocrData?.detectedAmount != null && (
                  <span className="text-[10px] font-semibold text-indigo-600 bg-indigo-50 border border-indigo-100 px-1.5 py-0.5 rounded">
                    Detected
                  </span>
                )}
              </div>
              <Input
                name="amount"
                type="number"
                step="0.01"
                placeholder="0.00"
                value={formData.amount}
                onChange={handleChange}
                error={errors.amount}
                leftIcon={<span className="text-sm font-bold text-slate-500">₹</span>}
                required
              />
            </div>

            {/* Category Dropdown with Suggestion Badge */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-medium text-slate-700 select-none flex items-center gap-1.5">
                  <Tag className="w-3.5 h-3.5 text-slate-400" />
                  <span>Category</span>
                </label>

                {isCategorySuggested ? (
                  <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded">
                    <Sparkles className="w-3 h-3" />
                    Suggested from receipt
                  </span>
                ) : (
                  <span className="text-[10px] text-slate-400 font-normal">
                    Select or change
                  </span>
                )}
              </div>

              <select
                name="categoryId"
                value={formData.categoryId}
                onChange={handleChange}
                className={`block w-full rounded-xl border bg-white text-sm text-slate-900 py-2.5 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 ${
                  isCategorySuggested
                    ? 'border-emerald-300 ring-1 ring-emerald-200/50'
                    : 'border-slate-200'
                }`}
              >
                <option value="">Choose a category (optional)</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Payment Method */}
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-slate-700 select-none">
                Payment Method
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() =>
                    setFormData((p) => ({ ...p, paymentMethod: 'DIGITAL' }))
                  }
                  className={`py-2 px-3 rounded-xl border text-xs font-semibold transition-all ${
                    formData.paymentMethod === 'DIGITAL'
                      ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                      : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  Digital (UPI / Card)
                </button>

                <button
                  type="button"
                  onClick={() =>
                    setFormData((p) => ({ ...p, paymentMethod: 'CASH' }))
                  }
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

            {/* Notes */}
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-slate-700 select-none">
                Notes
              </label>
              <textarea
                name="notes"
                rows={2}
                value={formData.notes}
                onChange={handleChange}
                className="block w-full rounded-xl border border-slate-200 bg-white text-sm text-slate-900 placeholder-slate-400 py-2 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            {/* Submit Action */}
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
                Create & Link Transaction
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default ReceiptReviewModal;
