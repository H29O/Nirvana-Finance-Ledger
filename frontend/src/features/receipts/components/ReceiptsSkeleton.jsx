import React from 'react';
import Card from '../../../components/ui/Card';

export function ReceiptsSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 animate-pulse">
      {[1, 2, 3, 4, 5, 6].map((i) => (
        <Card key={i} className="bg-white border-slate-200/80 p-5 space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-100" />
              <div className="space-y-1.5">
                <div className="h-4 w-28 bg-slate-200 rounded" />
                <div className="h-3 w-16 bg-slate-100 rounded" />
              </div>
            </div>
            <div className="h-5 w-16 bg-slate-100 rounded-full" />
          </div>

          <div className="h-28 w-full bg-slate-50 border border-slate-100 rounded-xl flex items-center justify-center">
            <div className="h-6 w-24 bg-slate-200 rounded" />
          </div>

          <div className="flex items-center justify-between pt-2 border-t border-slate-100">
            <div className="h-4 w-20 bg-slate-100 rounded" />
            <div className="h-7 w-24 bg-slate-200 rounded-lg" />
          </div>
        </Card>
      ))}
    </div>
  );
}

export default ReceiptsSkeleton;
