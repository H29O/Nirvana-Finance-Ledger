import React, { useRef } from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { RotateCcw, Check, ArrowRight, Sparkles } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StoryClosing({
  overview,
  categoryDistribution,
  monthName,
  year,
  onReplay,
  onExit,
}) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [
    overview,
    categoryDistribution,
    monthName,
    year,
  ]);

  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const totalTransactions = overview?.totalTransactions ?? 0;
  const topCategory = categoryDistribution?.topCategory;

  const isSurplus = netSavings >= 0;

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-[#24221F]"
    >
      <WrappedBackground mood="closing" />

      {/* Top Tag */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold tracking-widest uppercase bg-white/90 text-[#24221F] border border-[#24221F]/15 shadow-2xs backdrop-blur-xs">
          <Sparkles className="w-3.5 h-3.5 text-[#F8D66D]" />
          <span>Chapter 6 • Complete</span>
        </div>
      </div>

      {/* Main Closing Headline & Summary Ledger */}
      <div className="relative z-10 my-auto py-4 space-y-5">
        <div data-animate="headline" className="space-y-1">
          <p className="text-xs font-satoshi font-semibold uppercase tracking-wider text-[#24221F]/60">
            {monthName} {year} Archived
          </p>
          <h2 className="font-tanker text-3xl sm:text-4xl md:text-5xl text-[#24221F] tracking-wide uppercase leading-tight">
            That&apos;s your <br className="sm:hidden" />
            <span className="text-[#F05A47]">
              {monthName}.
            </span>
          </h2>
          <p className="text-xs sm:text-sm font-satoshi font-normal text-[#24221F]/70">
            Here is your financial signature for the month.
          </p>
        </div>

        {/* The Monthly Summary Ledger Card */}
        <div
          data-animate="hero"
          className="p-4 sm:p-5 rounded-2xl bg-white/90 border border-amber-900/10 shadow-xs space-y-2.5 font-satoshi backdrop-blur-xs"
        >
          <div className="flex items-center justify-between text-xs text-[#24221F]/70 font-medium">
            <span className="tracking-wider text-[11px] uppercase text-[#24221F]/60">Inflow</span>
            <span className="font-bold text-emerald-600 text-sm font-tabular font-satoshi">
              {formatCurrency(totalIncome)}
            </span>
          </div>

          <div className="flex items-center justify-between text-xs text-[#24221F]/70 font-medium">
            <span className="tracking-wider text-[11px] uppercase text-[#24221F]/60">Outflow</span>
            <span className="font-bold text-[#F05A47] text-sm font-tabular font-satoshi">
              {formatCurrency(totalExpense)}
            </span>
          </div>

          <div className="pt-2 border-t border-[#24221F]/10 flex items-center justify-between text-xs sm:text-sm">
            <span className="font-bold text-[#24221F] tracking-wider text-[11px] uppercase">
              {isSurplus ? 'Net Saved' : 'Net Deficit'}
            </span>
            <span
              className={`font-black text-base font-tabular font-satoshi ${
                isSurplus ? 'text-[#174C3C]' : 'text-[#F05A47]'
              }`}
            >
              {formatCurrency(Math.abs(netSavings))}
            </span>
          </div>

          <div className="pt-2 border-t border-[#24221F]/10 flex items-center justify-between text-[11px] text-[#24221F]/70 font-medium">
            <span className="tracking-wider uppercase text-[#24221F]/60">Top Category</span>
            <span className="font-bold text-[#24221F] truncate max-w-[140px]">
              {topCategory?.categoryName || '—'}
            </span>
          </div>

          <div className="flex items-center justify-between text-[11px] text-[#24221F]/70 font-medium">
            <span className="tracking-wider uppercase text-[#24221F]/60">Total Volume</span>
            <span className="font-bold text-[#24221F] font-tabular">
              {totalTransactions} transactions
            </span>
          </div>
        </div>
      </div>

      {/* Footer Navigation */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-[#24221F]/10"
      >
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onReplay();
          }}
          className="text-xs font-semibold text-[#24221F]/70 hover:text-[#24221F] flex items-center gap-1.5 transition-colors px-2.5 py-1.5 rounded-lg hover:bg-black/5 active:scale-95"
        >
          <RotateCcw className="w-3.5 h-3.5 text-[#F8D66D]" />
          <span>Replay Story</span>
        </button>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onExit();
          }}
          className="text-xs font-bold text-white bg-[#F05A47] hover:bg-[#dd4836] px-3.5 py-1.5 rounded-lg transition-all shadow-md shadow-[#F05A47]/25 flex items-center gap-1.5 active:scale-95"
        >
          <span>Exit Wrapped</span>
          <ArrowRight className="w-3.5 h-3.5 text-[#FFF8E7]" />
        </button>
      </div>
    </div>
  );
}

export default StoryClosing;
