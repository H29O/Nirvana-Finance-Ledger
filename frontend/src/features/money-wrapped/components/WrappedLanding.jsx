import React from 'react';
import Card from '../../../components/ui/Card';
import PageHeader from '../../../components/ui/PageHeader';
import Button from '../../../components/ui/Button';
import {
  CalendarRange,
  Calendar,
  BookOpen,
  ArrowRight,
  CheckCircle2,
  Lock,
  Clock,
} from 'lucide-react';
import { getRollingWrappedMonths } from '../utils/wrappedDateUtils';

export function WrappedLanding({ onSelectMonth }) {
  const rollingMonths = getRollingWrappedMonths();
  const currentMonth = rollingMonths[0]; // Most recent month (current)
  const latestOpenableMonth = rollingMonths.find((m) => m.isReleased);

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Top Header */}
      <PageHeader
        title="Money Wrapped"
        subtitle="Your month in money — personalized financial stories for your rolling 3-month window."
        badge={
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200/80">
            <CalendarRange className="w-3.5 h-3.5 text-slate-500" />
            <span>Rolling 3-Month Window</span>
          </span>
        }
      />

      {/* Feature Card */}
      {currentMonth && (
        <div
          className={`p-6 sm:p-7 rounded-2xl border transition-all ${
            currentMonth.isReleased
              ? 'border-indigo-100 bg-gradient-to-b from-indigo-50/40 via-white to-white shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]'
              : 'border-amber-100 bg-gradient-to-b from-amber-50/30 via-white to-white shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]'
          }`}
        >
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="space-y-2 max-w-lg">
              {currentMonth.isReleased ? (
                <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/70">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse" />
                  <span>Available Now • {currentMonth.monthName}</span>
                </div>
              ) : (
                <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-800 border border-amber-200/70">
                  <Clock className="w-3 h-3 text-amber-600" />
                  <span>Releases on {currentMonth.releaseDateLabel}</span>
                </div>
              )}

              <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 leading-tight">
                {currentMonth.isReleased
                  ? `${currentMonth.monthName} was your money story.`
                  : `${currentMonth.monthName} Story unlocks on ${currentMonth.releaseDateLabel}.`}
              </h2>

              <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                {currentMonth.isReleased
                  ? `Discover your income overview, top spending categories, frequent merchants, and financial habits for ${currentMonth.monthName}.`
                  : `Your full ${currentMonth.monthName} Money Wrapped recap unlocks on the final calendar day once all monthly activity settles.`}
              </p>
            </div>

            <div className="shrink-0 w-full md:w-auto flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5">
              {currentMonth.isReleased ? (
                <Button
                  variant="primary"
                  size="md"
                  onClick={() => onSelectMonth(currentMonth.year, currentMonth.month)}
                  leftIcon={<BookOpen className="w-4 h-4" />}
                  rightIcon={<ArrowRight className="w-4 h-4 ml-0.5" />}
                  className="w-full md:w-auto px-5 py-2.5 rounded-xl font-semibold text-xs sm:text-sm shadow-sm"
                >
                  <span>Open {currentMonth.monthName} Story</span>
                </Button>
              ) : latestOpenableMonth ? (
                <Button
                  variant="primary"
                  size="md"
                  onClick={() => onSelectMonth(latestOpenableMonth.year, latestOpenableMonth.month)}
                  leftIcon={<BookOpen className="w-4 h-4" />}
                  rightIcon={<ArrowRight className="w-4 h-4 ml-0.5" />}
                  className="w-full md:w-auto px-5 py-2.5 rounded-xl font-semibold text-xs sm:text-sm shadow-sm"
                >
                  <span>Open {latestOpenableMonth.monthName} Story</span>
                </Button>
              ) : (
                <Button
                  variant="outline"
                  size="md"
                  disabled
                  leftIcon={<Lock className="w-4 h-4" />}
                  className="w-full md:w-auto px-5 py-2.5 rounded-xl font-semibold text-xs sm:text-sm opacity-60"
                >
                  <span>Available on {currentMonth.releaseDateLabel}</span>
                </Button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Available Rolling Months Section */}
      <div className="space-y-3 pt-2">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-slate-500" />
              <span>Available Monthly Recaps</span>
            </h3>
            <p className="text-xs text-slate-500">
              Select any released month within the rolling 3-month retention window to launch its story.
            </p>
          </div>
        </div>

        {/* Cohesive 3-Card Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {rollingMonths.map((item) => {
            const isReleased = item.isReleased;

            return (
              <Card
                key={item.key}
                onClick={isReleased ? () => onSelectMonth(item.year, item.month) : undefined}
                className={`p-5 rounded-2xl border transition-all flex flex-col justify-between ${
                  isReleased
                    ? 'bg-white border-slate-200/80 hover:border-slate-300 hover:shadow-sm cursor-pointer group'
                    : 'bg-slate-50/60 border-slate-200/60 opacity-80 cursor-default'
                }`}
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
                      {item.year}
                    </span>
                    {isReleased ? (
                      <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200/70">
                        <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                        <span>{item.isCurrent ? 'Available Now' : 'Completed'}</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-800 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200/70">
                        <Clock className="w-3 h-3 text-amber-600" />
                        <span>Releases {item.releaseDateLabel}</span>
                      </span>
                    )}
                  </div>

                  <div className="space-y-1">
                    <h4
                      className={`text-base font-bold tracking-tight ${
                        isReleased
                          ? 'text-slate-900 group-hover:text-indigo-600 transition-colors'
                          : 'text-slate-700'
                      }`}
                    >
                      {item.monthName} {item.year}
                    </h4>
                    <p className="text-xs text-slate-500 leading-relaxed font-normal">
                      {isReleased
                        ? item.isCurrent
                          ? 'Complete monthly recap, trends & top merchants.'
                          : 'Full monthly recap, trends & top merchants.'
                        : `Unlocks on the final calendar day (${item.releaseDateLabel}) after monthly activity settles.`}
                    </p>
                  </div>
                </div>

                <div
                  className={`pt-3 mt-4 border-t border-slate-100 flex items-center justify-between text-xs font-semibold ${
                    isReleased
                      ? 'text-slate-600 group-hover:text-indigo-600 transition-colors'
                      : 'text-slate-400'
                  }`}
                >
                  {isReleased ? (
                    <>
                      <span>View Story</span>
                      <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
                    </>
                  ) : (
                    <>
                      <span className="flex items-center gap-1.5 text-slate-500 font-medium">
                        <Lock className="w-3.5 h-3.5 text-slate-400" />
                        <span>Available on {item.releaseDateLabel}</span>
                      </span>
                      <span className="text-[10px] font-semibold text-amber-800/80 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200/60">
                        Upcoming
                      </span>
                    </>
                  )}
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
