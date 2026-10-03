import React, { useState, useRef } from 'react';
import receiptService from '../../../services/receipt.service';
import Button from '../../../components/ui/Button';
import {
  X,
  UploadCloud,
  Camera,
  FileText,
  Image as ImageIcon,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  RotateCcw,
} from 'lucide-react';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'application/pdf'];

export function ReceiptUploadModal({ isOpen, onClose, onUploadComplete }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [autoScanOcr, setAutoScanOcr] = useState(true);

  const [step, setStep] = useState('idle'); // 'idle' | 'uploading' | 'ocr' | 'completed' | 'error'
  const [errorMessage, setErrorMessage] = useState('');

  const fileInputRef = useRef(null);
  const cameraInputRef = useRef(null);

  if (!isOpen) return null;

  const handleFileChange = (file) => {
    if (!file) return;

    // Validate size
    if (file.size > MAX_FILE_SIZE) {
      setErrorMessage('File size exceeds the 10MB limit.');
      return;
    }

    // Validate type
    if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
      setErrorMessage('Only JPEG, PNG, and PDF files are supported.');
      return;
    }

    setErrorMessage('');
    setSelectedFile(file);

    if (file.type.startsWith('image/')) {
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
    } else {
      setPreviewUrl(null);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileChange(e.dataTransfer.files[0]);
    }
  };

  const handleUploadAndProcess = async () => {
    if (!selectedFile) return;

    setStep('uploading');
    setErrorMessage('');

    try {
      // Step 1: Upload to backend
      const receipt = await receiptService.uploadReceipt(selectedFile);

      let ocrData = null;
      if (autoScanOcr) {
        // Step 2: Run OCR
        setStep('ocr');
        try {
          ocrData = await receiptService.processOcr(receipt.id);
        } catch (ocrErr) {
          console.warn('OCR processing error on upload', ocrErr);
        }
      }

      setStep('completed');
      setTimeout(() => {
        onUploadComplete(receipt, ocrData);
        onClose();
      }, 600);
    } catch (err) {
      setStep('error');
      setErrorMessage(
        err.normalizedMessage || 'Failed to upload receipt. Please try again.'
      );
    }
  };

  const handleClose = () => {
    if (step === 'uploading' || step === 'ocr') return; // prevent closing mid-upload
    setSelectedFile(null);
    setPreviewUrl(null);
    setStep('idle');
    setErrorMessage('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={handleClose}
        aria-hidden="true"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              Scan & Upload Receipt
            </h3>
            <p className="text-xs text-slate-500">
              Add paper receipts or payment screenshots (JPEG, PNG, PDF up to 10MB)
            </p>
          </div>

          <button
            type="button"
            onClick={handleClose}
            aria-label="Close"
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Upload States / Dropzone */}
        {step === 'idle' || step === 'error' ? (
          <div className="space-y-4">
            {!selectedFile ? (
              <div
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                className="border-2 border-dashed border-slate-200 hover:border-indigo-400 rounded-2xl p-6 sm:p-8 text-center space-y-3 transition-colors bg-slate-50/50 hover:bg-indigo-50/20 cursor-pointer"
                onClick={() => fileInputRef.current?.click()}
              >
                <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center mx-auto text-indigo-600">
                  <UploadCloud className="w-6 h-6" />
                </div>

                <div className="space-y-1">
                  <p className="text-xs sm:text-sm font-semibold text-slate-800">
                    Click to upload or drag and drop
                  </p>
                  <p className="text-[11px] text-slate-400">
                    JPEG, PNG, or PDF bills up to 10MB
                  </p>
                </div>

                {/* Mobile Camera Direct Button */}
                <div className="pt-2">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      cameraInputRef.current?.click();
                    }}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-white border border-slate-200 text-slate-700 hover:bg-slate-100 shadow-sm transition-colors"
                  >
                    <Camera className="w-3.5 h-3.5 text-emerald-600" />
                    <span>Take Photo with Camera</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-10 h-10 rounded-xl bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0">
                      {selectedFile.type === 'application/pdf' ? (
                        <FileText className="w-5 h-5" />
                      ) : (
                        <ImageIcon className="w-5 h-5" />
                      )}
                    </div>

                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-900 truncate">
                        {selectedFile.name}
                      </p>
                      <p className="text-[11px] text-slate-400">
                        {(selectedFile.size / 1024).toFixed(0)} KB
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setSelectedFile(null);
                      setPreviewUrl(null);
                    }}
                    className="text-xs text-rose-600 hover:underline font-semibold"
                  >
                    Change
                  </button>
                </div>

                {/* Image thumbnail preview if available */}
                {previewUrl && (
                  <div className="max-h-48 overflow-hidden rounded-xl border border-slate-200 bg-white flex items-center justify-center p-1">
                    <img
                      src={previewUrl}
                      alt="Receipt preview"
                      className="max-h-44 object-contain rounded-lg"
                    />
                  </div>
                )}
              </div>
            )}

            {/* Auto OCR Checkbox */}
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-indigo-50/50 border border-indigo-100 text-xs">
              <input
                id="autoScan"
                type="checkbox"
                checked={autoScanOcr}
                onChange={(e) => setAutoScanOcr(e.target.checked)}
                className="rounded border-indigo-300 text-indigo-600 focus:ring-indigo-500 w-4 h-4 cursor-pointer"
              />
              <label
                htmlFor="autoScan"
                className="font-medium text-slate-700 cursor-pointer select-none flex items-center gap-1.5"
              >
                <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                <span>Automatically run OCR and extract details</span>
              </label>
            </div>
          </div>
        ) : (
          /* Processing Progress Display */
          <div className="py-8 text-center space-y-4">
            <div className="w-14 h-14 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center mx-auto text-indigo-600">
              {step === 'completed' ? (
                <CheckCircle2 className="w-7 h-7 text-emerald-600 animate-bounce" />
              ) : (
                <RotateCcw className="w-7 h-7 animate-spin text-indigo-600" />
              )}
            </div>

            <div className="space-y-1">
              <h4 className="text-sm font-bold text-slate-900">
                {step === 'uploading' && 'Uploading receipt to vault...'}
                {step === 'ocr' && 'Reading receipt and extracting details...'}
                {step === 'completed' && 'Receipt processed successfully!'}
              </h4>
              <p className="text-xs text-slate-400">
                {step === 'uploading' && 'Encrypting and saving file to storage'}
                {step === 'ocr' && 'Reading receipt and identifying merchant, total, and date'}
                {step === 'completed' && 'Ready for your review'}
              </p>
            </div>
          </div>
        )}

        {/* Hidden Inputs */}
        <input
          ref={fileInputRef}
          type="file"
          accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
          className="hidden"
          onChange={(e) => handleFileChange(e.target.files?.[0])}
        />
        <input
          ref={cameraInputRef}
          type="file"
          accept="image/*"
          capture="environment"
          className="hidden"
          onChange={(e) => handleFileChange(e.target.files?.[0])}
        />

        {/* Footer Actions */}
        {(step === 'idle' || step === 'error') && (
          <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
            <Button variant="ghost" size="sm" onClick={handleClose}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              disabled={!selectedFile}
              onClick={handleUploadAndProcess}
            >
              Upload & Extract
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}

export default ReceiptUploadModal;
