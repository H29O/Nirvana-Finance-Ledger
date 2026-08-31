import React, { useState } from 'react';
import PageHeader from '../../../components/ui/PageHeader';
import Button from '../../../components/ui/Button';
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  SlidersHorizontal,
  CalendarRange,
} from 'lucide-react';
import {
  getThisMonth,
  getLastMonth,
  getLast3Months,
  getLast6Months,
  getThisYear,
  shiftMonthRange,
  formatRangeLabel,
} from '../utils/dateRanges';

export function AnalyticsHeader({
  startDate,
  endDate,
  periodLabel,
  onPeriodChange,
  onRefresh,
  isLoading,
}) {
  const [isCustomOpen, setIsCustomOpen] = useState(false);
  const [customStart, setCustomStart] = useState(startDate);
  const [customEnd, setCustomEnd] = useState(endDate);

  const presets = [
    { key: 'THIS_MONTH', label: 'This Month', getter: getThisMonth },
    { key: 'LAST_MONTH', label: 'Last Month', getter: getLastMonth },
    { key: 'LAST_3_MONTHS', label: 'Last 3M', getter: getLast3Months },
    { key: 'LAST_6_MONTHS', label: 'Last 6M', getter: getLast6Months },
    { key: 'THIS_YEAR', label: 'This Year', getter: getThisYear },
  ];

  const handlePresetSelect = (preset) => {
    const range = preset.getter();
    onPeriodChange(range.startDate, range.endDate, preset.label);
    setIsCustomOpen(false);
  };

  const handlePrevMonth = () => {
    const range = shiftMonthRange(startDate, -1);
    onPeriodChange(range.startDate, range.endDate, range.label);
  };

  const handleNextMonth = () => {
    const range = shiftMonthRange(startDate, 1);
    onPeriodChange(range.startDate, range.endDate, range.label);
  };

  const handleApplyCustom = (e) => {
    e.preventDefault();
    if (customStart && customEnd && customStart <= customEnd) {
      onPeriodChange(customStart, customEnd, 'Custom Range');
      setIsCustomOpen(false);
    }
  };

  return (
    <div className="space-y-4">
      <PageHeader
        title="Financial Analytics"
        subtitle="Understand your spending, income, and financial trends."
        badge={
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
            <Calendar className="w-3.5 h-3.5" />
            <span>{periodLabel}</span>
          </span>
        }
        actions={
          <div className="flex items-center gap-2 flex-wrap">
            {/* Quick Month Shift */}
            <div className="inline-flex items-center rounded-xl bg-slate-100 p-0.5 border border-slate-200/80">
              <button
                type="button"
                onClick={handlePrevMonth}
                title="Previous Month"
                aria-label="Previous Month"
                className="p-1.5 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-white transition-colors"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={handleNextMonth}
                title="Next Month"
                aria-label="Next Month"
                className="p-1.5 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-white transition-colors"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            {/* Refresh Button */}
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={isLoading}
              className="flex items-center gap-1.5 border-slate-200 hover:bg-slate-50 text-slate-700"
            >
              <RotateCcw
                className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`}
              />
              <span className="hidden sm:inline">Refresh</span>
            </Button>
          </div>
        }
      />

      {/* Filter Presets Toolbar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 p-3 bg-white rounded-2xl border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)]">
        {/* Preset Buttons */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto pb-1 sm:pb-0 scrollbar-none">
          {presets.map((preset) => {
            const isSelected = periodLabel === preset.label;
            return (
              <button
                key={preset.key}
                type="button"
                onClick={() => handlePresetSelect(preset)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all whitespace-nowrap ${
                  isSelected
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                {preset.label}
              </button>
            );
          })}

          <button
            type="button"
            onClick={() => setIsCustomOpen(!isCustomOpen)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5 whitespace-nowrap ${
              periodLabel === 'Custom Range'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>Custom</span>
          </button>
        </div>

        {/* Selected Date Span Indicator */}
        <div className="text-xs text-slate-500 font-medium flex items-center gap-1.5 self-end sm:self-center">
          <CalendarRange className="w-3.5 h-3.5 text-slate-400" />
          <span>{formatRangeLabel(startDate, endDate)}</span>
        </div>
      </div>

      {/* Custom Date Range Popover Form */}
      {isCustomOpen && (
        <form
          onSubmit={handleApplyCustom}
          className="p-4 bg-slate-50 border border-slate-200 rounded-2xl flex flex-col sm:flex-row items-end gap-3 animate-fade-in"
        >
          <div className="w-full sm:w-auto flex-1 space-y-1">
            <label className="text-[11px] font-semibold text-slate-600 uppercase tracking-wider block">
              Start Date
            </label>
            <input
              type="date"
              value={customStart}
              onChange={(e) => setCustomStart(e.target.value)}
              className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              required
            />
          </div>

          <div className="w-full sm:w-auto flex-1 space-y-1">
            <label className="text-[11px] font-semibold text-slate-600 uppercase tracking-wider block">
              End Date
            </label>
            <input
              type="date"
              value={customEnd}
              onChange={(e) => setCustomEnd(e.target.value)}
              className="w-full px-3 py-1.5 text-xs bg-white border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              required
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setIsCustomOpen(false)}
              className="text-xs"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              className="text-xs w-full sm:w-auto"
            >
              Apply Range
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}

export default AnalyticsHeader;
