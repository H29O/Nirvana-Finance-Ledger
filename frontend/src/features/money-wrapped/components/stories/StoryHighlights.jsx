import React, { useRef } from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { Zap, Coffee, Sparkles } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StoryHighlights({
  spendingSpikes,
  smallPurchases,
  monthName,
  onNext,
}) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [spendingSpikes, smallPurchases, monthName]);

  const peakDay = spendingSpikes?.peakSpendingDay;
  const microPurchases = smallPurchases;

  const peakAmount = Number(peakDay?.totalAmount || peakDay?.amount || 0);
  const hasPeakDay = peakDay && peakAmount > 0;
  const hasSmallPurchases =
    microPurchases && Number(microPurchases.totalAmount || 0) > 0;
  const smallCount =
    microPurchases?.transactionCount ?? microPurchases?.count ?? 0;

  // Format peak day date nicely
  let peakDateFormatted = null;
  if (peakDay?.date) {
    try {
      const d = new Date(peakDay.date);
      peakDateFormatted = d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
      });
    } catch {
      peakDateFormatted = peakDay.date;
    }
  }

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-slate-900"
    >
      <WrappedBackground mood="highlights" />

      {/* Top Tag */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold uppercase tracking-widest bg-sky-100/90 text-sky-900 border border-sky-300/60 shadow-2xs">
          <Zap className="w-3.5 h-3.5 text-sky-600" />
          <span>Chapter 5 • Highlights & Spikes</span>
        </span>
      </div>

      {/* Main Content as a Connected Timeline Sequence */}
      <div className="relative z-10 my-auto py-4 space-y-6">
        <div data-animate="headline" className="space-y-1">
          <p className="text-xs font-satoshi font-semibold uppercase tracking-wider text-slate-400">
            Outflow Peaks & Patterns
          </p>
          <h2 className="font-tanker text-3xl sm:text-4xl text-slate-900 tracking-wide uppercase leading-tight">
            Moments that defined {monthName}.
          </h2>
        </div>

        {/* Connected Visual Sequence */}
        <div className="relative pl-6 space-y-5 border-l-2 border-sky-300/60 ml-2 pt-1 font-satoshi">
          {/* Moment 1: Peak Spending Day */}
          {hasPeakDay && (
            <div data-animate="item" className="relative space-y-1">
              {/* Glowing Timeline Node */}
              <div className="absolute -left-[31px] top-1 w-4 h-4 rounded-full bg-sky-500 border-2 border-white shadow-xs ring-4 ring-sky-200/50" />

              <span className="text-[10px] font-satoshi font-bold uppercase tracking-wider text-sky-700 block">
                Peak Outflow Day • {peakDateFormatted}
              </span>

              <div className="text-2xl sm:text-3xl font-satoshi font-black font-tabular text-slate-900 tracking-tight">
                {formatCurrency(peakAmount)}
              </div>

              <p className="text-xs font-satoshi font-normal text-slate-500">
                Highest single-day outflow ({peakDay.transactionCount}{' '}
                {peakDay.transactionCount === 1
                  ? 'transaction'
                  : 'transactions'}
                ).
              </p>
            </div>
          )}

          {/* Moment 2: Small Purchases */}
          {hasSmallPurchases && (
            <div data-animate="item" className="relative space-y-1 pt-1">
              {/* Glowing Timeline Node */}
              <div className="absolute -left-[31px] top-2 w-4 h-4 rounded-full bg-amber-500 border-2 border-white shadow-xs ring-4 ring-amber-200/50" />

              <span className="text-[10px] font-satoshi font-bold uppercase tracking-wider text-amber-700 block">
                Micro-Purchases • Under {formatCurrency(microPurchases.threshold || 100)}
              </span>

              <div className="text-2xl sm:text-3xl font-satoshi font-black font-tabular text-slate-900 tracking-tight">
                {formatCurrency(microPurchases.totalAmount)}
              </div>

              <p className="text-xs font-satoshi font-normal text-slate-500">
                Quick spends and small treats across {smallCount} transactions.
              </p>
            </div>
          )}

          {!hasPeakDay && !hasSmallPurchases && (
            <div className="relative py-4 text-xs text-slate-400">
              No standout spikes or micro-purchase habits logged for {monthName}.
            </div>
          )}
        </div>
      </div>

      {/* Tap Next Indicator */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-stone-200/70"
      >
        <span className="text-xs text-slate-500 font-satoshi">
          Next: Final summary
        </span>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="text-xs font-bold text-sky-800 hover:text-sky-950 px-3.5 py-1.5 rounded-lg bg-sky-200/80 hover:bg-sky-200 transition-colors shadow-2xs"
        >
          Next →
        </button>
      </div>
    </div>
  );
}

export default StoryHighlights;
