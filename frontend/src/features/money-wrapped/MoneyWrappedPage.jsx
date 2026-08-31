import React, { useState, useEffect, useCallback } from 'react';
import wrappedService from '../../services/wrapped.service';
import WrappedLanding from './components/WrappedLanding';
import WrappedLocked from './components/WrappedLocked';
import WrappedStory from './components/WrappedStory';
import WrappedSkeleton from './components/WrappedSkeleton';
import Button from '../../components/ui/Button';
import { AlertCircle } from 'lucide-react';
import {
  isMonthCompleted,
  getAvailableCompletedMonths,
  MONTH_NAMES,
} from './utils/wrappedDateUtils';

export function MoneyWrappedPage() {
  const completedMonths = getAvailableCompletedMonths(12);
  const latestCompleted = completedMonths[0] || { year: 2026, month: 7 };

  // View state: 'landing' | 'story' | 'locked'
  const [viewMode, setViewMode] = useState('landing');
  const [selectedYear, setSelectedYear] = useState(latestCompleted.year);
  const [selectedMonth, setSelectedMonth] = useState(latestCompleted.month);

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
    setSelectedYear(year);
    setSelectedMonth(month);

    if (!isMonthCompleted(year, month)) {
      setViewMode('locked');
      setWrappedData(null);
    } else {
      setViewMode('story');
      loadStoryData(year, month);
    }
  };

  const handleOpenLatest = () => {
    if (latestCompleted) {
      handleSelectMonth(latestCompleted.year, latestCompleted.month);
    }
  };

  const handleBackToLanding = () => {
    setViewMode('landing');
    setWrappedData(null);
    setError('');
  };

  const monthName = MONTH_NAMES[selectedMonth] || `Month ${selectedMonth}`;

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

      {viewMode === 'locked' && (
        <WrappedLocked
          targetMonthName={monthName}
          targetYear={selectedYear}
          onOpenLatestCompleted={handleOpenLatest}
          onBackToHub={handleBackToLanding}
        />
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
