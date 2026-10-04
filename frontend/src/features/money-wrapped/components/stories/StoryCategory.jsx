import React, { useRef } from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { Award, Sparkles } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StoryCategory({ categoryDistribution, monthName, onNext }) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [categoryDistribution, monthName]);

  const topCategory = categoryDistribution?.topCategory;
  const categories = categoryDistribution?.categories || [];
  const hasCategories = categories.length > 0 && topCategory != null;

  const topCategoryName = topCategory?.categoryName || 'Uncategorized';
  const topCategoryAmount = Number(topCategory?.amount || 0);
  const topCategoryPercentage = Number(topCategory?.percentage || 0);
  const topColor = topCategory?.color || '#7C3AED';

  // Supporting runners-up
  const supportingCategories = categories
    .filter((c) => c.categoryName !== topCategoryName)
    .slice(0, 3);

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-slate-900"
    >
      <WrappedBackground mood="category" />

      {/* Top Tag */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold uppercase tracking-widest bg-amber-100/90 text-amber-900 border border-amber-300/60 shadow-2xs">
          <Award className="w-3.5 h-3.5 text-amber-600" />
          <span>Chapter 3 • Category Focus</span>
        </span>
      </div>

      {/* Main Content */}
      <div className="relative z-10 my-auto py-2 space-y-3 sm:space-y-3.5">
        {hasCategories ? (
          <>
            <div data-animate="headline" className="space-y-1">
              <p className="text-xs font-satoshi font-semibold uppercase tracking-wider text-slate-400">
                Where your money went
              </p>
              <h2 className="font-tanker text-3xl sm:text-4xl text-slate-900 tracking-wide uppercase leading-tight">
                <span className="text-[#7C3AED] underline decoration-[#FFD166] decoration-4 underline-offset-4">
                  {topCategoryName}
                </span>{' '}
                took the biggest slice.
              </h2>
            </div>

            {/* Hero Category Spotlight (The Winner) */}
            <div
              data-animate="hero"
              className="p-3.5 sm:p-4 rounded-2xl bg-white/90 border border-amber-200/80 shadow-xs space-y-2 font-satoshi backdrop-blur-xs relative overflow-hidden"
            >
              {/* Subtle gold ribbon gradient in top-right corner */}
              <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-bl from-amber-100/50 to-transparent pointer-events-none rounded-tr-2xl" />

              <div className="flex items-center justify-between relative z-10">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-50 text-xs font-bold text-amber-950 shadow-2xs border border-amber-200/70">
                  <Sparkles className="w-3 h-3 text-amber-600" />
                  <span>#1 Category</span>
                </span>
                <span className="text-sm font-black text-[#7C3AED] font-tabular">
                  {topCategoryPercentage.toFixed(0)}% of total
                </span>
              </div>

              <div className="text-4xl sm:text-5xl font-satoshi font-black font-tabular text-slate-900 tracking-tight relative z-10">
                {formatCurrency(topCategoryAmount)}
              </div>

              {/* Proportional visual bar for top category */}
              <div className="w-full h-2.5 bg-stone-100 rounded-full overflow-hidden relative z-10">
                <div
                  className="h-full rounded-full transition-all duration-700"
                  style={{
                    width: `${Math.min(100, Math.max(15, topCategoryPercentage))}%`,
                    backgroundColor: topColor,
                  }}
                />
              </div>
            </div>

            {/* Supporting Categories (Subordinate list) */}
            {supportingCategories.length > 0 && (
              <div data-animate="item" className="space-y-2 pt-1 font-satoshi">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                  Followed by
                </span>
                <div className="space-y-2">
                  {supportingCategories.map((cat, idx) => {
                    const catAmount = Number(cat.amount || 0);
                    const catPercent = Number(cat.percentage || 0);
                    return (
                      <div
                        key={cat.categoryId || idx}
                        className="flex items-center justify-between text-xs gap-3 p-1 rounded-lg hover:bg-white/40 transition-colors"
                      >
                        <div className="flex items-center gap-2 min-w-0 w-28 sm:w-32">
                          <span
                            className="w-2.5 h-2.5 rounded-full shrink-0 shadow-2xs"
                            style={{ backgroundColor: cat.color || '#94a3b8' }}
                          />
                          <span className="font-semibold text-slate-800 truncate">
                            {cat.categoryName}
                          </span>
                        </div>

                        {/* Thin horizontal bar */}
                        <div className="flex-1 h-1.5 bg-stone-200/70 rounded-full overflow-hidden mx-1">
                          <div
                            className="h-full rounded-full"
                            style={{
                              width: `${Math.min(100, catPercent)}%`,
                              backgroundColor: cat.color || '#6366f1',
                            }}
                          />
                        </div>

                        <div className="shrink-0 text-right font-tabular">
                          <span className="font-bold text-slate-900">
                            {formatCurrency(catAmount)}
                          </span>
                          <span className="text-slate-400 text-[10px] ml-1 font-satoshi font-semibold">
                            {catPercent.toFixed(0)}%
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="py-12 text-center space-y-2">
            <h3 className="text-xl font-bold text-slate-800">
              No categorized expenses
            </h3>
            <p className="text-xs text-slate-500 max-w-xs mx-auto">
              You did not log categorized expenses in {monthName}.
            </p>
          </div>
        )}
      </div>

      {/* Tap Next Indicator */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-stone-200/70"
      >
        <span className="text-xs text-slate-500 font-satoshi">
          Next: Your most visited places
        </span>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="text-xs font-bold text-amber-900 hover:text-amber-950 px-3.5 py-1.5 rounded-lg bg-amber-200/80 hover:bg-amber-200 transition-colors shadow-2xs"
        >
          Next →
        </button>
      </div>
    </div>
  );
}

export default StoryCategory;
