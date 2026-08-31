import React from 'react';
import Card from '../../../components/ui/Card';
import { formatCurrency, formatPercentage } from '../../../lib/formatters';
import { ArrowUpRight, ArrowDownRight, Wallet, TrendingUp } from 'lucide-react';

export function DashboardFinancialSummary({ summary }) {
  const totalIncome = summary?.totalIncome ?? 0;
  const totalExpense = summary?.totalExpense ?? 0;
  const netSavings = summary?.netSavings ?? (totalIncome - totalExpense);
  const totalTransactions = summary?.totalTransactions ?? 0;

  const isPositiveSavings = netSavings >= 0;
  const savingsRate =
    totalIncome > 0 ? (netSavings / totalIncome) * 100 : null;

  return (
    <Card className="p-6 md:p-8 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
        {/* Main Prominent Savings Block */}
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Monthly Savings
            </span>
            {savingsRate !== null && (
              <span
                className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full ${
                  isPositiveSavings
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200/70'
                    : 'bg-rose-50 text-rose-700 border border-rose-200/70'
                }`}
              >
                {isPositiveSavings ? (
                  <TrendingUp className="w-3 h-3 text-emerald-600" />
                ) : (
                  <ArrowDownRight className="w-3 h-3 text-rose-600" />
                )}
                {formatPercentage(savingsRate)} rate
              </span>
            )}
          </div>

          <div className="flex items-baseline gap-3">
            <h2
              className={`text-3xl sm:text-4xl font-bold font-tabular tracking-tight ${
                isPositiveSavings ? 'text-slate-900' : 'text-rose-600'
              }`}
            >
              {formatCurrency(netSavings)}
            </h2>
            <span className="text-xs text-slate-400 font-medium">
              {isPositiveSavings ? 'retained this month' : 'deficit this month'}
            </span>
          </div>
        </div>

        {/* Income, Expense, and Activity Sub-Metrics */}
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-4 lg:pt-0 border-t lg:border-t-0 lg:border-l border-slate-100 lg:pl-8">
          {/* Income */}
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              <span>Income</span>
            </div>
            <div className="text-lg sm:text-xl font-bold text-slate-900 font-tabular">
              {formatCurrency(totalIncome)}
            </div>
          </div>

          {/* Expenses */}
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium">
              <span className="w-2 h-2 rounded-full bg-rose-500" />
              <span>Expenses</span>
            </div>
            <div className="text-lg sm:text-xl font-bold text-slate-900 font-tabular">
              {formatCurrency(totalExpense)}
            </div>
          </div>

          {/* Transaction Count */}
          <div className="col-span-2 sm:col-span-1 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-slate-500 font-medium">
              <Wallet className="w-3 h-3 text-slate-400" />
              <span>Transactions</span>
            </div>
            <div className="text-lg sm:text-xl font-bold text-slate-900 font-tabular">
              {totalTransactions}
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
}

export default DashboardFinancialSummary;
