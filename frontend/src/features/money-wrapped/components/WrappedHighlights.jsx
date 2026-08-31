import React from 'react';
import Card, { CardContent } from '../../../components/ui/Card';
import { formatCurrency, formatPercentage } from '../../../lib/formatters';
import {
  Sparkles,
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
  ArrowDownRight,
  Lightbulb,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

export function WrappedHighlights({ monthOverMonth, insights = [] }) {
  const expenseChangeAmount = Number(monthOverMonth?.expenseChangeAmount || 0);
  const expenseChangePct = Number(monthOverMonth?.expenseChangePercentage || 0);
  const prevExpense = Number(monthOverMonth?.previousMonthExpense || 0);
  const trend = monthOverMonth?.trend;

  const isDecreased = expenseChangeAmount <= 0;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-1.5 rounded-xl bg-amber-50 text-amber-600 border border-amber-100">
          <Sparkles className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Highlights & Month-over-Month Velocity
          </h3>
          <p className="text-xs text-slate-500">
            How your spending compares to the previous month and key takeaways
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Month-over-Month Comparison Card */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Month-over-Month Trajectory
              </span>
              <span
                className={`inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                  isDecreased
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-rose-50 text-rose-700 border-rose-200'
                }`}
              >
                {isDecreased ? (
                  <>
                    <ArrowDownRight className="w-3 h-3" />
                    <span>Spent Less</span>
                  </>
                ) : (
                  <>
                    <ArrowUpRight className="w-3 h-3" />
                    <span>Spent More</span>
                  </>
                )}
              </span>
            </div>

            <div className="space-y-0.5">
              <h4 className="text-2xl font-bold font-tabular text-slate-900">
                {formatPercentage(expenseChangePct)}
              </h4>
              <p className="text-xs text-slate-500 font-tabular">
                {isDecreased ? 'Saved' : 'Added'}{' '}
                <strong>{formatCurrency(Math.abs(expenseChangeAmount))}</strong> compared to last month
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 text-xs text-slate-400 flex items-center justify-between mt-3 font-tabular">
            <span>Previous Month Total:</span>
            <span className="font-semibold text-slate-700">
              {formatCurrency(prevExpense)}
            </span>
          </div>
        </Card>

        {/* Backend Key Insights (Spans 2 columns) */}
        <div className="lg:col-span-2 space-y-3">
          {insights.length === 0 ? (
            <Card className="p-6 bg-white border-slate-200/80 flex items-center justify-center text-center text-xs text-slate-500 h-full">
              Keep logging transactions to unlock personalized monthly highlights and observations.
            </Card>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 h-full">
              {insights.map((insight, idx) => (
                <Card
                  key={insight.code || idx}
                  className="p-4 bg-white border-slate-200/80 shadow-[0_1px_2px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between space-y-2"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-slate-900">
                      <Lightbulb className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                      <span>{insight.title}</span>
                    </div>
                    <p className="text-xs text-slate-600 leading-relaxed">
                      {insight.message}
                    </p>
                  </div>

                  {insight.impact && (
                    <div className="pt-2 border-t border-slate-100/80">
                      <span className="text-[10px] font-semibold text-indigo-600 uppercase tracking-wider">
                        {insight.impact}
                      </span>
                    </div>
                  )}
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default WrappedHighlights;
