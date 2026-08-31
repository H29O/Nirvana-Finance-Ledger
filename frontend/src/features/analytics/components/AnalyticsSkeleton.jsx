import React from 'react';
import Card from '../../../components/ui/Card';

export function AnalyticsSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* 4 Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i} className="p-5 bg-white border border-slate-200/80 space-y-3">
            <div className="flex items-center justify-between">
              <div className="h-3 w-20 bg-slate-200 rounded" />
              <div className="w-8 h-8 rounded-xl bg-slate-100" />
            </div>
            <div className="h-7 w-32 bg-slate-200 rounded" />
            <div className="pt-3 border-t border-slate-100 space-y-1.5">
              <div className="h-2.5 w-24 bg-slate-100 rounded" />
              <div className="h-1.5 w-full bg-slate-100 rounded-full" />
            </div>
          </Card>
        ))}
      </div>

      {/* 2 Main Visualizations */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Donut Chart Skeleton */}
        <Card className="p-6 bg-white border border-slate-200/80 space-y-6">
          <div className="flex items-center justify-between">
            <div className="space-y-1.5">
              <div className="h-4 w-36 bg-slate-200 rounded" />
              <div className="h-3 w-48 bg-slate-100 rounded" />
            </div>
            <div className="h-7 w-28 bg-slate-100 rounded-xl" />
          </div>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-8 py-4">
            <div className="w-48 h-48 rounded-full border-8 border-slate-100 bg-slate-50" />
            <div className="w-full sm:w-1/2 space-y-3">
              {[1, 2, 3, 4].map((j) => (
                <div key={j} className="h-10 bg-slate-100 rounded-xl" />
              ))}
            </div>
          </div>
        </Card>

        {/* Trend Chart Skeleton */}
        <Card className="p-6 bg-white border border-slate-200/80 space-y-6">
          <div className="flex items-center justify-between">
            <div className="space-y-1.5">
              <div className="h-4 w-40 bg-slate-200 rounded" />
              <div className="h-3 w-52 bg-slate-100 rounded" />
            </div>
            <div className="h-6 w-32 bg-slate-100 rounded" />
          </div>
          <div className="h-56 bg-slate-50 rounded-2xl border border-slate-100 flex items-end p-4 gap-4 justify-around">
            {[40, 70, 50, 90, 60, 80].map((h, k) => (
              <div key={k} className="flex gap-1 items-end">
                <div
                  className="w-3.5 bg-slate-200 rounded-t"
                  style={{ height: `${h}%` }}
                />
                <div
                  className="w-3.5 bg-slate-100 rounded-t"
                  style={{ height: `${h * 0.7}%` }}
                />
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* Insights Row Skeleton */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((m) => (
          <Card key={m} className="p-5 bg-white border border-slate-200/80 space-y-3">
            <div className="h-3 w-24 bg-slate-200 rounded" />
            <div className="h-6 w-28 bg-slate-200 rounded" />
            <div className="h-3 w-full bg-slate-100 rounded" />
          </Card>
        ))}
      </div>
    </div>
  );
}

export default AnalyticsSkeleton;
