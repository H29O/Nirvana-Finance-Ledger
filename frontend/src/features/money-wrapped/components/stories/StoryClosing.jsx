import React from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { Sparkles, RotateCcw, Home, CheckCircle2, ArrowLeft } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StoryClosing({
  overview,
  categoryDistribution,
  monthName,
  year,
  onReplay,
  onExit,
}) {
  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const savingsRate = Number(overview?.savingsRate || 0);
  const topCategory = categoryDistribution?.topCategory;

  const isSurplus = netSavings >= 0;

  return (
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-100">
        <CheckCircle2 className="w-3.5 h-3.5" />
        <span>Month Completed</span>
      </div>

      {/* Recap Card */}
      <div className="space-y-4 max-w-sm w-full mx-auto my-auto animate-slide-up">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          {monthName} {year} Recap
        </span>

        <h3 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
          That was your month.
        </h3>

        {/* Shareable Summary Board */}
        <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200/90 shadow-sm space-y-3.5 text-left font-tabular">
          <div className="flex items-center justify-between text-xs sm:text-sm">
            <span className="text-slate-500 font-medium">Brought In</span>
            <span className="font-bold text-emerald-600">
              {formatCurrency(totalIncome)}
            </span>
          </div>

          <div className="flex items-center justify-between text-xs sm:text-sm">
            <span className="text-slate-500 font-medium">Total Spent</span>
            <span className="font-bold text-rose-600">
              {formatCurrency(totalExpense)}
            </span>
          </div>

          <div className="pt-2 border-t border-slate-200 flex items-center justify-between text-sm sm:text-base font-extrabold">
            <span className="text-slate-800">
              {isSurplus ? 'Net Kept' : 'Net Deficit'}
            </span>
            <span className={isSurplus ? 'text-indigo-600' : 'text-amber-600'}>
              {formatCurrency(Math.abs(netSavings))}
            </span>
          </div>

          {topCategory && (
            <div className="pt-2 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
              <span className="font-medium text-slate-400">Top Category</span>
              <span className="font-bold text-slate-800 truncate max-w-[150px]">
                {topCategory.categoryName}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="pt-2 animate-fade-in w-full max-w-xs space-y-2">
        <Button
          variant="primary"
          size="md"
          onClick={onExit}
          className="w-full text-xs sm:text-sm font-bold flex items-center justify-center gap-2 py-2.5 rounded-xl shadow-sm"
        >
          <Home className="w-4 h-4" />
          <span>Exit Wrapped</span>
        </Button>

        <Button
          variant="ghost"
          size="sm"
          onClick={onReplay}
          className="w-full text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center justify-center gap-1.5"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Replay Story</span>
        </Button>
      </div>
    </div>
  );
}

export default StoryClosing;
