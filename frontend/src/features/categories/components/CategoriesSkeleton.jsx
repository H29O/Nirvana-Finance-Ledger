import React from 'react';
import Card from '../../../components/ui/Card';

export function CategoriesSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 animate-pulse">
      {[1, 2, 3, 4, 5, 6].map((i) => (
        <Card key={i} className="bg-white border-slate-200/80 p-5 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-100" />
              <div className="space-y-1.5">
                <div className="h-4 w-28 bg-slate-200 rounded" />
                <div className="h-3 w-16 bg-slate-100 rounded" />
              </div>
            </div>
            <div className="h-6 w-12 bg-slate-100 rounded-lg" />
          </div>
          <div className="h-3 w-full bg-slate-50 rounded" />
        </Card>
      ))}
    </div>
  );
}

export default CategoriesSkeleton;
