import React from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { Sparkles, Calendar, Store, ArrowRight, Coffee, HandCoins } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StoryHighlights({
  spendingSpikes,
  merchantAnalysis,
  smallPurchases,
  debtStatistics,
  monthName,
  onNext,
}) {
  const peakDay = spendingSpikes?.peakSpendingDay;
  const topMerchant = merchantAnalysis?.topMerchants?.[0];
  const microPurchases = smallPurchases;

  const hasPeakDay = peakDay && Number(peakDay.amount || 0) > 0;
  const hasTopMerchant = topMerchant && Number(topMerchant.totalSpent || 0) > 0;
  const hasSmallPurchases =
    microPurchases && Number(microPurchases.totalAmount || 0) > 0;

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
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-100">
        <Sparkles className="w-3.5 h-3.5" />
        <span>Highlights & Habits</span>
      </div>

      {/* Main Highlights Content */}
      <div className="space-y-4 max-w-lg mx-auto my-auto animate-slide-up w-full">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          Notable Moments in {monthName}
        </span>

        <div className="grid grid-cols-1 gap-3 max-w-sm mx-auto text-left">
          {/* Peak Spending Day */}
          {hasPeakDay && (
            <div className="p-3.5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex items-start gap-3">
              <div className="p-2 rounded-xl bg-rose-50 text-rose-600 border border-rose-100 shrink-0 mt-0.5">
                <Calendar className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Biggest Spending Day
                </span>
                <p className="text-sm font-extrabold text-slate-900">
                  {peakDateFormatted}
                </p>
                <p className="text-xs text-slate-600 font-tabular font-semibold">
                  {formatCurrency(peakDay.amount)}{' '}
                  <span className="text-slate-400 font-normal">
                    ({peakDay.transactionCount}{' '}
                    {peakDay.transactionCount === 1 ? 'transaction' : 'transactions'})
                  </span>
                </p>
              </div>
            </div>
          )}

          {/* Top Merchant */}
          {hasTopMerchant && (
            <div className="p-3.5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex items-start gap-3">
              <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100 shrink-0 mt-0.5">
                <Store className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Top Frequent Spot
                </span>
                <p className="text-sm font-extrabold text-slate-900 truncate">
                  {topMerchant.merchantName}
                </p>
                <p className="text-xs text-slate-600 font-tabular font-semibold">
                  {formatCurrency(topMerchant.totalSpent)}{' '}
                  <span className="text-slate-400 font-normal">
                    ({topMerchant.transactionCount} visits)
                  </span>
                </p>
              </div>
            </div>
          )}

          {/* Micro Purchases */}
          {hasSmallPurchases && (
            <div className="p-3.5 rounded-2xl bg-white border border-slate-200/80 shadow-sm flex items-start gap-3">
              <div className="p-2 rounded-xl bg-amber-50 text-amber-600 border border-amber-100 shrink-0 mt-0.5">
                <Coffee className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Small Treats & Purchases
                </span>
                <p className="text-sm font-extrabold text-slate-900 font-tabular">
                  {formatCurrency(microPurchases.totalAmount)}
                </p>
                <p className="text-xs text-slate-500 font-medium">
                  {microPurchases.count} transactions under ₹20
                </p>
              </div>
            </div>
          )}

          {!hasPeakDay && !hasTopMerchant && !hasSmallPurchases && (
            <div className="p-4 text-center text-xs text-slate-400">
              No standout spikes or habits logged for this period.
            </div>
          )}
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

export default StoryHighlights;
