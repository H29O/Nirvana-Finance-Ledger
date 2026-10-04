import React from 'react';
import Card from '../../../components/ui/Card';

export function WrappedSkeleton() {
  return (
    <Card className="p-8 sm:p-12 bg-white border border-slate-200/80 shadow-sm rounded-2xl min-h-[520px] flex flex-col justify-between animate-pulse">
      {/* Top Header Skeleton */}
      <div className="space-y-3">
        <div className="flex justify-between items-center">
          <div className="h-4 w-28 bg-slate-200 rounded-full" />
          <div className="h-4 w-12 bg-slate-200 rounded-full" />
        </div>
        <div className="h-2 w-full bg-slate-100 rounded-full" />
      </div>

      {/* Center Narrative Skeleton */}
      <div className="space-y-4 max-w-sm mx-auto text-center w-full py-8">
        <div className="h-4 w-24 bg-slate-200 rounded-full mx-auto" />
        <div className="h-8 w-48 bg-slate-200 rounded-xl mx-auto" />
        <div className="h-14 w-60 bg-slate-200 rounded-2xl mx-auto" />
        <div className="h-4 w-52 bg-slate-100 rounded-full mx-auto" />
      </div>

      {/* Bottom Button Skeleton */}
      <div className="h-10 w-full max-w-xs bg-slate-200 rounded-xl mx-auto" />
    </Card>
  );
}

export default WrappedSkeleton;
