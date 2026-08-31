import React, { useState, useEffect } from 'react';
import Card from '../../../components/ui/Card';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import receiptService from '../../../services/receipt.service';
import {
  FileText,
  Image as ImageIcon,
  Link as LinkIcon,
  Unlink,
  Sparkles,
  Trash2,
  ExternalLink,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  PlusCircle,
} from 'lucide-react';

export function ReceiptCard({
  receipt,
  onReviewOcr,
  onLinkClick,
  onUnlinkClick,
  onDeleteClick,
}) {
  const [ocrData, setOcrData] = useState(null);
  const [isLoadingOcr, setIsLoadingOcr] = useState(false);
  const [isProcessingOcr, setIsProcessingOcr] = useState(false);

  const isPdf = receipt.contentType === 'application/pdf';
  const fileSizeKb = receipt.fileSize ? (receipt.fileSize / 1024).toFixed(0) + ' KB' : '';

  // Try to load existing OCR data on mount
  useEffect(() => {
    let isMounted = true;
    setIsLoadingOcr(true);
    receiptService
      .getOcrData(receipt.id)
      .then((data) => {
        if (isMounted) setOcrData(data);
      })
      .catch(() => {
        if (isMounted) setOcrData(null);
      })
      .finally(() => {
        if (isMounted) setIsLoadingOcr(false);
      });

    return () => {
      isMounted = false;
    };
  }, [receipt.id]);

  const handleRunOcr = async () => {
    setIsProcessingOcr(true);
    try {
      const data = await receiptService.processOcr(receipt.id);
      setOcrData(data);
      if (onReviewOcr) {
        onReviewOcr(receipt, data);
      }
    } catch (err) {
      setOcrData({ status: 'FAILED', failureReason: 'OCR execution failed' });
    } finally {
      setIsProcessingOcr(false);
    }
  };

  const handleOpenFile = async () => {
    try {
      const { objectUrl } = await receiptService.getReceiptBlob(receipt.id);
      window.open(objectUrl, '_blank');
    } catch (err) {
      console.error('Failed to open receipt file', err);
    }
  };

  const isLinked = Boolean(receipt.transactionId);
  const hasCompletedOcr = ocrData?.status === 'COMPLETED';

  return (
    <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] p-5 flex flex-col justify-between space-y-4 hover:border-slate-300 transition-all">
      {/* Top File Header */}
      <div className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
              {isPdf ? <FileText className="w-5 h-5" /> : <ImageIcon className="w-5 h-5" />}
            </div>

            <div className="min-w-0">
              <p
                className="text-sm font-semibold text-slate-900 truncate"
                title={receipt.originalFileName}
              >
                {receipt.originalFileName}
              </p>
              <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-0.5">
                <span>{formatDate(receipt.uploadedAt)}</span>
                {fileSizeKb && (
                  <>
                    <span>•</span>
                    <span>{fileSizeKb}</span>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Quick File View */}
          <button
            type="button"
            onClick={handleOpenFile}
            title="View full receipt"
            aria-label="View full receipt"
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors shrink-0"
          >
            <ExternalLink className="w-4 h-4" />
          </button>
        </div>

        {/* OCR Summary & Status Banner */}
        <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-slate-600 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
              <span>OCR Intelligence</span>
            </span>

            {isProcessingOcr ? (
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-indigo-600 animate-pulse">
                <RotateCcw className="w-3 h-3 animate-spin" />
                Reading receipt...
              </span>
            ) : hasCompletedOcr ? (
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-50 border border-emerald-200/80 px-2 py-0.5 rounded-full">
                <CheckCircle2 className="w-3 h-3" />
                Extracted
              </span>
            ) : ocrData?.status === 'FAILED' ? (
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-rose-700 bg-rose-50 border border-rose-200 px-2 py-0.5 rounded-full">
                <AlertCircle className="w-3 h-3" />
                Failed
              </span>
            ) : (
              <span className="text-[10px] font-medium text-slate-400">
                Not scanned yet
              </span>
            )}
          </div>

          {/* Extracted Details */}
          {hasCompletedOcr && (
            <div className="pt-1.5 border-t border-slate-200/60 flex items-center justify-between text-xs">
              <div>
                <span className="text-[11px] text-slate-400 block">Detected:</span>
                <span className="font-semibold text-slate-800 truncate max-w-[140px] block">
                  {ocrData.detectedMerchant || 'Merchant not found'}
                </span>
              </div>
              <div className="text-right">
                <span className="text-[11px] text-slate-400 block">Total:</span>
                <span className="font-bold text-slate-900 font-tabular text-sm">
                  {ocrData.detectedAmount != null
                    ? formatCurrency(ocrData.detectedAmount)
                    : '—'}
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Linked Transaction Status */}
        <div className="flex items-center justify-between text-xs pt-1">
          <span className="text-[11px] text-slate-400 font-medium">Ledger Link:</span>
          {isLinked ? (
            <div className="flex items-center gap-1.5">
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/70">
                <LinkIcon className="w-3 h-3" />
                ✓ Linked (Tx #{receipt.transactionId})
              </span>
              <button
                type="button"
                onClick={() => onUnlinkClick(receipt.id)}
                title="Unlink from transaction"
                className="p-1 text-slate-400 hover:text-rose-600 rounded"
              >
                <Unlink className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-medium text-amber-600 bg-amber-50 border border-amber-200/80 px-2 py-0.5 rounded-full">
                Not linked
              </span>
              <button
                type="button"
                onClick={() => onLinkClick(receipt)}
                className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-700 hover:underline flex items-center gap-1"
              >
                <LinkIcon className="w-3 h-3" />
                <span>Link</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Footer Actions */}
      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
        <button
          type="button"
          onClick={() => onDeleteClick(receipt)}
          title="Delete receipt"
          aria-label="Delete receipt"
          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
        </button>

        {/* Primary Actions */}
        <div className="flex items-center gap-1.5">
          {!isLinked && (
            <button
              type="button"
              onClick={() => onLinkClick(receipt)}
              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-semibold border border-slate-200 text-slate-600 hover:bg-slate-50 transition-colors"
            >
              <LinkIcon className="w-3 h-3" />
              <span>Link</span>
            </button>
          )}

          {hasCompletedOcr ? (
            <button
              type="button"
              onClick={() => onReviewOcr(receipt, ocrData)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-indigo-600 text-white hover:bg-indigo-700 transition-colors shadow-sm"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>{isLinked ? 'View OCR' : 'Create Transaction'}</span>
            </button>
          ) : (
            <button
              type="button"
              onClick={handleRunOcr}
              disabled={isProcessingOcr || isLoadingOcr}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors disabled:opacity-50"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>{isProcessingOcr ? 'Scanning...' : 'Scan OCR'}</span>
            </button>
          )}
        </div>
      </div>
    </Card>
  );
}

export default ReceiptCard;
