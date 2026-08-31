import React from 'react';
import { Sparkles, ArrowRight, Calendar } from 'lucide-react';
import Button from '../../../../components/ui/Button';

export function StoryIntro({ monthName, year, onNext }) {
  return (
    <div className="flex flex-col items-center justify-between min-h-[420px] sm:min-h-[460px] text-center p-4 sm:p-8 space-y-6">
      {/* Top Tag */}
      <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-100/80 shadow-sm animate-fade-in">
        <Sparkles className="w-4 h-4 text-indigo-600" />
        <span>Your Monthly Story</span>
      </div>

      {/* Main Title & Teaser */}
      <div className="space-y-4 max-w-lg mx-auto my-auto animate-slide-up">
        <span className="text-xs sm:text-sm font-extrabold uppercase tracking-widest text-slate-400 block">
          {monthName} {year}
        </span>
        <h2 className="text-3xl sm:text-5xl font-black tracking-tight text-slate-900 leading-[1.15]">
          Your Month in Money.
        </h2>
        <p className="text-sm sm:text-base text-slate-600 leading-relaxed max-w-md mx-auto">
          Every rupee tells a story. Let&apos;s step through what you brought in, what you spent, and how you paced yourself this month.
        </p>
      </div>

      {/* Action CTA */}
      <div className="pt-2 animate-fade-in w-full max-w-xs">
        <Button
          variant="primary"
          size="lg"
          onClick={onNext}
          className="w-full text-sm font-bold flex items-center justify-center gap-2 py-3 rounded-2xl shadow-md hover:shadow-lg transition-all"
        >
          <span>Start Story</span>
          <ArrowRight className="w-4 h-4" />
        </Button>
      </div>
    </div>
  );
}

export default StoryIntro;
