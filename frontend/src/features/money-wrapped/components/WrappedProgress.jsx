import React from 'react';

export function WrappedProgress({
  totalSteps,
  currentIndex,
  onStepClick,
  accentColor = '#7C3AED',
  isDark = false,
}) {
  return (
    <div
      className="flex items-center gap-1.5 w-full select-none"
      role="tablist"
      aria-label="Story chapters"
    >
      {Array.from({ length: totalSteps }).map((_, index) => {
        const isPast = index < currentIndex;
        const isCurrent = index === currentIndex;

        return (
          <button
            key={index}
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onStepClick && onStepClick(index);
            }}
            className="flex-1 py-1.5 group cursor-pointer focus:outline-none"
            title={`Chapter ${index + 1} of ${totalSteps}`}
            aria-label={`Jump to chapter ${index + 1}`}
            aria-selected={isCurrent}
          >
            <div
              className={`h-1 w-full rounded-full overflow-hidden transition-all duration-200 group-hover:h-1.5 ${
                isDark ? 'bg-white/20' : 'bg-black/10'
              }`}
            >
              <div
                className={`h-full transition-all duration-300 ease-out rounded-full ${
                  isPast
                    ? isDark
                      ? 'bg-white/80 w-full'
                      : 'bg-slate-800 w-full'
                    : isCurrent
                    ? 'w-full shadow-xs'
                    : 'w-0'
                }`}
                style={isCurrent ? { backgroundColor: accentColor } : {}}
              />
            </div>
          </button>
        );
      })}
    </div>
  );
}

export default WrappedProgress;
