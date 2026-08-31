import React from 'react';
import {
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Calendar,
  RotateCcw,
} from 'lucide-react';
import Button from '../../../components/ui/Button';

const MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

export function WrappedIntro({
  selectedYear,
  selectedMonth,
  onPeriodChange,
  onRefresh,
  isLoading,
}) {
  const currentYear = new Date().getFullYear();
  const years = [currentYear, currentYear - 1, currentYear - 2, currentYear - 3].filter(
    (y) => y >= 2000
  );

  const handlePrev = () => {
    let nextMonth = selectedMonth - 1;
    let nextYear = selectedYear;
    if (nextMonth < 1) {
      nextMonth = 12;
      nextYear -= 1;
    }
    if (nextYear >= 2000) {
      onPeriodChange(nextYear, nextMonth);
    }
  };

  const handleNext = () => {
    let nextMonth = selectedMonth + 1;
    let nextYear = selectedYear;
    if (nextMonth > 12) {
      nextMonth = 1;
      nextYear += 1;
    }
    onPeriodChange(nextYear, nextMonth);
  };

  return (
    <div className="space-y-6 pb-2">
      {/* Hero Title & Subtitle */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-200/80">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100/80 shadow-sm">
              <Sparkles className="w-5 h-5" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
              Your Money Wrapped
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 max-w-xl leading-relaxed">
            A look back at your financial trajectory, habits, and spending story.
          </p>
        </div>

        {/* Controls: Year / Month Pickers & Navigation */}
        <div className="flex items-center gap-2.5 flex-wrap shrink-0">
          {/* Quick Month Navigation */}
          <div className="inline-flex items-center rounded-xl bg-slate-100 p-0.5 border border-slate-200/80">
            <button
              type="button"
              onClick={handlePrev}
              title="Previous Month"
              aria-label="Previous Month"
              className="p-1.5 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-white transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handleNext}
              title="Next Month"
              aria-label="Next Month"
              className="p-1.5 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-white transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Month Selector */}
          <select
            value={selectedMonth}
            onChange={(e) => onPeriodChange(selectedYear, Number(e.target.value))}
            className="px-3 py-1.5 text-xs font-semibold bg-white border border-slate-200 rounded-xl text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {MONTH_NAMES.map((name, idx) => (
              <option key={idx + 1} value={idx + 1}>
                {name}
              </option>
            ))}
          </select>

          {/* Year Selector */}
          <select
            value={selectedYear}
            onChange={(e) => onPeriodChange(Number(e.target.value), selectedMonth)}
            className="px-3 py-1.5 text-xs font-semibold bg-white border border-slate-200 rounded-xl text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {years.map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>

          {/* Refresh Button */}
          <Button
            variant="outline"
            size="sm"
            onClick={onRefresh}
            disabled={isLoading}
            className="p-2 border-slate-200 text-slate-600 hover:bg-slate-50"
            title="Refresh"
            aria-label="Refresh"
          >
            <RotateCcw
              className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`}
            />
          </Button>
        </div>
      </div>
    </div>
  );
}

export default WrappedIntro;
