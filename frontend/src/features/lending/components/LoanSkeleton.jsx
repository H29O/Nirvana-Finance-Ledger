import React from 'react';

export function LoanSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Overview Cards Skeleton */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="p-5 rounded-2xl bg-slate-100 border border-slate-200/60 h-28 flex flex-col justify-between"
          >
            <div className="h-4 bg-slate-200 rounded w-24" />
            <div className="h-7 bg-slate-200 rounded w-36" />
          </div>
        ))}
      </div>

      {/* Filter Tabs Skeleton */}
      <div className="flex items-center justify-between gap-4">
        <div className="h-10 bg-slate-100 rounded-xl w-64" />
        <div className="h-10 bg-slate-100 rounded-xl w-32" />
      </div>

      {/* Records Grid Skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="p-5 rounded-2xl bg-white border border-slate-200/80 shadow-sm space-y-4"
          >
            <div className="flex items-start justify-between">
              <div className="space-y-2">
                <div className="h-5 bg-slate-200 rounded w-32" />
                <div className="h-3.5 bg-slate-100 rounded w-20" />
              </div>
              <div className="h-6 bg-slate-200 rounded-full w-16" />
            </div>

            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex justify-between">
                <div className="h-3.5 bg-slate-100 rounded w-16" />
                <div className="h-4 bg-slate-200 rounded w-20" />
              </div>
              <div className="flex justify-between">
                <div className="h-3.5 bg-slate-100 rounded w-20" />
                <div className="h-4 bg-slate-200 rounded w-24" />
              </div>
              <div className="h-2 bg-slate-100 rounded-full w-full mt-2" />
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
              <div className="h-8 bg-slate-100 rounded-xl w-24" />
              <div className="h-8 bg-slate-200 rounded-xl w-24" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default LoanSkeleton;
