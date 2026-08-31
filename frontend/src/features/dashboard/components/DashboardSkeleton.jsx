import React from 'react';
import Card from '../../../components/ui/Card';

export function DashboardSkeleton() {
  return (
    <div className="space-y-6 animate-pulse" role="status" aria-label="Loading dashboard">
      {/* Header Skeleton */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-slate-200/80">
        <div className="space-y-2">
          <div className="h-8 w-44 bg-slate-200 rounded-xl" />
          <div className="h-4 w-64 bg-slate-100 rounded-lg" />
        </div>
        <div className="h-10 w-44 bg-slate-200 rounded-xl" />
      </div>

      {/* Main Financial Summary Skeleton */}
      <Card className="p-6 md:p-8 bg-white border-slate-200/80">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div className="space-y-3">
            <div className="h-4 w-32 bg-slate-100 rounded" />
            <div className="h-10 w-52 bg-slate-200 rounded-xl" />
          </div>
          <div className="grid grid-cols-3 gap-6 pt-4 lg:pt-0 border-t lg:border-t-0 lg:border-l border-slate-100 lg:pl-8">
            <div className="space-y-2">
              <div className="h-3 w-16 bg-slate-100 rounded" />
              <div className="h-6 w-24 bg-slate-200 rounded" />
            </div>
            <div className="space-y-2">
              <div className="h-3 w-16 bg-slate-100 rounded" />
              <div className="h-6 w-24 bg-slate-200 rounded" />
            </div>
            <div className="space-y-2">
              <div className="h-3 w-16 bg-slate-100 rounded" />
              <div className="h-6 w-16 bg-slate-200 rounded" />
            </div>
          </div>
        </div>
      </Card>

      {/* Main Donut Chart Skeleton */}
      <Card className="p-6 md:p-8 bg-white border-slate-200/80">
        <div className="h-5 w-48 bg-slate-200 rounded mb-6" />
        <div className="flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="w-52 h-52 rounded-full border-8 border-slate-100 bg-slate-50 shrink-0" />
          <div className="flex-1 w-full space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-10 w-full bg-slate-100 rounded-xl" />
            ))}
          </div>
        </div>
      </Card>

      {/* Recent Activity Skeleton */}
      <Card className="p-6 bg-white border-slate-200/80 space-y-4">
        <div className="h-5 w-36 bg-slate-200 rounded" />
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-12 w-full bg-slate-100 rounded-xl" />
          ))}
        </div>
      </Card>
    </div>
  );
}

export default DashboardSkeleton;
