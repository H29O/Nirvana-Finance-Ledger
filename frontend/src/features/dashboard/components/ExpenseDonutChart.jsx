import React, { useState } from 'react';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import { PieChart as PieIcon, Layers } from 'lucide-react';

const FRIENDLY_COLORS = [
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

export function ExpenseDonutChart({ data }) {
  const [hoveredIndex, setHoveredIndex] = useState(null);

  const totalExpense = data?.totalExpense ?? 0;
  const categories = data?.categories || [];

  // Filter out any zero or negative entries
  const validCategories = categories.filter(
    (c) => (c.totalAmount ?? 0) > 0
  );

  const radius = 68;
  const strokeWidth = 24;
  const circumference = 2 * Math.PI * radius;

  // Compute slice offsets
  let accumulatedPercent = 0;
  const slices = validCategories.map((cat, index) => {
    const amount = cat.totalAmount ?? 0;
    const percent = totalExpense > 0 ? (amount / totalExpense) * 100 : 0;
    const strokeDasharray = `${(percent / 100) * circumference} ${circumference}`;
    const strokeDashoffset = -((accumulatedPercent / 100) * circumference);
    accumulatedPercent += percent;

    const color = cat.color || FRIENDLY_COLORS[index % FRIENDLY_COLORS.length];

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
    <Card className="p-6 md:p-8 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <CardHeader className="p-0 pb-6 flex flex-row items-center justify-between">
        <div>
          <CardTitle className="text-lg font-bold text-slate-900">
            Where Did Your Money Go?
          </CardTitle>
          <CardDescription className="text-xs text-slate-500">
            Category-wise breakdown of this month&apos;s spending
          </CardDescription>
        </div>

        <div className="p-2 rounded-xl bg-slate-50 text-slate-500 border border-slate-100">
          <PieIcon className="w-4 h-4" />
        </div>
      </CardHeader>

      <CardContent className="p-0">
        {validCategories.length === 0 ? (
          <div className="py-12 text-center space-y-2">
            <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-6 h-6" />
            </div>
            <p className="text-sm font-medium text-slate-700">No expenses this month</p>
            <p className="text-xs text-slate-400 max-w-xs mx-auto">
              Any expenses you log will automatically be categorized here.
            </p>
          </div>
        ) : (
          <div className="flex flex-col md:flex-row items-center justify-between gap-8 pt-2">
            {/* SVG Donut Chart with Center Summary */}
            <div className="relative w-52 h-52 sm:w-60 sm:h-60 shrink-0 flex items-center justify-center">
              <svg
                viewBox="0 0 180 180"
                className="w-full h-full -rotate-90 transform"
              >
                {/* Background Ring */}
                <circle
                  cx="90"
                  cy="90"
                  r={radius}
                  fill="transparent"
                  stroke="#f1f5f9"
                  strokeWidth={strokeWidth}
                />

                {/* Slices */}
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

              {/* Center Details Hole */}
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none p-4">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 truncate max-w-[130px]">
                  {activeCategory ? activeCategory.categoryName : 'Total Spent'}
                </span>
                <span className="text-xl sm:text-2xl font-extrabold font-tabular text-slate-900 leading-tight">
                  {formatCurrency(
                    activeCategory ? activeCategory.amount : totalExpense
                  )}
                </span>
                {activeCategory && (
                  <span className="text-xs font-bold text-indigo-600 mt-0.5">
                    {activeCategory.percent.toFixed(1)}% of total
                  </span>
                )}
              </div>
            </div>

            {/* Category Legend & Proportions List */}
            <div className="flex-1 w-full space-y-2.5 max-h-72 overflow-y-auto pr-1">
              {slices.map((slice, index) => {
                const isHovered = hoveredIndex === index;

                return (
                  <div
                    key={slice.categoryId || index}
                    onMouseEnter={() => setHoveredIndex(index)}
                    onMouseLeave={() => setHoveredIndex(null)}
                    className={`flex items-center justify-between p-2.5 rounded-xl transition-all cursor-pointer border ${
                      isHovered
                        ? 'bg-slate-50 border-slate-300 shadow-sm'
                        : 'border-transparent hover:bg-slate-50/70'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span
                        className="w-3 h-3 rounded-full shrink-0 transition-transform duration-150"
                        style={{
                          backgroundColor: slice.color,
                          transform: isHovered ? 'scale(1.25)' : 'scale(1)',
                        }}
                      />
                      <span className="text-xs sm:text-sm font-semibold text-slate-800 truncate">
                        {slice.categoryName || 'Uncategorized'}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 shrink-0">
                      <span className="text-xs font-semibold text-slate-500 font-tabular">
                        {slice.percent.toFixed(1)}%
                      </span>
                      <span className="text-xs sm:text-sm font-bold text-slate-900 font-tabular">
                        {formatCurrency(slice.amount)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default ExpenseDonutChart;
