import React from 'react';
import { formatCurrency } from '../../../../lib/formatters';
import { PieChart, ArrowRight, Tag } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StoryCategory({ categoryDistribution, monthName, onNext }) {
  const topCategory = categoryDistribution?.topCategory;
  const categories = categoryDistribution?.categories || [];
  const hasCategories = categories.length > 0 && topCategory != null;

  const topCategoryName = topCategory?.categoryName || 'Uncategorized';
  const topCategoryAmount = Number(topCategory?.amount || 0);
  const topCategoryPercentage = Number(topCategory?.percentage || 0);

  // SVG Donut calculation
  const totalAmount = categories.reduce((sum, c) => sum + Number(c.amount || 0), 0);
  const radius = 35;
  const circumference = 2 * Math.PI * radius;
  let accumulatedPercent = 0;

  const slices = categories.map((cat) => {
    const amount = Number(cat.amount || 0);
    const percent = totalAmount > 0 ? (amount / totalAmount) * 100 : 0;
    const strokeDasharray = `${(percent / 100) * circumference} ${circumference}`;
    const strokeDashoffset = -((accumulatedPercent / 100) * circumference);
    accumulatedPercent += percent;

    return {
      ...cat,
      percent,
      strokeDasharray,
      strokeDashoffset,
      color: cat.color || '#6366f1',
    };
  });

  return (
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
        <PieChart className="w-3.5 h-3.5" />
        <span>Where It Went</span>
      </div>

      {/* Main Stat & Narrative */}
      <div className="space-y-4 max-w-lg mx-auto my-auto animate-slide-up w-full">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          Top Spending Driver
        </span>

        {hasCategories ? (
          <>
            <h3 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              Most of it went to
            </h3>

            <div className="py-2">
              <div className="inline-flex items-center gap-2 px-4 py-2 rounded-2xl bg-indigo-50/80 border border-indigo-100 mb-2">
                <span
                  className="w-3 h-3 rounded-full shrink-0 shadow-sm"
                  style={{ backgroundColor: topCategory.color || '#6366f1' }}
                />
                <span className="text-lg sm:text-xl font-bold text-slate-900">
                  {topCategoryName}
                </span>
              </div>
              <p className="text-3xl sm:text-5xl font-black font-tabular text-slate-900 tracking-tight">
                {formatCurrency(topCategoryAmount)}
              </p>
              <span className="text-xs font-bold text-indigo-600 font-tabular mt-1 block">
                {topCategoryPercentage.toFixed(1)}% of your monthly spending
              </span>
            </div>

            {/* Compact visual category ring & top 3 tags */}
            <div className="pt-2 flex flex-wrap items-center justify-center gap-2 max-w-sm mx-auto">
              {slices.slice(0, 4).map((slice, i) => (
                <div
                  key={slice.categoryId || i}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-50 border border-slate-200/70 text-xs font-medium text-slate-700 font-tabular"
                >
                  <span
                    className="w-2 h-2 rounded-full shrink-0"
                    style={{ backgroundColor: slice.color }}
                  />
                  <span className="truncate max-w-[90px]">{slice.categoryName}</span>
                  <span className="text-slate-400 font-semibold">{slice.percent.toFixed(0)}%</span>
                </div>
              ))}
            </div>
          </>
        ) : (
          <div className="py-6 space-y-2">
            <h4 className="text-lg font-bold text-slate-700">
              No categorized expenses
            </h4>
            <p className="text-xs text-slate-400 max-w-xs mx-auto">
              You didn&apos;t log categorized expenses in {monthName}.
            </p>
          </div>
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

export default StoryCategory;
