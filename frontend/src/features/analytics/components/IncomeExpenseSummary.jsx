import React from 'react';
import Card, { CardContent } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import {
  TrendingUp,
  TrendingDown,
  PiggyBank,
  Receipt,
  ArrowUpRight,
  ArrowDownRight,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';

export function IncomeExpenseSummary({ overview }) {
  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const transactionCount = overview?.transactionCount ?? 0;

  const isSurplus = netSavings >= 0;
  const totalCashflow = totalIncome + totalExpense;

  // Calculate Savings Rate percentage
  let savingsRate = 0;
  if (totalIncome > 0) {
    savingsRate = Math.min(100, Math.max(-100, (netSavings / totalIncome) * 100));
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* Total Income Card */}
      <Card className="bg-white border-slate-200/80 p-5 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Total Income
            </span>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100 flex items-center justify-center">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="space-y-0.5">
            <h3 className="text-2xl font-bold font-tabular text-slate-900 tracking-tight">
              {formatCurrency(totalIncome)}
            </h3>
            <p className="text-[11px] text-slate-400 font-medium">
              Received in this period
            </p>
          </div>
        </div>

        {/* Proportional Cashflow Bar */}
        <div className="pt-4 border-t border-slate-100/80 mt-2">
          <div className="flex items-center justify-between text-[11px] text-slate-500 mb-1">
            <span>Cashflow Share</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {totalCashflow > 0
                ? `${((totalIncome / totalCashflow) * 100).toFixed(0)}%`
                : '0%'}
            </span>
          </div>
          <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-emerald-500 rounded-full transition-all duration-500"
              style={{
                width: `${
                  totalCashflow > 0 ? (totalIncome / totalCashflow) * 100 : 0
                }%`,
              }}
            />
          </div>
        </div>
      </Card>

      {/* Total Expenses Card */}
      <Card className="bg-white border-slate-200/80 p-5 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Total Expenses
            </span>
            <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-600 border border-rose-100 flex items-center justify-center">
              <TrendingDown className="w-4 h-4" />
            </div>
          </div>
          <div className="space-y-0.5">
            <h3 className="text-2xl font-bold font-tabular text-slate-900 tracking-tight">
              {formatCurrency(totalExpense)}
            </h3>
            <p className="text-[11px] text-slate-400 font-medium">
              Spent in this period
            </p>
          </div>
        </div>

        {/* Proportional Cashflow Bar */}
        <div className="pt-4 border-t border-slate-100/80 mt-2">
          <div className="flex items-center justify-between text-[11px] text-slate-500 mb-1">
            <span>Cashflow Share</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {totalCashflow > 0
                ? `${((totalExpense / totalCashflow) * 100).toFixed(0)}%`
                : '0%'}
            </span>
          </div>
          <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-rose-500 rounded-full transition-all duration-500"
              style={{
                width: `${
                  totalCashflow > 0 ? (totalExpense / totalCashflow) * 100 : 0
                }%`,
              }}
            />
          </div>
        </div>
      </Card>

      {/* Net Savings Card */}
      <Card className="bg-white border-slate-200/80 p-5 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Net Savings
            </span>
            <div
              className={`w-8 h-8 rounded-xl flex items-center justify-center border ${
                isSurplus
                  ? 'bg-indigo-50 text-indigo-600 border-indigo-100'
                  : 'bg-amber-50 text-amber-600 border-amber-100'
              }`}
            >
              <PiggyBank className="w-4 h-4" />
            </div>
          </div>
          <div className="space-y-0.5">
            <h3
              className={`text-2xl font-bold font-tabular tracking-tight ${
                isSurplus ? 'text-slate-900' : 'text-amber-700'
              }`}
            >
              {formatCurrency(netSavings)}
            </h3>
            <div className="flex items-center gap-1.5 mt-0.5">
              <span
                className={`inline-flex items-center gap-0.5 px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
                  isSurplus
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                }`}
              >
                {isSurplus ? (
                  <>
                    <ArrowUpRight className="w-3 h-3" />
                    <span>Surplus</span>
                  </>
                ) : (
                  <>
                    <ArrowDownRight className="w-3 h-3" />
                    <span>Deficit</span>
                  </>
                )}
              </span>
            </div>
          </div>
        </div>

        {/* Savings Rate Progress */}
        <div className="pt-4 border-t border-slate-100/80 mt-2">
          <div className="flex items-center justify-between text-[11px] text-slate-500 mb-1">
            <span>Savings Rate</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {totalIncome > 0 ? `${savingsRate.toFixed(1)}%` : '—'}
            </span>
          </div>
          <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                savingsRate >= 20
                  ? 'bg-emerald-500'
                  : savingsRate > 0
                  ? 'bg-indigo-500'
                  : 'bg-amber-500'
              }`}
              style={{ width: `${Math.max(0, Math.min(100, savingsRate))}%` }}
            />
          </div>
        </div>
      </Card>

      {/* Transaction Activity Card */}
      <Card className="bg-white border-slate-200/80 p-5 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Total Activity
            </span>
            <div className="w-8 h-8 rounded-xl bg-slate-50 text-slate-600 border border-slate-100 flex items-center justify-center">
              <Receipt className="w-4 h-4" />
            </div>
          </div>
          <div className="space-y-0.5">
            <h3 className="text-2xl font-bold font-tabular text-slate-900 tracking-tight">
              {transactionCount}
            </h3>
            <p className="text-[11px] text-slate-400 font-medium">
              Transactions recorded
            </p>
          </div>
        </div>

        {/* Average Transaction Value */}
        <div className="pt-4 border-t border-slate-100/80 mt-2">
          <div className="flex items-center justify-between text-[11px] text-slate-500 mb-1">
            <span>Avg / Entry</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {transactionCount > 0
                ? formatCurrency(totalExpense / transactionCount)
                : '—'}
            </span>
          </div>
          <div className="text-[10px] text-slate-400">
            Average expense per transaction
          </div>
        </div>
      </Card>
    </div>
  );
}

export default IncomeExpenseSummary;
