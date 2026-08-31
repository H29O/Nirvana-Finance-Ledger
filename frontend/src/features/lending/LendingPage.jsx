import React, { useState, useEffect, useCallback, useMemo } from 'react';
import lendingService from '../../services/lending.service';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import Card from '../../components/ui/Card';
import LoanCard from './components/LoanCard';
import LoanFormModal from './components/LoanFormModal';
import LoanPaymentModal from './components/LoanPaymentModal';
import LoanDetailsModal from './components/LoanDetailsModal';
import LoanDeleteModal from './components/LoanDeleteModal';
import LoanSkeleton from './components/LoanSkeleton';
import { formatCurrency } from '../../lib/formatters';
import {
  PlusCircle,
  RotateCcw,
  AlertCircle,
  CheckCircle2,
  Search,
  ArrowUpRight,
  ArrowDownLeft,
  Scale,
  Filter,
  Users,
} from 'lucide-react';

export function LendingPage() {
  const [records, setRecords] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Filters & Search
  const [typeFilter, setTypeFilter] = useState('ALL'); // 'ALL' | 'LENT' | 'BORROWED'
  const [statusFilter, setStatusFilter] = useState('ACTIVE'); // 'ALL' | 'ACTIVE' | 'SETTLED'
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);

  const [isPaymentOpen, setIsPaymentOpen] = useState(false);
  const [payingRecord, setPayingRecord] = useState(null);

  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const [detailsRecord, setDetailsRecord] = useState(null);

  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deletingRecord, setDeletingRecord] = useState(null);

  // Fetch all loan records (page size 100 to calculate true summaries and smooth UX)
  const loadRecords = useCallback(async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    else setIsRefreshing(true);
    setError('');

    try {
      const response = await lendingService.getLoanRecords({
        page: 0,
        size: 100,
        sort: 'createdAt,desc',
      });
      setRecords(response?.content || []);
    } catch (err) {
      setError(
        err.normalizedMessage || 'Failed to load lending records. Please try again.'
      );
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadRecords();
  }, [loadRecords]);

  // Auto-dismiss success notification after 4s
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(''), 4000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  // Derived Overview Metrics (calculated from real data)
  const metrics = useMemo(() => {
    let totalLentRemaining = 0;
    let activeLentCount = 0;
    let totalBorrowedRemaining = 0;
    let activeBorrowedCount = 0;

    for (const r of records) {
      const remaining = Number(r.remainingAmount || 0);
      if (r.status === 'ACTIVE') {
        if (r.type === 'LENT') {
          totalLentRemaining += remaining;
          activeLentCount++;
        } else if (r.type === 'BORROWED') {
          totalBorrowedRemaining += remaining;
          activeBorrowedCount++;
        }
      }
    }

    const netBalance = totalLentRemaining - totalBorrowedRemaining;

    return {
      totalLentRemaining,
      activeLentCount,
      totalBorrowedRemaining,
      activeBorrowedCount,
      netBalance,
    };
  }, [records]);

  // Filtered Records based on user selections
  const filteredRecords = useMemo(() => {
    return records.filter((record) => {
      // Type match
      if (typeFilter !== 'ALL' && record.type !== typeFilter) {
        return false;
      }

      // Status match
      if (statusFilter !== 'ALL' && record.status !== statusFilter) {
        return false;
      }

      // Search match
      if (searchQuery.trim() !== '') {
        const query = searchQuery.toLowerCase().trim();
        const nameMatch = record.personName?.toLowerCase().includes(query);
        const descMatch = record.description?.toLowerCase().includes(query);
        if (!nameMatch && !descMatch) return false;
      }

      return true;
    });
  }, [records, typeFilter, statusFilter, searchQuery]);

  // Split into Lent and Borrowed arrays for categorized display
  const lentRecords = useMemo(
    () => filteredRecords.filter((r) => r.type === 'LENT'),
    [filteredRecords]
  );
  const borrowedRecords = useMemo(
    () => filteredRecords.filter((r) => r.type === 'BORROWED'),
    [filteredRecords]
  );

  // Modal Handlers
  const handleOpenAdd = () => {
    setEditingRecord(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (record) => {
    setEditingRecord(record);
    setIsFormOpen(true);
  };

  const handleOpenPayment = (record) => {
    setPayingRecord(record);
    setIsPaymentOpen(true);
  };

  const handleOpenDetails = (record) => {
    setDetailsRecord(record);
    setIsDetailsOpen(true);
  };

  const handleOpenDelete = (record) => {
    setDeletingRecord(record);
    setIsDeleteOpen(true);
  };

  const handleSuccess = (message) => {
    setSuccessMessage(message);
    loadRecords(true);
  };

  const handleResetFilters = () => {
    setTypeFilter('ALL');
    setStatusFilter('ALL');
    setSearchQuery('');
  };

  const isFiltered =
    typeFilter !== 'ALL' || statusFilter !== 'ACTIVE' || searchQuery.trim() !== '';

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Page Header */}
      <PageHeader
        title="Lending & Borrowing"
        subtitle="Keep track of money you've lent and money you owe."
        actions={
          <div className="flex items-center gap-2.5">
            <Button
              variant="outline"
              size="sm"
              onClick={() => loadRecords(true)}
              disabled={isLoading || isRefreshing}
              className="flex items-center gap-1.5 border-slate-200 hover:bg-slate-50 text-slate-700"
            >
              <RotateCcw
                className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`}
              />
              <span className="hidden sm:inline">Refresh</span>
            </Button>

            <Button
              variant="primary"
              size="sm"
              onClick={handleOpenAdd}
              className="flex items-center gap-1.5 shadow-sm"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Add Record</span>
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
            onClick={() => loadRecords()}
            className="text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Retry
          </Button>
        </div>
      )}

      {/* 3. Overview Cards (Calculated directly from real backend data) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Money Lent (Receivable) */}
        <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] p-4 sm:p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">
              Money Lent (Receivable)
            </span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center border border-emerald-100">
              <ArrowUpRight className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-bold text-slate-900 font-tabular">
              {formatCurrency(metrics.totalLentRemaining)}
            </span>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {metrics.activeLentCount} active {metrics.activeLentCount === 1 ? 'borrower' : 'borrowers'}
            </p>
          </div>
        </Card>

        {/* Money Borrowed (Payable) */}
        <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] p-4 sm:p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">
              Money Borrowed (Payable)
            </span>
            <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center border border-amber-100">
              <ArrowDownLeft className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-xl sm:text-2xl font-bold text-slate-900 font-tabular">
              {formatCurrency(metrics.totalBorrowedRemaining)}
            </span>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {metrics.activeBorrowedCount} active {metrics.activeBorrowedCount === 1 ? 'lender' : 'lenders'}
            </p>
          </div>
        </Card>

        {/* Net Outstanding Balance */}
        <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] p-4 sm:p-5 flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">
              Net Balance
            </span>
            <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-700 flex items-center justify-center border border-indigo-100">
              <Scale className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span
              className={`text-xl sm:text-2xl font-bold font-tabular ${
                metrics.netBalance >= 0 ? 'text-emerald-700' : 'text-rose-700'
              }`}
            >
              {formatCurrency(Math.abs(metrics.netBalance))}
            </span>
            <p className="text-[11px] text-slate-400 mt-0.5">
              {metrics.netBalance >= 0 ? 'You are owed net' : 'You owe net'}
            </p>
          </div>
        </Card>
      </div>

      {/* Filter and Search Controls */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 bg-white p-3 sm:p-4 rounded-2xl border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        {/* Type Filter Buttons */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100/80 rounded-xl overflow-x-auto">
          <button
            type="button"
            onClick={() => setTypeFilter('ALL')}
            className={`py-1.5 px-3 rounded-lg text-xs font-semibold transition-all shrink-0 ${
              typeFilter === 'ALL'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            All Records
          </button>

          <button
            type="button"
            onClick={() => setTypeFilter('LENT')}
            className={`py-1.5 px-3 rounded-lg text-xs font-semibold transition-all shrink-0 ${
              typeFilter === 'LENT'
                ? 'bg-white text-emerald-700 shadow-sm'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Money Lent
          </button>

          <button
            type="button"
            onClick={() => setTypeFilter('BORROWED')}
            className={`py-1.5 px-3 rounded-lg text-xs font-semibold transition-all shrink-0 ${
              typeFilter === 'BORROWED'
                ? 'bg-white text-amber-700 shadow-sm'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Money Borrowed
          </button>
        </div>

        {/* Status Filter & Search */}
        <div className="flex items-center gap-2">
          {/* Status Dropdown */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-xl border border-slate-200 bg-white text-xs text-slate-700 py-2 px-3 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
          >
            <option value="ACTIVE">Active (Unsettled)</option>
            <option value="SETTLED">Settled ✓</option>
            <option value="ALL">All Statuses</option>
          </select>

          {/* Search Box */}
          <div className="relative min-w-[160px] sm:min-w-[200px]">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search person or note..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 text-xs rounded-xl border border-slate-200 bg-slate-50/50 text-slate-900 placeholder-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <LoanSkeleton />
      ) : records.length === 0 ? (
        /* Overall Empty State */
        <Card className="p-12 text-center bg-white border-slate-200/80 shadow-sm space-y-4">
          <div className="w-14 h-14 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center mx-auto text-indigo-600">
            <Users className="w-7 h-7" />
          </div>
          <div className="space-y-1 max-w-sm mx-auto">
            <h3 className="text-base font-bold text-slate-900">
              No loan records yet
            </h3>
            <p className="text-xs text-slate-500">
              Start keeping track of money you've lent to friends, family, or colleagues, and money you've borrowed.
            </p>
          </div>
          <Button
            variant="primary"
            size="sm"
            onClick={handleOpenAdd}
            className="inline-flex items-center gap-2"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Add your first record</span>
          </Button>
        </Card>
      ) : filteredRecords.length === 0 ? (
        /* Filtered Empty State */
        <Card className="p-10 text-center bg-white border-slate-200/80 shadow-sm space-y-3">
          <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center mx-auto text-slate-500">
            <Filter className="w-5 h-5" />
          </div>
          <div className="space-y-1">
            <h3 className="text-sm font-bold text-slate-900">
              No records match your filters
            </h3>
            <p className="text-xs text-slate-500">
              Try adjusting your search query or status filter.
            </p>
          </div>
          {isFiltered && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleResetFilters}
              className="text-xs"
            >
              Reset Filters
            </Button>
          )}
        </Card>
      ) : typeFilter === 'ALL' ? (
        /* Split Categorized Sections: Money Lent vs Money Borrowed */
        <div className="space-y-8">
          {/* Section 1: Money I've Lent */}
          {lentRecords.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center justify-between pb-1">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-emerald-500" />
                  <h3 className="text-sm font-bold text-slate-900">
                    Money I've Lent (Someone owes you)
                  </h3>
                  <span className="text-xs text-slate-400 font-normal">
                    ({lentRecords.length})
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {lentRecords.map((record) => (
                  <LoanCard
                    key={record.id}
                    record={record}
                    onRecordPayment={handleOpenPayment}
                    onViewDetails={handleOpenDetails}
                    onEdit={handleOpenEdit}
                    onDelete={handleOpenDelete}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Section 2: Money I've Borrowed */}
          {borrowedRecords.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center justify-between pb-1">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-amber-500" />
                  <h3 className="text-sm font-bold text-slate-900">
                    Money I've Borrowed (You owe someone)
                  </h3>
                  <span className="text-xs text-slate-400 font-normal">
                    ({borrowedRecords.length})
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {borrowedRecords.map((record) => (
                  <LoanCard
                    key={record.id}
                    record={record}
                    onRecordPayment={handleOpenPayment}
                    onViewDetails={handleOpenDetails}
                    onEdit={handleOpenEdit}
                    onDelete={handleOpenDelete}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        /* Single Tab Selected List Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredRecords.map((record) => (
            <LoanCard
              key={record.id}
              record={record}
              onRecordPayment={handleOpenPayment}
              onViewDetails={handleOpenDetails}
              onEdit={handleOpenEdit}
              onDelete={handleOpenDelete}
            />
          ))}
        </div>
      )}

      {/* Modals */}
      <LoanFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        editingRecord={editingRecord}
        onSuccess={handleSuccess}
      />

      <LoanPaymentModal
        isOpen={isPaymentOpen}
        onClose={() => setIsPaymentOpen(false)}
        record={payingRecord}
        onSuccess={handleSuccess}
      />

      <LoanDetailsModal
        isOpen={isDetailsOpen}
        onClose={() => setIsDetailsOpen(false)}
        record={detailsRecord}
        onRecordPayment={handleOpenPayment}
      />

      <LoanDeleteModal
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        record={deletingRecord}
        onSuccess={handleSuccess}
      />
    </div>
  );
}

export default LendingPage;
