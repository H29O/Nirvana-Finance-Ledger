import React from 'react';
import Card, { CardContent } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import {
  TrendingUp,
  TrendingDown,
  PiggyBank,
  ArrowUpRight,
  ArrowDownRight,
  ShieldCheck,
  Receipt,
  Sparkles,
} from 'lucide-react';

export function WrappedOverview({ overview }) {
  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const savingsRate = Number(overview?.savingsRate || 0);
  const totalTransactions = overview?.totalTransactions ?? 0;

  const isSurplus = netSavings >= 0;
  const totalCashflow = totalIncome + totalExpense;

  return (
    <div className="space-y-4">
      {/* Hero Spotlight Banner */}
      <Card className="p-6 sm:p-8 bg-gradient-to-br from-slate-900 to-indigo-950 text-white border-none shadow-xl rounded-3xl relative overflow-hidden">
        {/* Subtle decorative circles */}
        <div className="absolute -right-16 -top-16 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -left-16 -bottom-16 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 space-y-6">
          <div className="flex items-center justify-between">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-white/10 text-indigo-200 border border-white/10 backdrop-blur-sm">
              <Sparkles className="w-3.5 h-3.5 text-indigo-300" />
              <span>Financial Overview</span>
            </span>

            <span
              className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold ${
                isSurplus
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                  : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
              }`}
            >
              {isSurplus ? (
                <>
                  <ArrowUpRight className="w-3.5 h-3.5" />
                  <span>Savings Surplus</span>
                </>
              ) : (
                <>
                  <ArrowDownRight className="w-3.5 h-3.5" />
                  <span>Deficit Month</span>
                </>
              )}
            </span>
          </div>

          {/* Big Savings Number */}
          <div className="space-y-1">
            <span className="text-xs sm:text-sm font-medium text-slate-300 uppercase tracking-wider">
              Net Savings Retained
            </span>
            <div className="flex flex-col sm:flex-row sm:items-baseline gap-2 sm:gap-4">
              <h2 className="text-3xl sm:text-5xl font-extrabold font-tabular tracking-tight">
                {formatCurrency(netSavings)}
              </h2>
              {totalIncome > 0 && (
                <span className="text-sm font-semibold text-emerald-400">
                  {savingsRate.toFixed(1)}% of income saved
                </span>
              )}
            </div>
          </div>

          {/* Income & Expense Breakdown Row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-4 border-t border-white/10">
            <div className="space-y-1">
              <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
                <TrendingUp className="w-3.5 h-3.5 text-emerald-400" />
                <span>Total Income</span>
              </span>
              <p className="text-xl sm:text-2xl font-bold font-tabular text-emerald-300">
                {formatCurrency(totalIncome)}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
                <TrendingDown className="w-3.5 h-3.5 text-rose-400" />
                <span>Total Expenses</span>
              </span>
              <p className="text-xl sm:text-2xl font-bold font-tabular text-rose-300">
                {formatCurrency(totalExpense)}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
                <Receipt className="w-3.5 h-3.5 text-indigo-300" />
                <span>Total Activity</span>
              </span>
              <p className="text-xl sm:text-2xl font-bold font-tabular text-slate-100">
                {totalTransactions} transactions
              </p>
            </div>
          </div>

          {/* Cashflow Ratio Indicator */}
          {totalCashflow > 0 && (
            <div className="space-y-2 pt-2">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span>
                  Income:{' '}
                  {((totalIncome / totalCashflow) * 100).toFixed(0)}%
                </span>
                <span>
                  Expenses:{' '}
                  {((totalExpense / totalCashflow) * 100).toFixed(0)}%
                </span>
              </div>
              <div className="w-full h-2.5 bg-white/10 rounded-full overflow-hidden flex">
                <div
                  className="bg-emerald-400 transition-all duration-700"
                  style={{
                    width: `${(totalIncome / totalCashflow) * 100}%`,
                  }}
                />
                <div
                  className="bg-rose-400 transition-all duration-700"
                  style={{
                    width: `${(totalExpense / totalCashflow) * 100}%`,
                  }}
                />
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}

export default WrappedOverview;
