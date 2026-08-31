import React from 'react';
import Card, {
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from '../../../components/ui/Card';
import { formatCurrency, formatPercentage } from '../../../lib/formatters';
import {
  Lightbulb,
  ArrowUpRight,
  ArrowDownRight,
  Tag,
  Calendar,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Clock,
} from 'lucide-react';

export function AnalyticsInsights({
  comparison,
  overview,
  topCategory,
  startDate,
  endDate,
}) {
  const incomeChange = comparison?.incomeChangePercentage ?? 0;
  const expenseChange = comparison?.expenseChangePercentage ?? 0;
  const savingsChange = comparison?.savingsChangePercentage ?? 0;

  const totalExpense = Number(overview?.totalExpense || 0);

  // Calculate days in period
  let daysInPeriod = 30;
  if (startDate && endDate) {
    const s = new Date(startDate);
    const e = new Date(endDate);
    daysInPeriod = Math.max(
      1,
      Math.round((e.getTime() - s.getTime()) / (1000 * 60 * 60 * 24)) + 1
    );
  }

  const dailyAverageSpend = totalExpense / daysInPeriod;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-1.5 rounded-xl bg-amber-50 text-amber-600 border border-amber-100">
          <Lightbulb className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Financial Insights & Velocity
          </h3>
          <p className="text-xs text-slate-500">
            Key metrics and period-over-period financial trajectory
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Period Expense Trajectory */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-2">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Expense Trajectory
            </span>
            <div className="flex items-baseline gap-2">
              <span className="text-xl font-bold font-tabular text-slate-900">
                {formatPercentage(expenseChange)}
              </span>
              <span
                className={`inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                  expenseChange <= 0
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                }`}
              >
                {expenseChange <= 0 ? (
                  <>
                    <ArrowDownRight className="w-3 h-3" />
                    <span>Decreased</span>
                  </>
                ) : (
                  <>
                    <ArrowUpRight className="w-3 h-3" />
                    <span>Increased</span>
                  </>
                )}
              </span>
            </div>
          </div>

          <p className="text-xs text-slate-500 pt-3 border-t border-slate-100/80 mt-2">
            Compared to previous matching period (
            {formatCurrency(comparison?.previousPeriod?.expense || 0)})
          </p>
        </Card>

        {/* Period Income Trajectory */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-2">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Income Trajectory
            </span>
            <div className="flex items-baseline gap-2">
              <span className="text-xl font-bold font-tabular text-slate-900">
                {formatPercentage(incomeChange)}
              </span>
              <span
                className={`inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                  incomeChange >= 0
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-amber-50 text-amber-700 border-amber-200'
                }`}
              >
                {incomeChange >= 0 ? (
                  <>
                    <ArrowUpRight className="w-3 h-3" />
                    <span>Growth</span>
                  </>
                ) : (
                  <>
                    <ArrowDownRight className="w-3 h-3" />
                    <span>Reduced</span>
                  </>
                )}
              </span>
            </div>
          </div>

          <p className="text-xs text-slate-500 pt-3 border-t border-slate-100/80 mt-2">
            Compared to previous matching period (
            {formatCurrency(comparison?.previousPeriod?.income || 0)})
          </p>
        </Card>

        {/* Daily Spending Velocity */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-2">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Daily Spending Velocity
            </span>
            <div className="space-y-0.5">
              <h4 className="text-xl font-bold font-tabular text-slate-900">
                {formatCurrency(dailyAverageSpend)}
              </h4>
              <p className="text-[11px] text-slate-400">
                Average daily burn over {daysInPeriod} days
              </p>
            </div>
          </div>

          <div className="text-xs text-slate-500 pt-3 border-t border-slate-100/80 mt-2 flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-slate-400" />
            <span>Paced over active period span</span>
          </div>
        </Card>

        {/* Top Expense Category Driver */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-2">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Primary Cost Driver
            </span>
            <div className="space-y-0.5">
              <h4 className="text-xl font-bold text-slate-900 truncate">
                {topCategory ? topCategory.categoryName : 'No expenses logged'}
              </h4>
              <p className="text-[11px] text-slate-500 font-tabular">
                {topCategory
                  ? `${formatCurrency(topCategory.amount)} (${Number(
                      topCategory.percentage || 0
                    ).toFixed(1)}% of total)`
                  : '—'}
              </p>
            </div>
          </div>

          <div className="text-xs text-slate-500 pt-3 border-t border-slate-100/80 mt-2 flex items-center gap-1.5">
            <Tag className="w-3.5 h-3.5 text-indigo-500" />
            <span>Highest spending category</span>
          </div>
        </Card>
      </div>
    </div>
  );
}

export default AnalyticsInsights;
