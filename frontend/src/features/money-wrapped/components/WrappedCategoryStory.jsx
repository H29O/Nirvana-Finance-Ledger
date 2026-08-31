import React, { useState } from 'react';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import { PieChart as PieIcon, Layers, Tag, Award, Sparkles } from 'lucide-react';

const CATEGORY_PALETTE = [
  '#4f46e5', // Indigo
  '#06b6d4', // Cyan
  '#10b981', // Emerald
  '#f59e0b', // Amber
  '#f43f5e', // Rose
  '#8b5cf6', // Violet
  '#ec4899', // Pink
  '#3b82f6', // Blue
  '#14b8a6', // Teal
  '#f97316', // Orange
];

export function WrappedCategoryStory({ categoryDistribution }) {
  const [hoveredIndex, setHoveredIndex] = useState(null);

  const topCategory = categoryDistribution?.topCategory;
  const totalCategories = categoryDistribution?.totalCategoriesUsed ?? 0;
  const categories = categoryDistribution?.categories || [];

  const validCategories = categories.filter((c) => Number(c.amount || 0) > 0);
  const totalExpense = validCategories.reduce((acc, c) => acc + Number(c.amount || 0), 0);

  const radius = 68;
  const strokeWidth = 24;
  const circumference = 2 * Math.PI * radius;

  let accumulatedPercent = 0;
  const slices = validCategories.map((cat, index) => {
    const amount = Number(cat.amount || 0);
    const percent =
      cat.percentage != null
        ? Number(cat.percentage)
        : totalExpense > 0
        ? (amount / totalExpense) * 100
        : 0;

    const strokeDasharray = `${(percent / 100) * circumference} ${circumference}`;
    const strokeDashoffset = -((accumulatedPercent / 100) * circumference);
    accumulatedPercent += percent;

    const color = CATEGORY_PALETTE[index % CATEGORY_PALETTE.length];

    return {
      ...cat,
      amount,
      percent,
      color,
      strokeDasharray,
      strokeDashoffset,
    };
  });

  const activeCategory = hoveredIndex !== null ? slices[hoveredIndex] : null;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-1.5 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
          <PieIcon className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Category Breakdown & Cost Drivers
          </h3>
          <p className="text-xs text-slate-500">
            Where your funds flowed across essential and discretionary categories
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Spotlight: Top Category Driver */}
        <Card className="p-6 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                #1 Category Driver
              </span>
              <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100 flex items-center justify-center">
                <Award className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-1">
              <h4 className="text-2xl font-bold text-slate-900 truncate">
                {topCategory ? topCategory.categoryName : 'No expenses'}
              </h4>
              <p className="text-3xl font-extrabold font-tabular text-indigo-600">
                {topCategory ? formatCurrency(topCategory.amount) : '—'}
              </p>
              {topCategory?.percentage != null && (
                <p className="text-xs font-semibold text-slate-500 pt-1">
                  Responsible for {Number(topCategory.percentage).toFixed(1)}% of your expenses
                </p>
              )}
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100 text-xs text-slate-500 flex items-center justify-between mt-4">
            <span>Categories Used</span>
            <span className="font-semibold text-slate-800">
              {totalCategories} categories
            </span>
          </div>
        </Card>

        {/* Visual Donut & Distribution Spectrum (Spans 2 columns) */}
        <Card className="p-6 lg:col-span-2 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] flex flex-col justify-between">
          {validCategories.length === 0 ? (
            <div className="py-12 text-center space-y-2">
              <div className="w-12 h-12 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-center mx-auto text-slate-400">
                <Layers className="w-6 h-6" />
              </div>
              <p className="text-sm font-semibold text-slate-700">No categories recorded</p>
              <p className="text-xs text-slate-400">No category spending in this period.</p>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
              {/* SVG Donut */}
              <div className="relative w-48 h-48 sm:w-52 sm:h-52 shrink-0 flex items-center justify-center">
                <svg
                  viewBox="0 0 180 180"
                  className="w-full h-full -rotate-90 transform"
                >
                  <circle
                    cx="90"
                    cy="90"
                    r={radius}
                    fill="transparent"
                    stroke="#f1f5f9"
                    strokeWidth={strokeWidth}
                  />

                  {slices.map((slice, index) => {
                    const isHovered = hoveredIndex === index;
                    return (
                      <circle
                        key={slice.categoryId || index}
                        cx="90"
                        cy="90"
                        r={radius}
                        fill="transparent"
                        stroke={slice.color}
                        strokeWidth={isHovered ? strokeWidth + 4 : strokeWidth}
                        strokeDasharray={slice.strokeDasharray}
                        strokeDashoffset={slice.strokeDashoffset}
                        strokeLinecap="butt"
                        className="transition-all duration-200 cursor-pointer"
                        onMouseEnter={() => setHoveredIndex(index)}
                        onMouseLeave={() => setHoveredIndex(null)}
                      />
                    );
                  })}
                </svg>

                <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none p-4">
                  <span className="text-[10px] font-medium uppercase tracking-wider text-slate-400 truncate max-w-[100px]">
                    {activeCategory ? activeCategory.categoryName : 'Total Spent'}
                  </span>
                  <span className="text-base sm:text-lg font-bold font-tabular text-slate-900 leading-tight">
                    {formatCurrency(activeCategory ? activeCategory.amount : totalExpense)}
                  </span>
                  {activeCategory && (
                    <span className="text-[10px] font-semibold text-indigo-600 mt-0.5">
                      {activeCategory.percent.toFixed(1)}%
                    </span>
                  )}
                </div>
              </div>

              {/* Progress List */}
              <div className="flex-1 w-full space-y-2.5 max-h-56 overflow-y-auto pr-1">
                {slices.map((slice, index) => {
                  const isHovered = hoveredIndex === index;
                  return (
                    <div
                      key={slice.categoryId || index}
                      onMouseEnter={() => setHoveredIndex(index)}
                      onMouseLeave={() => setHoveredIndex(null)}
                      className={`p-2 rounded-xl transition-all cursor-pointer border ${
                        isHovered
                          ? 'bg-slate-50 border-slate-300 shadow-sm'
                          : 'border-transparent hover:bg-slate-50/70'
                      }`}
                    >
                      <div className="flex items-center justify-between text-xs mb-1">
                        <div className="flex items-center gap-2 min-w-0">
                          <span
                            className="w-2.5 h-2.5 rounded-full shrink-0"
                            style={{ backgroundColor: slice.color }}
                          />
                          <span className="font-semibold text-slate-800 truncate">
                            {slice.categoryName || 'Uncategorized'}
                          </span>
                        </div>
                        <div className="flex items-center gap-2 font-tabular shrink-0">
                          <span className="text-[11px] font-semibold text-slate-400">
                            {slice.percent.toFixed(1)}%
                          </span>
                          <span className="font-bold text-slate-900">
                            {formatCurrency(slice.amount)}
                          </span>
                        </div>
                      </div>
                      <div className="w-full h-1 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className="h-full rounded-full transition-all duration-300"
                          style={{
                            width: `${Math.min(100, Math.max(2, slice.percent))}%`,
                            backgroundColor: slice.color,
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

export default WrappedCategoryStory;
