import React, { useRef } from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { Store, Navigation, MapPin } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StoryMerchants({ merchantAnalysis, monthName, onNext }) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [merchantAnalysis, monthName]);

  const topMerchant =
    merchantAnalysis?.mostFrequentMerchant ||
    merchantAnalysis?.topMerchants?.[0];
  const merchantDisplayName =
    topMerchant?.merchant || topMerchant?.merchantName;
  const uniqueMerchants = merchantAnalysis?.uniqueMerchantsCount || 0;
  const hasMerchant = Boolean(
    merchantDisplayName && merchantDisplayName !== 'Unknown'
  );

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-[#174C3C]"
    >
      <WrappedBackground mood="merchants" />

      {/* Top Tag */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold uppercase tracking-widest bg-[#FFF8E7]/90 text-[#174C3C] border border-[#174C3C]/15 shadow-2xs backdrop-blur-xs">
          <Navigation className="w-3.5 h-3.5 text-[#174C3C]" />
          <span>Chapter 4 • Habits & Frequency</span>
        </span>
      </div>

      {/* Main Content */}
      <div className="relative z-10 my-auto py-4 space-y-6">
        <div data-animate="headline" className="space-y-1">
          <p className="text-xs font-satoshi font-semibold uppercase tracking-wider text-[#174C3C]/60">
            Frequent Spots in {monthName}
          </p>
          <h2 className="font-tanker text-3xl sm:text-4xl text-[#174C3C] tracking-wide uppercase leading-tight">
            Your most visited place.
          </h2>
        </div>

        {hasMerchant ? (
          <div className="space-y-4">
            {/* Massive Merchant Card (Peach + Apricot + Forest Green) */}
            <div
              data-animate="hero"
              className="p-5 sm:p-6 rounded-2xl bg-[#FFF8E7]/90 border border-[#174C3C]/15 space-y-4 backdrop-blur-xs relative overflow-hidden shadow-xs"
            >
              {/* Subtle radial glow inside card */}
              <div className="absolute top-0 right-0 w-32 h-32 bg-[#FFD1B8]/40 rounded-full blur-2xl pointer-events-none" />

              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#174C3C]/10 text-xs font-satoshi font-bold text-[#174C3C] border border-[#174C3C]/15">
                <Store className="w-3.5 h-3.5 text-[#174C3C]" />
                <span>#1 Frequent Merchant</span>
              </div>

              <div>
                <h3 className="font-tanker text-3xl sm:text-4xl md:text-5xl text-[#174C3C] tracking-wide break-words uppercase leading-tight">
                  {merchantDisplayName}
                </h3>
              </div>

              {/* Supporting stats */}
              <div className="pt-3 border-t border-[#174C3C]/15 flex items-center justify-between text-xs sm:text-sm font-satoshi font-medium text-[#174C3C]/80">
                <div>
                  <span className="text-[#174C3C]/60 text-[10px] uppercase tracking-wider block font-satoshi">
                    Visits
                  </span>
                  <span className="text-base font-bold text-[#174C3C] font-tabular font-satoshi">
                    {topMerchant.transactionCount}{' '}
                    {topMerchant.transactionCount === 1 ? 'time' : 'times'}
                  </span>
                </div>

                <div className="text-right">
                  <span className="text-[#174C3C]/60 text-[10px] uppercase tracking-wider block font-satoshi">
                    Total Spent
                  </span>
                  <span className="text-base font-black text-[#174C3C] font-tabular font-satoshi">
                    {formatCurrency(topMerchant.totalSpent)}
                  </span>
                </div>
              </div>
            </div>

            {/* Merchant Diversity Context */}
            {uniqueMerchants > 1 && (
              <div
                data-animate="item"
                className="flex items-center gap-2.5 px-4 py-2.5 rounded-2xl bg-[#FFF8E7]/75 border border-[#174C3C]/15 text-xs text-[#174C3C]/85 backdrop-blur-xs"
              >
                <MapPin className="w-4 h-4 text-[#174C3C] shrink-0" />
                <span>
                  You transacted across{' '}
                  <strong className="text-[#174C3C] font-bold">
                    {uniqueMerchants} distinct places
                  </strong>{' '}
                  in {monthName}.
                </span>
              </div>
            )}
          </div>
        ) : (
          <div className="py-12 text-center space-y-2">
            <h3 className="text-lg font-bold text-[#174C3C]">
              No frequent merchants
            </h3>
            <p className="text-xs text-[#174C3C]/70 max-w-xs mx-auto">
              Transactions for this month did not list specific recurring
              merchant details.
            </p>
          </div>
        )}
      </div>

      {/* Tap Next Indicator */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-[#174C3C]/15"
      >
        <span className="text-xs text-[#174C3C]/60 font-satoshi">
          Next: Notable spikes & moments
        </span>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="text-xs font-bold text-[#FFF8E7] bg-[#174C3C] hover:bg-[#123d30] px-3.5 py-1.5 rounded-lg transition-all shadow-md shadow-[#174C3C]/20 active:scale-95"
        >
          Next →
        </button>
      </div>
    </div>
  );
}

export default StoryMerchants;
