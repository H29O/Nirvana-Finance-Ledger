import React, { useState, useCallback } from 'react';
import wrappedService from '../../services/wrapped.service';
import WrappedLanding from './components/WrappedLanding';
import WrappedStory from './components/WrappedStory';
import WrappedSkeleton from './components/WrappedSkeleton';
import Button from '../../components/ui/Button';
import { AlertCircle } from 'lucide-react';
import {
  getRollingWrappedMonths,
  isMonthInRollingWindow,
  MONTH_NAMES,
} from './utils/wrappedDateUtils';

export function MoneyWrappedPage() {
  const rollingMonths = getRollingWrappedMonths();
  const latestMonth = rollingMonths[0] || { year: 2026, month: 10 };

  // View state: 'landing' | 'story'
  const [viewMode, setViewMode] = useState('landing');
  const [selectedYear, setSelectedYear] = useState(latestMonth.year);
  const [selectedMonth, setSelectedMonth] = useState(latestMonth.month);

  const [wrappedData, setWrappedData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const loadStoryData = useCallback(async (year, month) => {
    setIsLoading(true);
    setError('');

    try {
      const data = await wrappedService.getMoneyWrapped(year, month);
      setWrappedData(data);
    } catch (err) {
      setError(
        err.normalizedMessage ||
          'Failed to load your Money Wrapped story. Please try again.'
      );
      setWrappedData(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const handleSelectMonth = (year, month) => {
    if (!isMonthInRollingWindow(year, month)) {
      setError('Money Wrapped is only available for the rolling 3-month window.');
      return;
    }

    setSelectedYear(year);
    setSelectedMonth(month);
    setViewMode('story');
    loadStoryData(year, month);
  };

  const handleBackToLanding = () => {
    setViewMode('landing');
    setWrappedData(null);
    setError('');
  };

  return (
    <div className="max-w-6xl mx-auto pb-12">
      {/* Global Error Banner */}
      {error && (
        <div className="p-4 mb-6 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadStoryData(selectedYear, selectedMonth)}
            className="text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Retry
          </Button>
        </div>
      )}

      {/* View Mode Switching */}
      {viewMode === 'landing' && (
        <WrappedLanding onSelectMonth={handleSelectMonth} />
      )}

      {viewMode === 'story' && (
        <>
          {isLoading ? (
            <div className="max-w-xl mx-auto py-10">
              <WrappedSkeleton />
            </div>
          ) : (
            <WrappedStory
              data={wrappedData}
              year={selectedYear}
              month={selectedMonth}
              onExit={handleBackToLanding}
            />
          )}
        </>
      )}
    </div>
  );
}

export default MoneyWrappedPage;
