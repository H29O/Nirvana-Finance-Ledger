import React, { useState, useEffect, useCallback } from 'react';
import transactionService from '../../services/transaction.service';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import TransactionFilters from './components/TransactionFilters';
import TransactionList from './components/TransactionList';
import TransactionPagination from './components/TransactionPagination';
import TransactionsSkeleton from './components/TransactionsSkeleton';
import TransactionsEmptyState from './components/TransactionsEmptyState';
import AddTransactionModal from './components/AddTransactionModal';
import { PlusCircle, RotateCcw, AlertCircle } from 'lucide-react';

export function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
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

  // Filter and Sort states
  const [selectedType, setSelectedType] = useState('');
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState('');
  const [selectedSort, setSelectedSort] = useState('transactionDate,desc');
  const [currentPage, setCurrentPage] = useState(0);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);

  const isFiltered = Boolean(selectedType || selectedPaymentMethod || selectedSort !== 'transactionDate,desc');

  const loadTransactions = useCallback(async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    else setIsRefreshing(true);
    setError('');

    try {
      const data = await transactionService.getTransactions({
        type: selectedType || undefined,
        paymentMethod: selectedPaymentMethod || undefined,
        page: currentPage,
        size: 10,
        sort: selectedSort,
      });

      setTransactions(data?.content || []);
      setPageInfo({
        number: data?.number ?? 0,
        totalPages: data?.totalPages ?? 1,
        totalElements: data?.totalElements ?? 0,
        first: data?.first ?? true,
        last: data?.last ?? true,
      });
    } catch (err) {
      setError(err.normalizedMessage || 'Failed to load transactions. Please try again.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [selectedType, selectedPaymentMethod, selectedSort, currentPage]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const handleResetFilters = () => {
    setSelectedType('');
    setSelectedPaymentMethod('');
    setSelectedSort('transactionDate,desc');
    setCurrentPage(0);
  };

  const handleTypeChange = (newType) => {
    setSelectedType(newType);
    setCurrentPage(0);
  };

  const handlePaymentMethodChange = (newMethod) => {
    setSelectedPaymentMethod(newMethod);
    setCurrentPage(0);
  };

  const handleSortChange = (newSort) => {
    setSelectedSort(newSort);
    setCurrentPage(0);
  };

  const handlePageChange = (newPage) => {
    setCurrentPage(newPage);
  };

  const handleDeleteTransaction = async (id) => {
    try {
      await transactionService.deleteTransaction(id);
      loadTransactions(true);
    } catch (err) {
      setError(err.normalizedMessage || 'Failed to delete transaction.');
    }
  };

  const handleTransactionSaved = () => {
    loadTransactions(true);
  };

  const handleOpenAddModal = () => {
    setEditingTransaction(null);
    setIsModalOpen(true);
  };

  const handleEditTransaction = (tx) => {
    setEditingTransaction(tx);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Transactions"
        subtitle="Manage and track your everyday income, expense, and loan activity."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => loadTransactions(true)}
              isLoading={isRefreshing}
              title="Refresh"
              aria-label="Refresh"
              className="p-2 text-slate-500 hover:text-slate-900"
            >
              <RotateCcw className="w-4 h-4" />
            </Button>

            <Button
              variant="primary"
              size="sm"
              onClick={handleOpenAddModal}
              leftIcon={<PlusCircle className="w-4 h-4" />}
            >
              Add Transaction
            </Button>
          </div>
        }
      />

      {/* Error Alert */}
      {error && (
        <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 flex items-center justify-between text-xs text-rose-700 shadow-sm">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-500 shrink-0" />
            <span>{error}</span>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => loadTransactions(false)}
            className="text-xs border-rose-300 hover:bg-rose-100 text-rose-700"
          >
            Retry
          </Button>
        </div>
      )}

      {/* Filter and Sort Toolbar */}
      <TransactionFilters
        type={selectedType}
        onTypeChange={handleTypeChange}
        paymentMethod={selectedPaymentMethod}
        onPaymentMethodChange={handlePaymentMethodChange}
        sort={selectedSort}
        onSortChange={handleSortChange}
        onResetFilters={handleResetFilters}
        isFiltered={isFiltered}
      />

      {/* Main Content Area */}
      {isLoading ? (
        <TransactionsSkeleton />
      ) : transactions.length === 0 ? (
        <TransactionsEmptyState
          isFiltered={isFiltered}
          onAddTransaction={handleOpenAddModal}
          onResetFilters={handleResetFilters}
        />
      ) : (
        <div className="space-y-4">
          <TransactionList
            transactions={transactions}
            onDeleteTransaction={handleDeleteTransaction}
            onEditTransaction={handleEditTransaction}
          />

          <TransactionPagination
            currentPage={pageInfo.number}
            totalPages={pageInfo.totalPages}
            totalElements={pageInfo.totalElements}
            isFirst={pageInfo.first}
            isLast={pageInfo.last}
            onPageChange={handlePageChange}
          />
        </div>
      )}

      {/* Add / Edit Transaction Modal */}
      <AddTransactionModal
        isOpen={isModalOpen}
        transactionToEdit={editingTransaction}
        onClose={() => {
          setIsModalOpen(false);
          setEditingTransaction(null);
        }}
        onSuccess={handleTransactionSaved}
      />
    </div>
  );
}

export default TransactionsPage;
