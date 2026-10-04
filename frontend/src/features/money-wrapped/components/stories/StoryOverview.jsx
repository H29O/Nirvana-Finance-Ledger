import React, { useRef } from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { ArrowUpRight, TrendingUp } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StoryOverview({ overview, monthName, onNext }) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [overview, monthName]);

  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const savingsRate = Number(overview?.savingsRate || 0);

  const isSurplus = netSavings >= 0;
  const totalFlow = totalIncome + totalExpense;
  const expenseRatio = totalFlow > 0 ? (totalExpense / totalFlow) * 100 : 50;
  const incomeRatio = totalFlow > 0 ? (totalIncome / totalFlow) * 100 : 50;

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-slate-900"
    >
      <WrappedBackground mood="overview" />

      {/* Top Tag */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold uppercase tracking-widest bg-violet-100/80 text-violet-800 border border-violet-200/60 shadow-2xs">
          Chapter 1 • The Big Picture
        </span>
      </div>

      {/* Main Hero Number & Narrative */}
      <div className="relative z-10 my-auto py-4 space-y-6">
        <div data-animate="headline" className="space-y-1">
          <p className="text-xs font-satoshi font-semibold uppercase tracking-wider text-slate-400">
            Across all accounts in {monthName}
          </p>
          <h2 className="font-tanker text-3xl sm:text-4xl text-slate-900 tracking-wide uppercase leading-tight">
            You spent across the month.
          </h2>
        </div>

        {/* Hero Number with Subtle Halo Ring */}
        <div data-animate="hero" className="relative py-2">
          {/* Subtle halo accent behind hero amount */}
          <div className="absolute -left-3 top-0 w-32 h-16 bg-violet-200/40 rounded-full blur-xl -z-10" />

          <div className="text-5xl sm:text-6xl font-satoshi font-black font-tabular text-slate-900 tracking-tight">
            {formatCurrency(totalExpense)}
          </div>
          <span className="inline-flex items-center gap-1.5 text-xs font-satoshi font-bold text-rose-600 uppercase tracking-wider mt-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse" />
            <span>Total Outflow</span>
          </span>
        </div>

        {/* Proportional Cashflow Comparison Bar */}
        <div data-animate="item" className="space-y-2 pt-1 font-satoshi">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-600">
            <span>Cashflow Balance</span>
            <span>
              {totalIncome > 0
                ? `${Math.round((totalExpense / totalIncome) * 100)}% of income`
                : ''}
            </span>
          </div>

          {/* Visual Split Bar */}
          <div className="w-full h-3 bg-amber-900/5 rounded-full overflow-hidden flex shadow-inner border border-amber-900/10">
            <div
              className="bg-emerald-500 transition-all duration-700"
              style={{ width: `${incomeRatio}%` }}
              title="Income proportion"
            />
            <div
              className="bg-rose-500 transition-all duration-700"
              style={{ width: `${expenseRatio}%` }}
              title="Expense proportion"
            />
          </div>

          {/* Secondary Inflow & Savings Row */}
          <div className="grid grid-cols-2 gap-3 pt-2">
            <div className="p-3.5 rounded-2xl bg-white/80 border border-emerald-100 shadow-xs space-y-1 backdrop-blur-xs">
              <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-700 uppercase tracking-wider font-satoshi">
                <ArrowUpRight className="w-3.5 h-3.5" />
                <span>Brought In</span>
              </span>
              <p className="text-lg font-bold font-tabular text-slate-900 font-satoshi">
                {formatCurrency(totalIncome)}
              </p>
            </div>

            <div className="p-3.5 rounded-2xl bg-white/80 border border-violet-100 shadow-xs space-y-1 backdrop-blur-xs">
              <span
                className={`flex items-center gap-1 text-[11px] font-bold uppercase tracking-wider font-satoshi ${
                  isSurplus ? 'text-violet-700' : 'text-amber-700'
                }`}
              >
                <TrendingUp className="w-3.5 h-3.5" />
                <span>{isSurplus ? 'Net Kept' : 'Deficit'}</span>
              </span>
              <p className="text-lg font-bold font-tabular text-slate-900 font-satoshi">
                {formatCurrency(Math.abs(netSavings))}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Tap Next Indicator */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-stone-200/70"
      >
        <span className="text-xs text-slate-500 font-satoshi">
          {isSurplus
            ? `Kept ${savingsRate.toFixed(0)}% of incoming money`
            : 'Spent more than earned'}
        </span>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="text-xs font-bold text-indigo-700 hover:text-indigo-800 px-3.5 py-1.5 rounded-lg bg-indigo-100/80 hover:bg-indigo-100 transition-colors shadow-2xs"
        >
          Next →
        </button>
      </div>
    </div>
  );
}

export default StoryOverview;
