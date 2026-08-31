import React from 'react';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/Card';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import {
  Store,
  Flame,
  CreditCard,
  Coffee,
  Calendar,
  Sparkles,
  ShoppingBag,
  TrendingUp,
  Award,
} from 'lucide-react';

export function WrappedSpendingStory({
  merchantAnalysis,
  spendingSpikes,
  smallPurchases,
}) {
  const topMerchant = merchantAnalysis?.topMerchantBySpend;
  const frequentMerchant = merchantAnalysis?.mostFrequentMerchant;
  const uniqueCount = merchantAnalysis?.uniqueMerchantsCount ?? 0;

  const peakDay = spendingSpikes?.peakSpendingDay;
  const largestTx = spendingSpikes?.largestSingleTransaction;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-1.5 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
          <Store className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Your Spending Habits & Places
          </h3>
          <p className="text-xs text-slate-500">
            The merchants, peak spending days, and micro-habits that shaped your month
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* 1. Top Merchant by Spend */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Top Merchant by Spend
              </span>
              <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-600 border border-amber-100 flex items-center justify-center">
                <Award className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-lg font-bold text-slate-900 truncate">
                {topMerchant?.merchant || 'No merchants logged'}
              </h4>
              <p className="text-2xl font-extrabold font-tabular text-slate-900">
                {topMerchant ? formatCurrency(topMerchant.totalSpent) : '—'}
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-xs text-slate-500 flex items-center justify-between mt-2">
            <span>Activity</span>
            <span className="font-semibold text-slate-700">
              {topMerchant
                ? `${topMerchant.transactionCount} ${
                    topMerchant.transactionCount === 1
                      ? 'transaction'
                      : 'transactions'
                  }`
                : '0 transactions'}
            </span>
          </div>
        </Card>

        {/* 2. Most Frequent Merchant */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Most Frequent Spot
              </span>
              <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 border border-indigo-100 flex items-center justify-center">
                <ShoppingBag className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-lg font-bold text-slate-900 truncate">
                {frequentMerchant?.merchant || 'No merchants logged'}
              </h4>
              <p className="text-2xl font-extrabold font-tabular text-slate-900">
                {frequentMerchant ? `${frequentMerchant.transactionCount} visits` : '—'}
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-xs text-slate-500 flex items-center justify-between mt-2">
            <span>Total Spent</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {frequentMerchant ? formatCurrency(frequentMerchant.totalSpent) : '—'}
            </span>
          </div>
        </Card>

        {/* 3. Peak Spending Spike Day */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Peak Spending Day
              </span>
              <div className="w-7 h-7 rounded-lg bg-rose-50 text-rose-600 border border-rose-100 flex items-center justify-center">
                <Flame className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-lg font-bold text-slate-900">
                {peakDay?.date ? formatDate(peakDay.date) : 'No peak day'}
              </h4>
              <p className="text-2xl font-extrabold font-tabular text-rose-600">
                {peakDay ? formatCurrency(peakDay.totalAmount) : '—'}
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-xs text-slate-500 flex items-center justify-between mt-2">
            <span>Transactions on this day</span>
            <span className="font-semibold text-slate-700">
              {peakDay?.transactionCount
                ? `${peakDay.transactionCount} ${
                    peakDay.transactionCount === 1
                      ? 'transaction'
                      : 'transactions'
                  }`
                : '—'}
            </span>
          </div>
        </Card>

        {/* 4. Largest Single Purchase */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Largest Single Purchase
              </span>
              <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 border border-indigo-100 flex items-center justify-center">
                <CreditCard className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-lg font-bold text-slate-900 truncate">
                {largestTx?.merchant || 'No purchase recorded'}
              </h4>
              <p className="text-2xl font-extrabold font-tabular text-slate-900">
                {largestTx ? formatCurrency(largestTx.amount) : '—'}
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-xs text-slate-500 flex items-center justify-between mt-2">
            <span className="truncate max-w-[140px]">
              {largestTx?.categoryName || 'Uncategorized'}
            </span>
            <span>
              {largestTx?.transactionDate ? formatDate(largestTx.transactionDate) : ''}
            </span>
          </div>
        </Card>

        {/* 5. Small Purchases Impact (< ₹20) */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Small Purchases Impact (≤ ₹20)
              </span>
              <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 border border-emerald-100 flex items-center justify-center">
                <Coffee className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-lg font-bold text-slate-900">
                {smallPurchases?.transactionCount
                  ? `${smallPurchases.transactionCount} micro-expenses`
                  : '0 micro-expenses'}
              </h4>
              <p className="text-2xl font-extrabold font-tabular text-slate-900">
                {smallPurchases ? formatCurrency(smallPurchases.totalAmount) : '—'}
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-xs text-slate-500 flex items-center justify-between mt-2">
            <span>Share of expenses</span>
            <span className="font-semibold text-slate-700 font-tabular">
              {smallPurchases?.percentageOfTotalExpense != null
                ? `${Number(smallPurchases.percentageOfTotalExpense).toFixed(1)}%`
                : '0%'}
            </span>
          </div>
        </Card>

        {/* 6. Merchant Diversity */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Merchant Diversity
              </span>
              <div className="w-7 h-7 rounded-lg bg-slate-50 text-slate-600 border border-slate-100 flex items-center justify-center">
                <Store className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-lg font-bold text-slate-900">
                {uniqueCount} Unique Places
              </h4>
              <p className="text-xs text-slate-500 leading-relaxed pt-1">
                You transacted with {uniqueCount} different stores and businesses during this period.
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-[11px] text-indigo-600 font-medium mt-2">
            Diverse spend distribution
          </div>
        </Card>
      </div>
    </div>
  );
}

export default WrappedSpendingStory;
