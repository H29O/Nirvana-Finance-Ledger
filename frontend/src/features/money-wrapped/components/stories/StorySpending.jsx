import React, { useRef } from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { Receipt, Flame } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StorySpending({ overview, monthName, onNext }) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [overview, monthName]);

  const totalExpense = Number(overview?.totalExpense || 0);
  const totalTransactions = overview?.totalTransactions ?? 0;
  const avgDailySpend = totalExpense > 0 ? Math.round(totalExpense / 30) : 0;

  const transactionText =
    totalTransactions === 1
      ? '1 transaction'
      : `${totalTransactions} transactions`;

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-slate-900"
    >
      <WrappedBackground mood="spending" />

      {/* Top Tag */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold uppercase tracking-widest bg-rose-100/80 text-rose-800 border border-rose-200/60 shadow-2xs">
          <Flame className="w-3.5 h-3.5 text-rose-600" />
          <span>Chapter 2 • Daily Rhythm</span>
        </span>
      </div>

      {/* Main Visual & Typography */}
      <div className="relative z-10 my-auto py-4 space-y-6">
        <div data-animate="headline" className="space-y-1">
          <p className="text-xs font-satoshi font-semibold uppercase tracking-wider text-slate-400">
            Daily Outflow Cadence
          </p>
          <h2 className="font-tanker text-3xl sm:text-4xl text-slate-900 tracking-wide uppercase leading-tight">
            A daily spending cadence.
          </h2>
        </div>

        {/* Hero Pacing Figure (Satoshi Black Tabular) */}
        <div data-animate="hero" className="relative py-1">
          <div className="flex items-baseline gap-2">
            <span className="text-5xl sm:text-6xl font-satoshi font-black font-tabular text-[#FF6B6B] tracking-tight">
              {formatCurrency(avgDailySpend)}
            </span>
            <span className="text-sm font-satoshi font-bold text-slate-400">
              / day
            </span>
          </div>
          <p className="text-xs font-satoshi font-normal text-slate-500 mt-1">
            Across {monthName}&apos;s 30-day rhythm
          </p>
        </div>

        {/* Visual Rhythm Scale / Bar */}
        <div data-animate="item" className="space-y-3 pt-1 font-satoshi">
          <div className="p-4 rounded-2xl bg-white/85 border border-rose-200/70 shadow-xs space-y-3 backdrop-blur-xs">
            <div className="flex items-center justify-between text-xs font-semibold text-rose-950">
              <span className="flex items-center gap-1.5 font-bold">
                <Receipt className="w-3.5 h-3.5 text-[#FF6B6B]" />
                <span>Transaction Volume</span>
              </span>
              <span className="font-bold text-rose-700 font-tabular">
                {transactionText}
              </span>
            </div>

            {/* Stylized Daily Activity Cadence Strip */}
            <div className="grid grid-cols-10 gap-1 pt-1">
              {Array.from({ length: 10 }).map((_, i) => (
                <div
                  key={i}
                  className={`h-2.5 rounded-sm transition-all duration-500 ${
                    i < Math.min(10, Math.ceil(totalTransactions / 4))
                      ? 'bg-gradient-to-t from-[#FF6B6B] to-[#FF8E8E]'
                      : 'bg-rose-100/70'
                  }`}
                />
              ))}
            </div>

            <p className="text-[11px] text-rose-900/80 leading-relaxed font-normal">
              You logged an average of {(totalTransactions / 30).toFixed(1)}{' '}
              transactions per day, totalling {formatCurrency(totalExpense)}.
            </p>
          </div>
        </div>
      </div>

      {/* Tap Next Indicator */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-stone-200/70"
      >
        <span className="text-xs text-slate-500 font-satoshi">
          Next: Where the money went
        </span>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="text-xs font-bold text-rose-700 hover:text-rose-800 px-3.5 py-1.5 rounded-lg bg-rose-100/80 hover:bg-rose-100 transition-colors shadow-2xs"
        >
          Next →
        </button>
      </div>
    </div>
  );
}

export default StorySpending;
