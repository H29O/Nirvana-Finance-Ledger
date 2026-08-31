import React from 'react';
import Card, { CardContent } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import { Sparkles, CheckCircle2, ShieldCheck, Heart } from 'lucide-react';

const MONTH_NAMES = [
  '',
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

export function WrappedClosing({ year, month, overview }) {
  const totalIncome = Number(overview?.totalIncome || 0);
  const totalExpense = Number(overview?.totalExpense || 0);
  const netSavings = Number(overview?.netSavings || 0);
  const savingsRate = Number(overview?.savingsRate || 0);

  const monthName = MONTH_NAMES[month] || `Month ${month}`;

  return (
    <Card className="p-6 sm:p-8 bg-slate-900 text-white rounded-3xl border-none shadow-xl text-center space-y-5 relative overflow-hidden my-4">
      {/* Decorative glow */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,_var(--tw-gradient-stops))] from-indigo-500/20 via-transparent to-transparent pointer-events-none" />

      <div className="relative z-10 space-y-4 max-w-xl mx-auto">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-white/10 text-indigo-300 border border-white/10">
          <Sparkles className="w-3.5 h-3.5" />
          <span>{monthName} {year} Retrospective</span>
        </div>

        <h3 className="text-xl sm:text-2xl font-extrabold tracking-tight text-white">
          Financial Recap Complete
        </h3>

        <div className="p-4 bg-white/5 rounded-2xl border border-white/10 grid grid-cols-1 sm:grid-cols-3 gap-4 text-center">
          <div className="space-y-0.5">
            <span className="text-[11px] text-slate-400 uppercase tracking-wider block">
              You Earned
            </span>
            <span className="text-lg font-bold font-tabular text-emerald-300">
              {formatCurrency(totalIncome)}
            </span>
          </div>

          <div className="space-y-0.5">
            <span className="text-[11px] text-slate-400 uppercase tracking-wider block">
              You Spent
            </span>
            <span className="text-lg font-bold font-tabular text-rose-300">
              {formatCurrency(totalExpense)}
            </span>
          </div>

          <div className="space-y-0.5">
            <span className="text-[11px] text-slate-400 uppercase tracking-wider block">
              You Saved
            </span>
            <span className="text-lg font-bold font-tabular text-indigo-300">
              {formatCurrency(netSavings)}
            </span>
          </div>
        </div>

        <p className="text-xs text-slate-400 leading-relaxed max-w-md mx-auto">
          {netSavings >= 0
            ? `Great discipline! You maintained a positive savings rate of ${savingsRate.toFixed(
                1
              )}% during this period.`
            : 'Awareness is the first step toward financial control. Keep tracking your expenses to build momentum!'}
        </p>
      </div>
    </Card>
  );
}

export default WrappedClosing;
