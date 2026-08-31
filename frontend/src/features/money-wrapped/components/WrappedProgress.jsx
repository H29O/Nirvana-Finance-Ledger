import React from 'react';

export function WrappedProgress({ totalSteps, currentIndex, onStepClick }) {
  return (
    <div className="flex items-center gap-1.5 w-full px-2">
      {Array.from({ length: totalSteps }).map((_, index) => {
        const isPast = index < currentIndex;
        const isCurrent = index === currentIndex;

        return (
          <button
            key={index}
            type="button"
            onClick={() => onStepClick && onStepClick(index)}
            className="flex-1 h-1.5 rounded-full overflow-hidden bg-slate-200/80 transition-all cursor-pointer focus:outline-none"
            title={`Step ${index + 1} of ${totalSteps}`}
            aria-label={`Go to step ${index + 1}`}
          >
            <div
              className={`h-full transition-all duration-300 ${
                isPast || isCurrent ? 'bg-indigo-600' : 'bg-transparent'
              }`}
              style={{
                width: isPast || isCurrent ? '100%' : '0%',
              }}
            />
          </button>
        );
      })}
    </div>
  );
}

export default WrappedProgress;
