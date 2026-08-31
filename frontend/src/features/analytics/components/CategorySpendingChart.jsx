import React, { useState } from 'react';
import Card, {
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import { PieChart as PieIcon, Layers, Tag, ArrowDownRight, ArrowUpRight } from 'lucide-react';

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
  '#64748b', // Slate
];

export function CategorySpendingChart({
  categories = [],
  categoryType = 'EXPENSE',
  onTypeChange,
}) {
  const [hoveredIndex, setHoveredIndex] = useState(null);

  const totalAmount = categories.reduce(
    (acc, cat) => acc + Number(cat.amount || 0),
    0
  );

  const validCategories = categories.filter((c) => Number(c.amount || 0) > 0);

  const radius = 68;
  const strokeWidth = 24;
  const circumference = 2 * Math.PI * radius;

  // Compute SVG arc slice parameters
  let accumulatedPercent = 0;
  const slices = validCategories.map((cat, index) => {
    const amount = Number(cat.amount || 0);
    const percent =
      cat.percentage != null
        ? Number(cat.percentage)
        : totalAmount > 0
        ? (amount / totalAmount) * 100
        : 0;

    const strokeDasharray = `${(percent / 100) * circumference} ${circumference}`;
    const strokeDashoffset = -((accumulatedPercent / 100) * circumference);
    accumulatedPercent += percent;

    const color =
      CATEGORY_PALETTE[index % CATEGORY_PALETTE.length];

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

  const isExpense = categoryType === 'EXPENSE';

  return (
    <Card className="p-6 md:p-7 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] flex flex-col justify-between">
      {/* Header & Type Toggle */}
      <CardHeader className="p-0 pb-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-100">
        <div>
          <CardTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
            <PieIcon className="w-4 h-4 text-indigo-600" />
            <span>Category Distribution</span>
          </CardTitle>
          <CardDescription className="text-xs text-slate-500 mt-0.5">
            {isExpense
              ? 'Where your money was spent across categories'
              : 'Sources and distribution of your income'}
          </CardDescription>
        </div>

        {/* Expense vs Income Toggle */}
        <div className="inline-flex items-center rounded-xl bg-slate-100 p-0.5 border border-slate-200/80 shrink-0">
          <button
            type="button"
            onClick={() => onTypeChange('EXPENSE')}
            className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all flex items-center gap-1 ${
              isExpense
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-900'
            }`}
          >
            <ArrowDownRight className="w-3.5 h-3.5 text-rose-500" />
            <span>Expenses</span>
          </button>
          <button
            type="button"
            onClick={() => onTypeChange('INCOME')}
            className={`px-3 py-1 text-xs font-semibold rounded-lg transition-all flex items-center gap-1 ${
              !isExpense
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-900'
            }`}
          >
            <ArrowUpRight className="w-3.5 h-3.5 text-emerald-500" />
            <span>Income</span>
          </button>
        </div>
      </CardHeader>

      <CardContent className="p-0 pt-6">
        {validCategories.length === 0 ? (
          <div className="py-14 text-center space-y-2">
            <div className="w-12 h-12 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-6 h-6" />
            </div>
            <p className="text-sm font-semibold text-slate-700">
              No category data recorded
            </p>
            <p className="text-xs text-slate-400 max-w-xs mx-auto">
              {isExpense
                ? 'No expenses logged for this period. Categorized expenses will appear here.'
                : 'No income logged for this period. Categorized income will appear here.'}
            </p>
          </div>
        ) : (
          <div className="flex flex-col lg:flex-row items-center justify-between gap-8">
            {/* SVG Donut Chart with Dynamic Center Display */}
            <div className="relative w-52 h-52 sm:w-56 sm:h-56 shrink-0 flex items-center justify-center">
              <svg
                viewBox="0 0 180 180"
                className="w-full h-full -rotate-90 transform"
              >
                {/* Base ring */}
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

              {/* Center Content */}
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none p-4">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 truncate max-w-[130px]">
                  {activeCategory
                    ? activeCategory.categoryName
                    : isExpense
                    ? 'Total Spent'
                    : 'Total Income'}
                </span>
                <span className="text-xl sm:text-2xl font-extrabold font-tabular text-slate-900 leading-tight">
                  {formatCurrency(
                    activeCategory ? activeCategory.amount : totalAmount
                  )}
                </span>
                {activeCategory ? (
                  <span className="text-xs font-bold text-indigo-600 mt-0.5">
                    {activeCategory.percent.toFixed(1)}% of total
                  </span>
                ) : (
                  <span className="text-[11px] font-semibold text-slate-400 mt-0.5">
                    {validCategories.length} Categories
                  </span>
                )}
              </div>
            </div>

            {/* Category Breakdown Progress Bars List */}
            <div className="flex-1 w-full space-y-2.5 max-h-72 overflow-y-auto pr-1">
              {slices.map((slice, index) => {
                const isHovered = hoveredIndex === index;

                return (
                  <div
                    key={slice.categoryId || index}
                    onMouseEnter={() => setHoveredIndex(index)}
                    onMouseLeave={() => setHoveredIndex(null)}
                    className={`p-2.5 rounded-xl border transition-all cursor-pointer ${
                      isHovered
                        ? 'bg-slate-50 border-slate-300 shadow-sm'
                        : 'border-slate-100 bg-white hover:bg-slate-50/70'
                    }`}
                  >
                    {/* Line 1: Category Name (Left) + INR Amount (Right) */}
                    <div className="flex items-center justify-between gap-3 min-w-0">
                      <div className="flex items-center gap-2 min-w-0 flex-1">
                        <span
                          className="w-2.5 h-2.5 rounded-full shrink-0 shadow-sm"
                          style={{ backgroundColor: slice.color }}
                        />
                        <span
                          className="text-xs sm:text-sm font-bold text-slate-800 truncate"
                          title={slice.categoryName}
                        >
                          {slice.categoryName || 'Uncategorized'}
                        </span>
                      </div>
                      <span className="text-xs sm:text-sm font-extrabold text-slate-900 font-tabular shrink-0 text-right">
                        {formatCurrency(slice.amount)}
                      </span>
                    </div>

                    {/* Line 2: Transaction Count (Left) + Percentage (Right) */}
                    <div className="flex items-center justify-between gap-3 text-xs text-slate-500 font-medium pl-4.5 mt-0.5">
                      <span>
                        {slice.transactionCount != null
                          ? `${slice.transactionCount} ${
                              slice.transactionCount === 1
                                ? 'transaction'
                                : 'transactions'
                            }`
                          : '—'}
                      </span>
                      <span className="font-bold text-indigo-600 font-tabular shrink-0 text-right">
                        {slice.percent.toFixed(1)}%
                      </span>
                    </div>

                    {/* Progress Bar */}
                    <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden mt-1.5">
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
      </CardContent>
    </Card>
  );
}

export default CategorySpendingChart;
