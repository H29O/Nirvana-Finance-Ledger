import React from 'react';
import StatCard from '../../../components/ui/StatCard';
import { formatCurrency, formatPercentage } from '../../../lib/formatters';
import { TrendingUp, TrendingDown, PiggyBank, ReceiptText } from 'lucide-react';

export function DashboardMetrics({ summary }) {
  const totalIncome = summary?.totalIncome ?? 0;
  const totalExpense = summary?.totalExpense ?? 0;
  const totalLent = summary?.totalLent ?? 0;
  const totalDebt = summary?.totalDebt ?? (summary?.totalBorrowed ?? 0);
  const netSavings = summary?.netSavings ?? (totalIncome - totalExpense + totalDebt - totalLent);
  const totalTransactions = summary?.totalTransactions ?? 0;

  // Savings rate percentage based strictly on backend numbers
  const savingsRate =
    totalIncome > 0
      ? ((netSavings / totalIncome) * 100)
      : null;

  const isNetPositive = netSavings >= 0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* Net Savings */}
      <StatCard
        title="Monthly Net Savings"
        value={formatCurrency(netSavings)}
        subtitle={isNetPositive ? 'Retained cashflow' : 'Net monthly deficit'}
        change={savingsRate !== null ? `${formatPercentage(savingsRate)} rate` : undefined}
        changeType={isNetPositive ? 'positive' : 'negative'}
        icon={
          <PiggyBank
            className={`w-5 h-5 ${
              isNetPositive ? 'text-emerald-400' : 'text-rose-400'
            }`}
          />
        }
      />

      {/* Total Income */}
      <StatCard
        title="Total Income"
        value={formatCurrency(totalIncome)}
        subtitle="Credits & inflows"
        icon={<TrendingUp className="w-5 h-5 text-emerald-400" />}
      />

      {/* Total Expenses */}
      <StatCard
        title="Total Expenses"
        value={formatCurrency(totalExpense)}
        subtitle="Debits & spending"
        icon={<TrendingDown className="w-5 h-5 text-rose-400" />}
      />

      {/* Transactions Activity */}
      <StatCard
        title="Transactions Logged"
        value={totalTransactions.toString()}
        subtitle="Entries this month"
        icon={<ReceiptText className="w-5 h-5 text-indigo-400" />}
      />
    </div>
  );
}

export default DashboardMetrics;
