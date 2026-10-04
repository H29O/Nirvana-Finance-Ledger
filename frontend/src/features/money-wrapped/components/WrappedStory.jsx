import React, { useState, useEffect } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  X,
  Inbox,
} from 'lucide-react';
import { MONTH_NAMES } from '../utils/wrappedDateUtils';
import WrappedProgress from './WrappedProgress';
import StoryIntro from './stories/StoryIntro';
import StoryOverview from './stories/StoryOverview';
import StorySpending from './stories/StorySpending';
import StoryCategory from './stories/StoryCategory';
import StoryMerchants from './stories/StoryMerchants';
import StoryHighlights from './stories/StoryHighlights';
import StoryClosing from './stories/StoryClosing';

const CHAPTER_CONFIG = [
  { step: 'intro', mood: 'intro', isDark: false, accentColor: '#24221F', canvasBg: 'bg-[#FDEAA8]', headerBg: 'bg-[#FDEAA8]/90' },
  { step: 'overview', mood: 'overview', isDark: false, accentColor: '#7C3AED', canvasBg: 'bg-[#FFF8EC]', headerBg: 'bg-[#FFF8EC]/90' },
  { step: 'spending', mood: 'spending', isDark: false, accentColor: '#FF6B6B', canvasBg: 'bg-[#FFF8EC]', headerBg: 'bg-[#FFF8EC]/90' },
  { step: 'category', mood: 'category', isDark: false, accentColor: '#FFD166', canvasBg: 'bg-[#FFF8EC]', headerBg: 'bg-[#FFF8EC]/90' },
  { step: 'merchants', mood: 'merchants', isDark: false, accentColor: '#174C3C', canvasBg: 'bg-[#FED7C3]', headerBg: 'bg-[#FED7C3]/90' },
  { step: 'highlights', mood: 'highlights', isDark: false, accentColor: '#38BDF8', canvasBg: 'bg-[#FFF8EC]', headerBg: 'bg-[#FFF8EC]/90' },
  { step: 'closing', mood: 'closing', isDark: false, accentColor: '#F05A47', canvasBg: 'bg-[#FFF8E7]', headerBg: 'bg-[#FFF8E7]/90' },
];

export function WrappedStory({ data, year, month, onExit }) {
  const [currentStep, setCurrentStep] = useState(0);

  const monthName = MONTH_NAMES[month] || `Month ${month}`;

  // Check if month has activity to tell a story
  const totalIncome = Number(data?.spendingOverview?.totalIncome || 0);
  const totalExpense = Number(data?.spendingOverview?.totalExpense || 0);
  const totalTransactions = data?.spendingOverview?.totalTransactions ?? 0;
  const hasActivity = totalIncome > 0 || totalExpense > 0 || totalTransactions > 0;

  const totalSteps = CHAPTER_CONFIG.length;
  const currentConfig = CHAPTER_CONFIG[currentStep] || CHAPTER_CONFIG[0];

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

  // Tap-to-advance on the story canvas
  const handleCanvasClick = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const width = rect.width;

    if (clickX < width * 0.3) {
      handlePrev();
    } else {
      handleNext();
    }
  };

  // Keyboard controls
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
      <div className="max-w-md mx-auto py-12 px-4 text-center">
        <div className="p-8 bg-white border border-stone-200/90 shadow-xl rounded-3xl space-y-6">
          <div className="w-14 h-14 rounded-2xl bg-stone-100 border border-stone-200 flex items-center justify-center mx-auto text-stone-400">
            <Inbox className="w-7 h-7" />
          </div>

          <div className="space-y-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 font-satoshi">
              {monthName} {year}
            </span>
            <h3 className="text-xl font-bold text-slate-900">
              Not enough activity yet
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 max-w-xs mx-auto leading-relaxed">
              No transactions or earnings were recorded in {monthName} {year}. Once you log activity for this month, your story will appear here.
            </p>
          </div>

          <div className="pt-2">
            <button
              type="button"
              onClick={onExit}
              className="w-full py-2.5 px-4 rounded-xl border border-stone-200 text-xs font-semibold text-slate-700 hover:bg-stone-50 transition-colors"
            >
              Browse Other Months
            </button>
          </div>
        </div>
      </div>
    );
  }

  const activeChapter = currentConfig.step;

  return (
    <div className="story-typography max-w-md sm:max-w-md mx-auto py-2 sm:py-6 px-3 select-none">
      {/* Outer Story Canvas with Mood Transitions */}
      <div className="relative">
        <div
          onClick={handleCanvasClick}
          className={`relative shadow-2xl rounded-3xl overflow-hidden flex flex-col justify-between h-[666px] max-h-[calc(100dvh-1.5rem)] cursor-pointer transition-colors duration-500 border border-amber-900/10 text-slate-900 ${currentConfig.canvasBg}`}
        >
          {/* Top Story Header & Segmented Progress */}
          <div
            onClick={(e) => e.stopPropagation()}
            className={`p-4 sm:p-5 pb-2 space-y-3 backdrop-blur-xs cursor-default z-20 shrink-0 transition-colors duration-500 ${currentConfig.headerBg}`}
          >
            {/* Segmented Story Progress Bar */}
            <WrappedProgress
              totalSteps={totalSteps}
              currentIndex={currentStep}
              onStepClick={(index) => setCurrentStep(index)}
              accentColor={currentConfig.accentColor}
              isDark={currentConfig.isDark}
            />

            <div className="flex items-center justify-between text-xs font-semibold pt-1">
              <div className="flex items-center gap-1.5 font-satoshi text-[11px]">
                <span className="font-bold text-slate-900">
                  {currentStep + 1}
                </span>
                <span className="text-slate-400">
                  / {totalSteps}
                </span>
                <span className="mx-1 text-slate-300">
                  •
                </span>
                <span className="font-semibold uppercase tracking-wider text-slate-700">
                  {monthName} {year}
                </span>
              </div>

              <button
                type="button"
                onClick={onExit}
                className="p-1 px-2 rounded-lg transition-colors flex items-center gap-1 text-xs font-medium focus:outline-none focus:ring-2 text-slate-500 hover:text-slate-900 hover:bg-black/5 focus:ring-slate-400/20"
                title="Exit Story (Esc)"
              >
                <span>Exit</span>
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Story Body Container with Animated Chapter Transitions */}
          <div
            key={currentStep}
            className="relative flex-1 flex flex-col justify-between overflow-hidden"
          >
            {activeChapter === 'intro' && (
              <StoryIntro
                monthName={monthName}
                year={year}
                onNext={handleNext}
              />
            )}

            {activeChapter === 'overview' && (
              <StoryOverview
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

            {activeChapter === 'category' && (
              <StoryCategory
                categoryDistribution={data?.categoryDistribution}
                monthName={monthName}
                onNext={handleNext}
              />
            )}

            {activeChapter === 'merchants' && (
              <StoryMerchants
                merchantAnalysis={data?.merchantAnalysis}
                monthName={monthName}
                onNext={handleNext}
              />
            )}

            {activeChapter === 'highlights' && (
              <StoryHighlights
                spendingSpikes={data?.spendingSpikes}
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
          </div>

          {/* Minimal Mobile Tap Navigation Hint */}
          <div
            onClick={(e) => e.stopPropagation()}
            className={`sm:hidden p-2.5 px-4 border-t flex items-center justify-between text-xs cursor-default font-satoshi transition-colors ${
              currentConfig.isDark
                ? 'bg-black/20 border-white/10 text-white/60'
                : 'bg-black/5 border-black/5 text-stone-500'
            }`}
          >
            <button
              type="button"
              onClick={handlePrev}
              disabled={currentStep === 0}
              className={`inline-flex items-center gap-1 font-semibold disabled:opacity-20 disabled:pointer-events-none p-1 ${
                currentConfig.isDark ? 'text-white' : 'text-stone-700'
              }`}
            >
              <ChevronLeft className="w-3.5 h-3.5" />
              <span>Back</span>
            </button>

            <span
              className={`text-[10px] ${
                currentConfig.isDark ? 'text-white/40' : 'text-stone-400'
              }`}
            >
              Tap sides to browse
            </span>

            <button
              type="button"
              onClick={handleNext}
              disabled={currentStep === totalSteps - 1}
              className="inline-flex items-center gap-1 font-bold disabled:opacity-20 disabled:pointer-events-none p-1"
              style={{ color: currentConfig.accentColor }}
            >
              <span>Next</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Desktop Side Chevron Controls */}
        {currentStep > 0 && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              handlePrev();
            }}
            aria-label="Previous chapter"
            className="hidden sm:flex absolute -left-12 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white/95 border border-stone-200 text-stone-700 hover:text-stone-900 hover:bg-white items-center justify-center shadow-lg transition-all hover:scale-105 active:scale-95 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
        )}

        {currentStep < totalSteps - 1 && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              handleNext();
            }}
            aria-label="Next chapter"
            className="hidden sm:flex absolute -right-12 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-white/95 border border-stone-200 text-stone-700 hover:text-stone-900 hover:bg-white items-center justify-center shadow-lg transition-all hover:scale-105 active:scale-95 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
}

export default WrappedStory;
