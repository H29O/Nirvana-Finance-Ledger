import React from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { TrendingDown, ArrowRight, Receipt } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StorySpending({ overview, monthName, onNext }) {
  const totalExpense = Number(overview?.totalExpense || 0);
  const totalTransactions = overview?.totalTransactions ?? 0;
  const hasExpenses = totalExpense > 0;

  const transactionText =
    totalTransactions === 1
      ? '1 transaction'
      : `${totalTransactions} transactions`;

  return (
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-100">
        <TrendingDown className="w-3.5 h-3.5" />
        <span>The Outflow</span>
      </div>

      {/* Main Stat & Narrative */}
      <div className="space-y-4 max-w-lg mx-auto my-auto animate-slide-up">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          Day-to-day spending
        </span>

        <h3 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
          {hasExpenses ? 'And you spent' : 'Zero expenses recorded'}
        </h3>

        <div className="py-2">
          <p className="text-4xl sm:text-6xl font-black font-tabular text-rose-600 tracking-tight">
            {formatCurrency(totalExpense)}
          </p>
        </div>

        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-xl bg-slate-100 text-slate-700 text-xs font-semibold">
          <Receipt className="w-3.5 h-3.5 text-slate-500" />
          <span>Across {transactionText} in {monthName}</span>
        </div>
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

export default StorySpending;
