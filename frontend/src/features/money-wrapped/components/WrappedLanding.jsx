import React from 'react';
import Card from '../../../components/ui/Card';
import PageHeader from '../../../components/ui/PageHeader';
import Button from '../../../components/ui/Button';
import {
  Sparkles,
  Calendar,
  BookOpen,
  ArrowRight,
  ChevronRight,
  Star,
  Activity,
} from 'lucide-react';
import { getRollingWrappedMonths } from '../utils/wrappedDateUtils';

export function WrappedLanding({ onSelectMonth }) {
  const rollingMonths = getRollingWrappedMonths();
  const currentMonth = rollingMonths[0]; // Most recent month (current)

  return (
    <div className="space-y-8 max-w-5xl mx-auto animate-fade-in">
      {/* Top Header */}
      <PageHeader
        title="Money Wrapped"
        subtitle="Your month in money — personalized financial stories for the last 3 rolling months."
        badge={
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-100 shadow-sm">
            <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
            <span>Rolling 3-Month Window</span>
          </span>
        }
      />

      {/* Hero Featured Card for Current Month */}
      {currentMonth && (
        <div className="relative overflow-hidden p-6 sm:p-8 rounded-3xl bg-gradient-to-br from-indigo-50/90 via-slate-50 to-purple-50/60 border border-indigo-100/80 shadow-sm">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative z-10">
            <div className="space-y-2 max-w-lg">
              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-indigo-100/80 text-indigo-800 border border-indigo-200">
                <Activity className="w-3.5 h-3.5 text-indigo-600" />
                <span>Current Month</span>
              </div>

              <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                Your {currentMonth.monthName} {currentMonth.year} Story is Ready
              </h2>

              <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                Discover your income overview, top spending categories, frequent merchants, and financial highlights for the current month.
              </p>
            </div>

            <div className="shrink-0 w-full md:w-auto">
              <Button
                variant="primary"
                size="lg"
                onClick={() => onSelectMonth(currentMonth.year, currentMonth.month)}
                className="w-full md:w-auto text-xs sm:text-sm font-bold flex items-center justify-center gap-2 py-3 px-5 rounded-2xl shadow-md hover:shadow-lg transition-all"
              >
                <BookOpen className="w-4 h-4" />
                <span>Open {currentMonth.monthName} Story</span>
                <ArrowRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Available Rolling Months Section */}
      <div className="space-y-4">
        <div>
          <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Calendar className="w-4 h-4 text-indigo-600" />
            <span>Available Monthly Recaps</span>
          </h3>
          <p className="text-xs text-slate-500">
            Select any month from your rolling 3-month window to launch its story.
          </p>
        </div>

        {/* Grid of the 3 Rolling Months */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {rollingMonths.map((item, index) => {
            const isCurrent = item.isCurrent;

            return (
              <Card
                key={item.key}
                onClick={() => onSelectMonth(item.year, item.month)}
                className={`p-5 bg-white rounded-2xl border transition-all cursor-pointer hover:scale-[1.02] active:scale-[0.99] flex flex-col justify-between group ${
                  isCurrent
                    ? 'border-indigo-200 shadow-md ring-2 ring-indigo-500/10'
                    : 'border-slate-200/80 hover:border-indigo-300 shadow-sm'
                }`}
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-extrabold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2.5 py-0.5 rounded-full border border-indigo-100">
                      {item.year}
                    </span>
                    {isCurrent ? (
                      <span className="inline-flex items-center gap-1 text-[10px] font-extrabold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-full border border-indigo-200">
                        <Activity className="w-3 h-3 text-indigo-600" />
                        <span>Current</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[10px] font-extrabold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                        <Star className="w-3 h-3 text-emerald-600" />
                        <span>Completed</span>
                      </span>
                    )}
                  </div>

                  <div className="space-y-1">
                    <h4 className="text-lg font-extrabold text-slate-900 group-hover:text-indigo-600 transition-colors">
                      {item.monthName} {item.year}
                    </h4>
                    <p className="text-xs text-slate-500">
                      {isCurrent
                        ? 'Month-to-date spending, categories & habits.'
                        : 'Complete monthly recap, trends & highlights.'}
                    </p>
                  </div>
                </div>

                <div className="pt-4 mt-3 border-t border-slate-100 flex items-center justify-between text-xs font-bold text-indigo-600 group-hover:translate-x-0.5 transition-transform">
                  <span>Open Story</span>
                  <ChevronRight className="w-4 h-4" />
                </div>
              </Card>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default WrappedLanding;
