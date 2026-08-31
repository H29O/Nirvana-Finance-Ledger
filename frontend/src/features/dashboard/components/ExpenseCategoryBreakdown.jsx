import React from 'react';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/Card';
import { formatCurrency, formatPercentage } from '../../../lib/formatters';
import { PieChart, ArrowUpRight, ArrowDownRight } from 'lucide-react';

export function ExpenseCategoryBreakdown({ data, incomeVsExpense }) {
  const totalExpense = data?.totalExpense ?? 0;
  const categories = data?.categories || [];

  const totalIncome = incomeVsExpense?.totalIncome ?? 0;
  const netBalance = incomeVsExpense?.balance ?? (totalIncome - totalExpense);

  // Income vs expense ratio
  const maxCashflow = Math.max(totalIncome, totalExpense, 1);
  const incomePercent = Math.min(100, Math.round((totalIncome / maxCashflow) * 100));
  const expensePercent = Math.min(100, Math.round((totalExpense / maxCashflow) * 100));

  return (
    <div className="space-y-6">
      {/* Income vs Expense Comparative Flow */}
      <Card className="border-slate-800">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-sm font-semibold">Monthly Cashflow Ratio</CardTitle>
              <CardDescription>Visual comparison of total inflows against total outflows</CardDescription>
            </div>
            <span className="text-xs font-mono font-medium text-slate-400">
              {totalIncome > 0 ? `${((totalExpense / totalIncome) * 100).toFixed(0)}% burn rate` : '—'}
            </span>
          </div>
        </CardHeader>
        <CardContent className="space-y-4 pt-1">
          {/* Income Bar */}
          <div className="space-y-1.5">
            <div className="flex justify-between text-xs">
              <span className="flex items-center gap-1 text-slate-300 font-medium">
                <ArrowUpRight className="w-3.5 h-3.5 text-emerald-400" />
                Income
              </span>
              <span className="font-semibold text-emerald-400 font-tabular">
                {formatCurrency(totalIncome)}
              </span>
            </div>
            <div className="w-full h-2.5 bg-slate-800/80 rounded-full overflow-hidden">
              <div
                className="h-full bg-emerald-500 rounded-full transition-all duration-500 ease-out"
                style={{ width: `${incomePercent}%` }}
              />
            </div>
          </div>

          {/* Expense Bar */}
          <div className="space-y-1.5">
            <div className="flex justify-between text-xs">
              <span className="flex items-center gap-1 text-slate-300 font-medium">
                <ArrowDownRight className="w-3.5 h-3.5 text-rose-400" />
                Expenses
              </span>
              <span className="font-semibold text-rose-400 font-tabular">
                {formatCurrency(totalExpense)}
              </span>
            </div>
            <div className="w-full h-2.5 bg-slate-800/80 rounded-full overflow-hidden">
              <div
                className="h-full bg-rose-500 rounded-full transition-all duration-500 ease-out"
                style={{ width: `${expensePercent}%` }}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Expense by Category Breakdown */}
      <Card className="border-slate-800">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-md bg-indigo-950/60 text-indigo-400">
                <PieChart className="w-4 h-4" />
              </div>
              <div>
                <CardTitle className="text-sm font-semibold">Expense by Category</CardTitle>
                <CardDescription>Distribution of expenses across spending categories</CardDescription>
              </div>
            </div>
            <span className="text-xs font-semibold text-slate-300 font-tabular">
              Total: {formatCurrency(totalExpense)}
            </span>
          </div>
        </CardHeader>

        <CardContent>
          {categories.length === 0 ? (
            <div className="py-6 text-center text-xs text-slate-400">
              No category expenses recorded for this month.
            </div>
          ) : (
            <div className="space-y-3.5 pt-1">
              {categories.map((cat, idx) => {
                const amount = cat.totalAmount ?? 0;
                const percentage =
                  totalExpense > 0 ? (amount / totalExpense) * 100 : 0;
                const itemColor = cat.color || '#6366f1';

                return (
                  <div key={cat.categoryId || idx} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2 min-w-0">
                        <span
                          className="w-2.5 h-2.5 rounded-full shrink-0"
                          style={{ backgroundColor: itemColor }}
                        />
                        <span className="text-slate-200 font-medium truncate">
                          {cat.categoryName || 'Uncategorized'}
                        </span>
                      </div>

                      <div className="flex items-center gap-3 shrink-0">
                        <span className="text-slate-400 font-tabular text-[11px]">
                          {percentage.toFixed(1)}%
                        </span>
                        <span className="font-semibold text-slate-100 font-tabular">
                          {formatCurrency(amount)}
                        </span>
                      </div>
                    </div>

                    <div className="w-full h-1.5 bg-slate-800/80 rounded-full overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all duration-500 ease-out"
                        style={{
                          width: `${Math.min(100, Math.max(0, percentage))}%`,
                          backgroundColor: itemColor,
                        }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default ExpenseCategoryBreakdown;
