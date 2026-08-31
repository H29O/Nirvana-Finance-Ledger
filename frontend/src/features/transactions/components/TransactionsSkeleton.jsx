import React from 'react';
import Card from '../../../components/ui/Card';

export function TransactionsSkeleton() {
  return (
    <Card className="bg-white border-slate-200/80 p-6 space-y-4 animate-pulse">
      <div className="flex items-center justify-between pb-3 border-b border-slate-100">
        <div className="h-4 w-32 bg-slate-200 rounded" />
        <div className="h-4 w-20 bg-slate-100 rounded" />
      </div>

      <div className="space-y-3">
        {[1, 2, 3, 4, 5, 6].map((i) => (
          <div
            key={i}
            className="flex items-center justify-between py-3 border-b border-slate-100 last:border-0"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-100" />
              <div className="space-y-1.5">
                <div className="h-4 w-40 bg-slate-200 rounded" />
                <div className="h-3 w-24 bg-slate-100 rounded" />
              </div>
            </div>
            <div className="h-5 w-24 bg-slate-200 rounded" />
          </div>
        ))}
      </div>
    </Card>
  );
}

export default TransactionsSkeleton;
