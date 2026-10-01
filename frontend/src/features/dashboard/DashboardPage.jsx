import React, { useState, useEffect, useCallback } from 'react';
import useAuth from '../../hooks/useAuth';
import dashboardService from '../../services/dashboard.service';
import transactionService from '../../services/transaction.service';
import Button from '../../components/ui/Button';
import DashboardSkeleton from './components/DashboardSkeleton';
import DashboardFinancialSummary from './components/DashboardFinancialSummary';
import ExpenseDonutChart from './components/ExpenseDonutChart';
import RecentTransactionsList from './components/RecentTransactionsList';
import DashboardEmptyState from './components/DashboardEmptyState';
import {
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  AlertCircle,
  Calendar,
} from 'lucide-react';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

export function DashboardPage() {
  const { user } = useAuth();
  const currentDate = new Date();
  const currentYear = currentDate.getFullYear();
  const currentMonth = currentDate.getMonth() + 1; // 1-indexed (1 to 12)

  const [selectedYear, setSelectedYear] = useState(currentYear);
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);

  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorBanner, setErrorBanner] = useState(null);

  // Dashboard Data State
  const [monthlySummary, setMonthlySummary] = useState(null);
  const [expenseByCategory, setExpenseByCategory] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);

  const isCurrentMonth = selectedYear === currentYear && selectedMonth === currentMonth;

  // Month navigation helpers
  const handlePrevMonth = () => {
    if (selectedMonth === 1) {
      setSelectedYear((prev) => prev - 1);
      setSelectedMonth(12);
    } else {
      setSelectedMonth((prev) => prev - 1);
    }
  };

  const handleNextMonth = () => {
    if (selectedMonth === 12) {
      setSelectedYear((prev) => prev + 1);
      setSelectedMonth(1);
    } else {
      setSelectedMonth((prev) => prev + 1);
    }
  };

  const handleResetToCurrentMonth = () => {
    setSelectedYear(currentYear);
    setSelectedMonth(currentMonth);
  };

  // Fetch dashboard data using Promise.allSettled for fault tolerance
  const loadDashboardData = useCallback(async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    else setIsRefreshing(true);

    setErrorBanner(null);

    try {
      const results = await Promise.allSettled([
        dashboardService.getMonthlySummary(selectedYear, selectedMonth),
        dashboardService.getExpenseByCategory(selectedYear, selectedMonth),
        transactionService.getTransactions({ page: 0, size: 5, sort: 'transactionDate,desc' }),
      ]);

      const [summaryResult, categoryExpenseResult, recentTxResult] = results;

      let failuresCount = 0;

      // 1. Monthly Summary
      if (summaryResult.status === 'fulfilled') {
        setMonthlySummary(summaryResult.value);
      } else {
        failuresCount++;
      }

      // 2. Expense by Category
      if (categoryExpenseResult.status === 'fulfilled') {
        setExpenseByCategory(categoryExpenseResult.value);
      } else {
        failuresCount++;
      }

      // 3. Recent Transactions
      if (recentTxResult.status === 'fulfilled') {
        setRecentTransactions(recentTxResult.value?.content || []);
      } else {
        failuresCount++;
      }

      if (failuresCount > 0) {
        setErrorBanner(
          failuresCount === results.length
            ? 'Unable to connect to financial server. Please verify backend is running on port 2906.'
            : 'Some dashboard metrics could not be loaded. Showing available data.'
        );
      }
    } catch (err) {
      setErrorBanner('Failed to load dashboard metrics. Please try again.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [selectedYear, selectedMonth]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  const monthName = MONTH_NAMES[selectedMonth - 1];
  const firstName = user?.name ? user.name.split(' ')[0] : 'there';

  const isPeriodEmpty =
    (monthlySummary?.totalTransactions ?? 0) === 0 &&
    (monthlySummary?.totalIncome ?? 0) === 0 &&
    (monthlySummary?.totalExpense ?? 0) === 0 &&
    (monthlySummary?.totalLent ?? 0) === 0 &&
    (monthlySummary?.totalDebt ?? 0) === 0 &&
    (expenseByCategory?.categories?.length ?? 0) === 0;

  return (
    <div className="space-y-6">
      {/* Friendly Greeting & Month Selector Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200/80">
        <div className="space-y-0.5">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Hi, {firstName} 👋
          </h1>
          <p className="text-xs sm:text-sm text-slate-500">
            Here&apos;s your spending overview for {monthName} {selectedYear}.
          </p>
        </div>

        {/* Month Selector Controls */}
        <div className="flex items-center gap-2">
          <div className="flex items-center bg-white border border-slate-200/90 rounded-xl p-1 shadow-sm">
            <button
              type="button"
              onClick={handlePrevMonth}
              title="Previous Month"
              aria-label="Previous Month"
              className="p-1.5 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            <div className="px-2.5 flex items-center gap-1.5 text-xs font-semibold text-slate-800 select-none">
              <Calendar className="w-3.5 h-3.5 text-indigo-600" />
              <span>{monthName} {selectedYear}</span>
            </div>

            <button
              type="button"
              onClick={handleNextMonth}
              title="Next Month"
              aria-label="Next Month"
              className="p-1.5 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {!isCurrentMonth && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleResetToCurrentMonth}
              leftIcon={<RotateCcw className="w-3.5 h-3.5" />}
              className="text-xs"
            >
              Current
            </Button>
          )}

          <Button
            variant="ghost"
            size="sm"
            onClick={() => loadDashboardData(true)}
            isLoading={isRefreshing}
            title="Refresh Data"
            aria-label="Refresh Data"
            className="p-2"
          >
            <RotateCcw className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* Error Alert Banner */}
      {errorBanner && (
        <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center justify-between text-xs text-rose-700">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-500 shrink-0" />
            <span>{errorBanner}</span>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => loadDashboardData(false)}
            className="text-xs border-rose-300 hover:bg-rose-100 text-rose-700"
          >
            Retry
          </Button>
        </div>
      )}

      {/* 1. Main Financial Summary Card (Savings, Income, Expenses) */}
      <DashboardFinancialSummary summary={monthlySummary} />

      {/* If current period is genuinely empty */}
      {isPeriodEmpty ? (
        <DashboardEmptyState monthName={monthName} year={selectedYear} />
      ) : (
        <div className="space-y-6">
          {/* 2. Primary Dynamic Expense Donut Chart + Category Legend */}
          <ExpenseDonutChart data={expenseByCategory} />

          {/* 3. Clean Recent Activity List */}
          <RecentTransactionsList transactions={recentTransactions} />
        </div>
      )}
    </div>
  );
}

export default DashboardPage;
