import React from 'react';
import Card from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { Sparkles, Calendar, Clock, ArrowRight, BookOpen } from 'lucide-react';
import { getIncompleteMonthInfo } from '../utils/wrappedDateUtils';

export function WrappedLocked({
  targetMonthName,
  targetYear,
  onOpenLatestCompleted,
  onBackToHub,
}) {
  const upcomingInfo = getIncompleteMonthInfo();

  return (
    <div className="max-w-lg mx-auto py-6 px-4 animate-fade-in text-center">
      <Card className="p-8 sm:p-10 bg-white border border-slate-200/90 shadow-md rounded-3xl space-y-6">
        {/* Animated Brewing Icon */}
        <div className="relative w-20 h-20 mx-auto flex items-center justify-center">
          <div className="absolute inset-0 rounded-3xl bg-indigo-50 border border-indigo-100 animate-pulse" />
          <Sparkles className="w-10 h-10 text-indigo-600 relative z-10 animate-bounce" />
        </div>

        {/* Narrative */}
        <div className="space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3.5 h-3.5" />
            <span>Coming Soon</span>
          </div>

          <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Your {targetMonthName} Wrapped is brewing.
          </h2>

          <p className="text-sm text-slate-600 leading-relaxed max-w-sm mx-auto">
            Your financial story is still being written. Come back when the month is complete to see where your money went.
          </p>
        </div>

        {/* Unlocking Schedule Box */}
        <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex items-center justify-center gap-3 text-xs font-semibold text-slate-700">
          <Calendar className="w-4 h-4 text-indigo-600" />
          <span>
            Available on {upcomingInfo.releaseDate} ({upcomingInfo.daysRemaining} days remaining)
          </span>
        </div>

        {/* Action CTAs */}
        <div className="pt-2 space-y-2.5 w-full max-w-xs mx-auto">
          {onOpenLatestCompleted && (
            <Button
              variant="primary"
              size="md"
              onClick={onOpenLatestCompleted}
              className="w-full text-xs sm:text-sm font-bold flex items-center justify-center gap-2 py-2.5 rounded-xl shadow-sm"
            >
              <BookOpen className="w-4 h-4" />
              <span>View Latest Completed Month</span>
            </Button>
          )}

          {onBackToHub && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onBackToHub}
              className="w-full text-xs font-semibold text-slate-500 hover:text-slate-800"
            >
              Back to Wrapped Hub
            </Button>
          )}
        </div>
      </Card>
    </div>
  );
}

export default WrappedLocked;
