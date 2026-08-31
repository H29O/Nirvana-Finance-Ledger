import React, { useState, useEffect, useCallback } from 'react';
import analyticsService from '../../services/analytics.service';
import { getThisMonth } from './utils/dateRanges';
import AnalyticsHeader from './components/AnalyticsHeader';
import IncomeExpenseSummary from './components/IncomeExpenseSummary';
import CategorySpendingChart from './components/CategorySpendingChart';
import SpendingTrendChart from './components/SpendingTrendChart';
import AnalyticsInsights from './components/AnalyticsInsights';
import AnalyticsSkeleton from './components/AnalyticsSkeleton';
import AnalyticsEmptyState from './components/AnalyticsEmptyState';
import Button from '../../components/ui/Button';
import { AlertCircle } from 'lucide-react';

export function AnalyticsPage() {
  // Date range state (default to This Month)
  const defaultRange = getThisMonth();
  const [startDate, setStartDate] = useState(defaultRange.startDate);
  const [endDate, setEndDate] = useState(defaultRange.endDate);
  const [periodLabel, setPeriodLabel] = useState(defaultRange.label);

  // Category breakdown type ('EXPENSE' | 'INCOME')
  const [categoryType, setCategoryType] = useState('EXPENSE');

  // Data states
  const [overview, setOverview] = useState(null);
  const [categories, setCategories] = useState([]);
  const [monthlyTrend, setMonthlyTrend] = useState([]);
  const [comparison, setComparison] = useState(null);

  // Loading & Error states
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');

  // Fetch all analytics data concurrently
  const loadAnalytics = useCallback(
    async (isSilent = false) => {
      if (!isSilent) setIsLoading(true);
      else setIsRefreshing(true);
      setError('');

      try {
        const [overviewRes, categoryRes, trendRes, comparisonRes] =
          await Promise.allSettled([
            analyticsService.getOverview(startDate, endDate),
            analyticsService.getCategoryBreakdown(startDate, endDate, categoryType),
            analyticsService.getMonthlyTrend(startDate, endDate),
            analyticsService.getPeriodComparison(startDate, endDate),
          ]);

        // Process Overview
        if (overviewRes.status === 'fulfilled') {
          setOverview(overviewRes.value);
        } else {
          console.warn('Overview fetch failed', overviewRes.reason);
        }

        // Process Category Breakdown
        if (categoryRes.status === 'fulfilled') {
          setCategories(categoryRes.value || []);
        } else {
          console.warn('Category breakdown fetch failed', categoryRes.reason);
          setCategories([]);
        }

        // Process Monthly Trend
        if (trendRes.status === 'fulfilled') {
          setMonthlyTrend(trendRes.value || []);
        } else {
          console.warn('Monthly trend fetch failed', trendRes.reason);
          setMonthlyTrend([]);
        }

        // Process Period Comparison
        if (comparisonRes.status === 'fulfilled') {
          setComparison(comparisonRes.value);
        } else {
          console.warn('Period comparison fetch failed', comparisonRes.reason);
          setComparison(null);
        }

        // If all 4 failed, show general error
        if (
          overviewRes.status === 'rejected' &&
          categoryRes.status === 'rejected' &&
          trendRes.status === 'rejected' &&
          comparisonRes.status === 'rejected'
        ) {
          setError(
            overviewRes.reason?.normalizedMessage ||
              'Failed to load analytics data. Please try again.'
          );
        }
      } catch (err) {
        setError(
          err.normalizedMessage || 'An unexpected error occurred while loading analytics.'
        );
      } finally {
        setIsLoading(false);
        setIsRefreshing(false);
      }
    },
    [startDate, endDate, categoryType]
  );

  useEffect(() => {
    loadAnalytics();
  }, [loadAnalytics]);

  const handlePeriodChange = (newStart, newEnd, newLabel) => {
    setStartDate(newStart);
    setEndDate(newEnd);
    setPeriodLabel(newLabel);
  };

  const handleResetToThisMonth = () => {
    const thisMonth = getThisMonth();
    handlePeriodChange(thisMonth.startDate, thisMonth.endDate, thisMonth.label);
  };

  // Identify top category driver for insights
  const topCategory =
    categories && categories.length > 0
      ? [...categories].sort(
          (a, b) => Number(b.amount || 0) - Number(a.amount || 0)
        )[0]
      : null;

  const hasActivity =
    overview &&
    (Number(overview.totalIncome) > 0 ||
      Number(overview.totalExpense) > 0 ||
      (overview.transactionCount != null && overview.transactionCount > 0));

  return (
    <div className="space-y-7 max-w-7xl mx-auto pb-12">
      {/* Header & Date Controls */}
      <AnalyticsHeader
        startDate={startDate}
        endDate={endDate}
        periodLabel={periodLabel}
        onPeriodChange={handlePeriodChange}
        onRefresh={() => loadAnalytics(true)}
        isLoading={isRefreshing}
      />

      {/* Global Error Banner */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadAnalytics()}
            className="text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Retry
          </Button>
        </div>
      )}

      {/* Main Content Area */}
      {isLoading ? (
        <AnalyticsSkeleton />
      ) : !hasActivity ? (
        <AnalyticsEmptyState
          onResetRange={handleResetToThisMonth}
          periodLabel={periodLabel}
        />
      ) : (
        <div className="space-y-7 animate-fade-in">
          {/* 1. Income, Expense & Net Savings Overview */}
          <IncomeExpenseSummary overview={overview} />

          {/* 2. Visualizations Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Category Breakdown Donut & Spectrum */}
            <CategorySpendingChart
              categories={categories}
              categoryType={categoryType}
              onTypeChange={(newType) => setCategoryType(newType)}
            />

            {/* Monthly Financial Trend Multi-Series Bar Chart */}
            <SpendingTrendChart monthlyTrend={monthlyTrend} />
          </div>

          {/* 3. Performance Insights & Velocity */}
          <AnalyticsInsights
            comparison={comparison}
            overview={overview}
            topCategory={topCategory}
            startDate={startDate}
            endDate={endDate}
          />
        </div>
      )}
    </div>
  );
}

export default AnalyticsPage;
