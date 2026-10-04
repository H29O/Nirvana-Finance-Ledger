import React, { useRef } from 'react';
import { ArrowRight, Sparkles } from 'lucide-react';
import { WrappedBackground } from '../decorations/WrappedBackground';
import { useWrappedAnimation } from '../../utils/useWrappedAnimation';

export function StoryIntro({ monthName, year, onNext }) {
  const containerRef = useRef(null);
  useWrappedAnimation(containerRef, [monthName, year]);

  return (
    <div
      ref={containerRef}
      className="relative flex-1 flex flex-col justify-between p-6 sm:p-8 text-left select-none overflow-hidden h-full text-[#24221F]"
    >
      <WrappedBackground mood="intro" />

      {/* Top Editorial Eyebrow */}
      <div data-animate="tag" className="relative z-10 pt-2">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold tracking-widest uppercase bg-[#FFF8E7]/90 text-[#24221F] border border-[#24221F]/15 backdrop-blur-xs shadow-2xs">
          <Sparkles className="w-3 h-3 text-[#24221F]" />
          <span>Nivara Wrapped • {year}</span>
        </div>
      </div>

      {/* Main Editorial Hero Cover */}
      <div className="relative z-10 my-auto py-4 space-y-5 max-w-sm">
        <div data-animate="headline" className="space-y-2">
          <span className="text-xs font-satoshi font-bold uppercase tracking-widest text-[#24221F]/60 block">
            Issue {monthName}
          </span>
          <h1 className="font-tanker text-4xl sm:text-5xl md:text-6xl text-[#24221F] tracking-wide uppercase leading-[0.98]">
            Your {monthName} <br />
            <span className="text-[#24221F]">
              in money.
            </span>
          </h1>
        </div>

        {/* Decorative Editorial Line */}
        <div
          data-animate="hero"
          className="w-14 h-1.5 bg-[#24221F] rounded-full shadow-xs"
        />

        <p
          data-animate="item"
          className="text-xs sm:text-sm text-[#24221F]/80 leading-relaxed font-satoshi font-normal"
        >
          Every rupee moved with purpose. Here is how your cashflow, spending
          habits, and standout moments unfolded across {monthName}.
        </p>

        <div data-animate="item" className="pt-1">
          <span className="inline-block text-[11px] text-[#24221F]/80 font-satoshi font-semibold bg-[#FFF8E7]/90 px-3 py-1 rounded-full border border-[#24221F]/15 backdrop-blur-xs shadow-2xs">
            7 Story Chapters • 1 Min Read
          </span>
        </div>
      </div>

      {/* Tap Next / Begin Indicator */}
      <div
        data-animate="footer"
        className="relative z-10 pt-4 pb-2 flex items-center justify-between border-t border-[#24221F]/15"
      >
        <span className="text-xs text-[#24221F]/60 font-satoshi">
          Tap anywhere to advance
        </span>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onNext();
          }}
          className="text-xs font-bold text-[#FFF8E7] bg-[#24221F] hover:bg-[#383531] px-3.5 py-1.5 rounded-lg transition-all shadow-md shadow-[#24221F]/20 flex items-center gap-1.5 active:scale-95"
        >
          <span>Begin Story</span>
          <ArrowRight className="w-3.5 h-3.5 text-[#F8D66D]" />
        </button>
      </div>
    </div>
  );
}

export default StoryIntro;
