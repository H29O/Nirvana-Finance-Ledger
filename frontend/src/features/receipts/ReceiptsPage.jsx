import React, { useState, useEffect, useCallback } from 'react';
import receiptService from '../../services/receipt.service';
import transactionService from '../../services/transaction.service';
import { matchReceiptWithTransactions } from './utils/receiptMatcher';
import { formatCurrency } from '../../lib/formatters';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import ReceiptCard from './components/ReceiptCard';
import ReceiptPagination from './components/ReceiptPagination';
import ReceiptsSkeleton from './components/ReceiptsSkeleton';
import ReceiptsEmptyState from './components/ReceiptsEmptyState';
import ReceiptUploadModal from './components/ReceiptUploadModal';
import ReceiptReviewModal from './components/ReceiptReviewModal';
import ReceiptLinkModal from './components/ReceiptLinkModal';
import ReceiptMatchChoiceModal from './components/ReceiptMatchChoiceModal';
import ReceiptDeleteModal from './components/ReceiptDeleteModal';
import { UploadCloud, RotateCcw, AlertCircle, CheckCircle2 } from 'lucide-react';

export function ReceiptsPage() {
  const [receipts, setReceipts] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    number: 0,
    totalPages: 1,
    totalElements: 0,
    first: true,
    last: true,
  });

  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);

  // Modals state
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [reviewReceipt, setReviewReceipt] = useState(null);
  const [reviewOcrData, setReviewOcrData] = useState(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);

  const [matchChoiceReceipt, setMatchChoiceReceipt] = useState(null);
  const [matchChoiceOcrData, setMatchChoiceOcrData] = useState(null);
  const [matchCandidates, setMatchCandidates] = useState([]);
  const [isMatchChoiceOpen, setIsMatchChoiceOpen] = useState(false);

  const [linkReceipt, setLinkReceipt] = useState(null);
  const [isLinkOpen, setIsLinkOpen] = useState(false);
  const [deleteReceipt, setDeleteReceipt] = useState(null);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const loadReceipts = useCallback(
    async (isSilent = false) => {
      if (!isSilent) setIsLoading(true);
      else setIsRefreshing(true);
      setError('');

      try {
        const data = await receiptService.getReceipts({
          page: currentPage,
          size: 9,
          sort: 'uploadedAt,desc',
        });

        setReceipts(data?.content || []);
        setRefreshKey((k) => k + 1);
        setPageInfo({
          number: data?.number ?? 0,
          totalPages: data?.totalPages ?? 1,
          totalElements: data?.totalElements ?? 0,
          first: data?.first ?? true,
          last: data?.last ?? true,
        });
      } catch (err) {
        setError(
          err.normalizedMessage || 'Failed to load receipts. Please try again.'
        );
      } finally {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    },
    [currentPage]
  );

  useEffect(() => {
    loadReceipts();
  }, [loadReceipts]);

  // Auto-dismiss success message after 4s
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => {
        setSuccessMessage('');
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  // Intelligent Receipt Post-Upload & Match Evaluation Flow
  const handleUploadComplete = async (newReceipt, ocrData) => {
    loadReceipts(true);
    if (!newReceipt) return;

    // If OCR didn't run or failed or detected no amount, fallback to manual review
    if (!ocrData || ocrData.status !== 'COMPLETED' || !ocrData.detectedAmount || Number(ocrData.detectedAmount) <= 0) {
      setReviewReceipt(newReceipt);
      setReviewOcrData(ocrData);
      setIsReviewOpen(true);
      return;
    }

    try {
      // Query recent transactions belonging to the user
      const txPage = await transactionService.getTransactions({
        page: 0,
        size: 100,
        sort: 'transactionDate,desc',
      });
      const transactions = txPage?.content || [];

      const matchResult = matchReceiptWithTransactions(ocrData, transactions);

      if (matchResult.status === 'STRONG_MATCH' && matchResult.matchedTransaction) {
        // Case 1: Single Strong Match -> Automatically Link
        const matchedTx = matchResult.matchedTransaction;
        try {
          await receiptService.linkTransaction(newReceipt.id, matchedTx.id);
          setSuccessMessage(
            `Receipt automatically linked to existing transaction (${formatCurrency(matchedTx.amount)} • ${matchedTx.merchant || 'Expense'})`
          );
          loadReceipts(true);
        } catch (linkErr) {
          // If transaction already has a receipt or link failed, open review
          console.warn('Auto-link failed, opening review modal', linkErr);
          setReviewReceipt(newReceipt);
          setReviewOcrData(ocrData);
          setIsReviewOpen(true);
        }
      } else if (matchResult.status === 'MULTIPLE_MATCHES' && matchResult.candidates?.length > 0) {
        // Case 3: Multiple Possible Matches -> Present Choice Modal
        setMatchChoiceReceipt(newReceipt);
        setMatchChoiceOcrData(ocrData);
        setMatchCandidates(matchResult.candidates);
        setIsMatchChoiceOpen(true);
      } else {
        // Case 2: No Matching Transaction -> Open Review & Create Modal
        setReviewReceipt(newReceipt);
        setReviewOcrData(ocrData);
        setIsReviewOpen(true);
      }
    } catch (err) {
      console.warn('Error during transaction match evaluation', err);
      setReviewReceipt(newReceipt);
      setReviewOcrData(ocrData);
      setIsReviewOpen(true);
    }
  };

  const handleReviewOcr = (receipt, ocrData) => {
    setReviewReceipt(receipt);
    setReviewOcrData(ocrData);
    setIsReviewOpen(true);
  };

  const handleOpenLinkModal = (receipt) => {
    setLinkReceipt(receipt);
    setIsLinkOpen(true);
  };

  const handleOpenDeleteModal = (receipt) => {
    setDeleteReceipt(receipt);
    setIsDeleteOpen(true);
  };

  const handleUnlink = async (receiptId) => {
    try {
      await receiptService.unlinkTransaction(receiptId);
      setSuccessMessage('Receipt unlinked from transaction.');
      loadReceipts(true);
    } catch (err) {
      setError(err.normalizedMessage || 'Failed to unlink transaction.');
    }
  };

  const handleConfirmDelete = async (receiptId) => {
    await receiptService.deleteReceipt(receiptId);
    setSuccessMessage('Receipt deleted successfully.');
    loadReceipts(true);
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Page Header */}
      <PageHeader
        title="Receipt Vault"
        subtitle="Store physical bills and payment screenshots, extract data with OCR, and link directly to your financial transactions."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => loadReceipts(true)}
              disabled={isLoading || isRefreshing}
              title="Refresh"
              aria-label="Refresh"
              className="flex items-center gap-1.5 border-slate-200 hover:bg-slate-50 text-slate-700"
            >
              <RotateCcw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Refresh</span>
            </Button>

            <Button
              variant="primary"
              size="sm"
              onClick={() => setIsUploadOpen(true)}
              className="flex items-center gap-1.5 shadow-sm"
            >
              <UploadCloud className="w-4 h-4" />
              <span>Scan / Upload Receipt</span>
            </Button>
          </div>
        }
      />

      {/* Success Notification */}
      {successMessage && (
        <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center justify-between shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button
            onClick={() => setSuccessMessage('')}
            className="text-emerald-600 hover:text-emerald-900 text-xs font-bold"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadReceipts()}
            className="text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Retry
          </Button>
        </div>
      )}

      {/* Content */}
      {isLoading ? (
        <ReceiptsSkeleton />
      ) : receipts.length === 0 ? (
        <ReceiptsEmptyState
          onUploadClick={() => setIsUploadOpen(true)}
          onScanClick={() => setIsUploadOpen(true)}
        />
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {receipts.map((receipt) => (
              <ReceiptCard
                key={`${receipt.id}-${refreshKey}`}
                receipt={receipt}
                refreshKey={refreshKey}
                onReviewOcr={handleReviewOcr}
                onLinkClick={handleOpenLinkModal}
                onUnlinkClick={handleUnlink}
                onDeleteClick={handleOpenDeleteModal}
              />
            ))}
          </div>

          <ReceiptPagination
            currentPage={pageInfo.number}
            totalPages={pageInfo.totalPages}
            totalElements={pageInfo.totalElements}
            onPageChange={(newPage) => setCurrentPage(newPage)}
          />
        </div>
      )}

      {/* Modals */}
      <ReceiptUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onUploadComplete={handleUploadComplete}
      />

      <ReceiptMatchChoiceModal
        isOpen={isMatchChoiceOpen}
        onClose={() => setIsMatchChoiceOpen(false)}
        receipt={matchChoiceReceipt}
        ocrData={matchChoiceOcrData}
        candidates={matchCandidates}
        onLinkSuccess={(msg) => {
          setSuccessMessage(msg);
          loadReceipts(true);
        }}
        onCreateNew={(receipt, ocrData) => {
          setReviewReceipt(receipt);
          setReviewOcrData(ocrData);
          setIsReviewOpen(true);
        }}
      />

      <ReceiptReviewModal
        isOpen={isReviewOpen}
        onClose={() => setIsReviewOpen(false)}
        receipt={reviewReceipt}
        ocrData={reviewOcrData}
        onSuccess={() => {
          setSuccessMessage('Transaction created and receipt linked successfully.');
          loadReceipts(true);
        }}
      />

      <ReceiptLinkModal
        isOpen={isLinkOpen}
        onClose={() => setIsLinkOpen(false)}
        receipt={linkReceipt}
        onSuccess={() => {
          setSuccessMessage('Receipt linked to transaction successfully.');
          loadReceipts(true);
        }}
      />

      <ReceiptDeleteModal
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        receipt={deleteReceipt}
        onConfirmDelete={handleConfirmDelete}
      />
    </div>
  );
}

export default ReceiptsPage;
