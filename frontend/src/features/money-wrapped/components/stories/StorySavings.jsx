import React from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { PiggyBank, ArrowRight, Sparkles, Scale, AlertCircle } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StorySavings({ overview, monthName, onNext }) {
  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const savingsRate = Number(overview?.savingsRate || 0);

  const isSurplus = netSavings > 0;
  const isDeficit = netSavings < 0;

  return (
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div
        className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${
          isSurplus
            ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
            : isDeficit
            ? 'bg-amber-50 text-amber-700 border border-amber-100'
            : 'bg-slate-100 text-slate-700 border border-slate-200'
        }`}
      >
        <PiggyBank className="w-3.5 h-3.5" />
        <span>The Bottom Line</span>
      </div>

      {/* Main Stat & Narrative */}
      <div className="space-y-4 max-w-lg mx-auto my-auto animate-slide-up">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          Which means...
        </span>

        {isSurplus && (
          <>
            <h3 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              You kept
            </h3>
            <div className="py-2">
              <p className="text-4xl sm:text-6xl font-black font-tabular text-indigo-600 tracking-tight">
                {formatCurrency(netSavings)}
              </p>
            </div>
            {totalIncome > 0 && (
              <div className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full bg-emerald-50 text-emerald-800 text-xs sm:text-sm font-bold border border-emerald-200 shadow-sm">
                <Sparkles className="w-4 h-4 text-emerald-600" />
                <span>{savingsRate.toFixed(1)}% of your income saved</span>
              </div>
            )}
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed max-w-sm mx-auto pt-2">
              You finished {monthName} with a healthy financial buffer.
            </p>
          </>
        )}

        {isDeficit && (
          <>
            <h3 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              You spent more than you brought in
            </h3>
            <div className="py-2">
              <p className="text-4xl sm:text-5xl font-black font-tabular text-amber-600 tracking-tight">
                {formatCurrency(Math.abs(netSavings))}
              </p>
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mt-1">
                Net Deficit for {monthName}
              </span>
            </div>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed max-w-sm mx-auto pt-1">
              Expenses exceeded income by {formatCurrency(Math.abs(netSavings))}. A valuable retrospective checkpoint to calibrate spending next month!
            </p>
          </>
        )}

        {!isSurplus && !isDeficit && (
          <>
            <h3 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              A Balanced Month
            </h3>
            <div className="py-2">
              <p className="text-4xl sm:text-5xl font-black font-tabular text-slate-700 tracking-tight">
                {formatCurrency(0)}
              </p>
            </div>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed max-w-sm mx-auto">
              Your expenses matched your incoming credits exactly in {monthName}.
            </p>
          </>
        )}
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

export default StorySavings;
