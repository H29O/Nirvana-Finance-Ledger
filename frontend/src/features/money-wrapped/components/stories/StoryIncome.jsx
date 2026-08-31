import React from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { TrendingUp, ArrowRight } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StoryIncome({ overview, monthName, onNext }) {
  const totalIncome = Number(overview?.totalIncome || 0);
  const hasIncome = totalIncome > 0;

  return (
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-100">
        <TrendingUp className="w-3.5 h-3.5" />
        <span>The Inflow</span>
      </div>

      {/* Main Stat & Narrative */}
      <div className="space-y-4 max-w-lg mx-auto my-auto animate-slide-up">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          In {monthName}
        </span>

        <h3 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
          {hasIncome ? 'You brought in' : 'No income logged'}
        </h3>

        <div className="py-2">
          <p className="text-4xl sm:text-6xl font-black font-tabular text-emerald-600 tracking-tight">
            {formatCurrency(totalIncome)}
          </p>
        </div>

        <p className="text-xs sm:text-sm text-slate-600 leading-relaxed max-w-sm mx-auto">
          {hasIncome
            ? `Your incoming deposits and earnings provided the fuel for ${monthName}.`
            : 'You had no incoming earnings recorded for this specific month.'}
        </p>
      </div>

      {/* CTA */}
      <div className="pt-2 animate-fade-in w-full max-w-xs">
        <Button
          variant="outline"
          size="md"
          onClick={onNext}
          className="w-full text-xs sm:text-sm font-semibold flex items-center justify-center gap-2 py-2.5 rounded-xl border-slate-200 hover:bg-slate-50"
        >
          <span>Continue</span>
          <ArrowRight className="w-4 h-4" />
        </Button>
      </div>
    </div>
  );
}

export default StoryIncome;
