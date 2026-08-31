import React, { useState, useEffect } from 'react';
import Card from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import {
  ChevronLeft,
  ChevronRight,
  X,
  RotateCcw,
  Sparkles,
  Inbox,
} from 'lucide-react';
import { MONTH_NAMES } from '../utils/wrappedDateUtils';
import WrappedProgress from './WrappedProgress';
import StoryIntro from './stories/StoryIntro';
import StoryIncome from './stories/StoryIncome';
import StorySpending from './stories/StorySpending';
import StorySavings from './stories/StorySavings';
import StoryCategory from './stories/StoryCategory';
import StoryHighlights from './stories/StoryHighlights';
import StoryClosing from './stories/StoryClosing';

export function WrappedStory({ data, year, month, onExit }) {
  const [currentStep, setCurrentStep] = useState(0);

  const monthName = MONTH_NAMES[month] || `Month ${month}`;

  // Check if month has enough activity to tell a story
  const totalIncome = Number(data?.spendingOverview?.totalIncome || 0);
  const totalExpense = Number(data?.spendingOverview?.totalExpense || 0);
  const totalTransactions = data?.spendingOverview?.totalTransactions ?? 0;
  const hasActivity = totalIncome > 0 || totalExpense > 0 || totalTransactions > 0;

  // Build the list of active story chapters
  const steps = [
    'intro',
    'income',
    'spending',
    'savings',
    'category',
    'highlights',
    'closing',
  ];

  const totalSteps = steps.length;

  const handleNext = () => {
    if (currentStep < totalSteps - 1) {
      setCurrentStep((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const handleReplay = () => {
    setCurrentStep(0);
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'ArrowRight' || e.key === ' ') {
        e.preventDefault();
        handleNext();
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        handlePrev();
      } else if (e.key === 'Escape') {
        e.preventDefault();
        onExit();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentStep, totalSteps]);

  // If no transactions or activity logged for this month
  if (!hasActivity) {
    return (
      <div className="max-w-md mx-auto py-10 px-4 animate-fade-in text-center">
        <Card className="p-8 bg-white border border-slate-200/90 shadow-sm rounded-3xl space-y-6">
          <div className="w-16 h-16 rounded-3xl bg-slate-50 border border-slate-100 flex items-center justify-center mx-auto text-slate-400">
            <Inbox className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <span className="text-xs font-extrabold uppercase tracking-widest text-slate-400">
              {monthName} {year}
            </span>
            <h3 className="text-2xl font-bold text-slate-900">
              Not enough activity yet
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 max-w-xs mx-auto leading-relaxed">
              No transactions or earnings were recorded in {monthName} {year}. Once you have logged activity for a month, your story will populate here.
            </p>
          </div>

          <div className="pt-2">
            <Button
              variant="outline"
              size="md"
              onClick={onExit}
              className="w-full text-xs font-semibold rounded-xl border-slate-200 hover:bg-slate-50"
            >
              Browse Other Months
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  const activeChapter = steps[currentStep];

  return (
    <div className="max-w-xl mx-auto py-2 sm:py-6 px-3 sm:px-4 animate-fade-in select-none">
      <Card className="relative bg-white border border-slate-200/90 shadow-lg rounded-3xl overflow-hidden flex flex-col justify-between min-h-[560px] sm:min-h-[580px]">
        {/* Top Header Controls */}
        <div className="p-4 pb-2 space-y-3 border-b border-slate-100/70">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
            <div className="flex items-center gap-1.5 font-tabular">
              <span className="text-slate-900 font-bold">
                {currentStep + 1}
              </span>
              <span className="text-slate-400">/ {totalSteps}</span>
              <span className="text-slate-300 mx-1">•</span>
              <span className="font-semibold text-slate-700">
                {monthName} {year}
              </span>
            </div>

            <button
              type="button"
              onClick={onExit}
              className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors flex items-center gap-1 text-xs font-medium"
              title="Exit Story"
            >
              <span>Exit</span>
              <X className="w-4 h-4" />
            </button>
          </div>

          {/* Segmented Progress Bars */}
          <WrappedProgress
            totalSteps={totalSteps}
            currentIndex={currentStep}
            onStepClick={(index) => setCurrentStep(index)}
          />
        </div>

        {/* Story Body Container */}
        <div className="relative flex-1 flex flex-col justify-center">
          {/* Active Story Chapter */}
          {activeChapter === 'intro' && (
            <StoryIntro
              monthName={monthName}
              year={year}
              onNext={handleNext}
            />
          )}

          {activeChapter === 'income' && (
            <StoryIncome
              overview={data?.spendingOverview}
              monthName={monthName}
              onNext={handleNext}
            />
          )}

          {activeChapter === 'spending' && (
            <StorySpending
              overview={data?.spendingOverview}
              monthName={monthName}
              onNext={handleNext}
            />
          )}

          {activeChapter === 'savings' && (
            <StorySavings
              overview={data?.spendingOverview}
              monthName={monthName}
              onNext={handleNext}
            />
          )}

          {activeChapter === 'category' && (
            <StoryCategory
              categoryDistribution={data?.categoryDistribution}
              monthName={monthName}
              onNext={handleNext}
            />
          )}

          {activeChapter === 'highlights' && (
            <StoryHighlights
              spendingSpikes={data?.spendingSpikes}
              merchantAnalysis={data?.merchantAnalysis}
              smallPurchases={data?.smallPurchases}
              debtStatistics={data?.debtStatistics}
              monthName={monthName}
              onNext={handleNext}
            />
          )}

          {activeChapter === 'closing' && (
            <StoryClosing
              overview={data?.spendingOverview}
              categoryDistribution={data?.categoryDistribution}
              monthName={monthName}
              year={year}
              onReplay={handleReplay}
              onExit={onExit}
            />
          )}

          {/* Side Chevron Navigation for Desktop */}
          {currentStep > 0 && (
            <button
              type="button"
              onClick={handlePrev}
              aria-label="Previous slide"
              className="hidden sm:flex absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-slate-100/80 hover:bg-slate-200/90 text-slate-700 items-center justify-center shadow-sm transition-all focus:outline-none"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
          )}

          {currentStep < totalSteps - 1 && (
            <button
              type="button"
              onClick={handleNext}
              aria-label="Next slide"
              className="hidden sm:flex absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-slate-100/80 hover:bg-slate-200/90 text-slate-700 items-center justify-center shadow-sm transition-all focus:outline-none"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Bottom Bar Controls for Mobile */}
        <div className="sm:hidden p-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={handlePrev}
            disabled={currentStep === 0}
            className="text-xs"
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            <span>Prev</span>
          </Button>

          <span className="text-[11px] font-bold text-slate-400">
            {currentStep + 1} of {totalSteps}
          </span>

          <Button
            variant="ghost"
            size="sm"
            onClick={handleNext}
            disabled={currentStep === totalSteps - 1}
            className="text-xs font-bold text-indigo-600"
          >
            <span>Next</span>
            <ChevronRight className="w-4 h-4 ml-1" />
          </Button>
        </div>
      </Card>
    </div>
  );
}

export default WrappedStory;
