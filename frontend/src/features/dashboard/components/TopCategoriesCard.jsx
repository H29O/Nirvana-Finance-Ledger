import React from 'react';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/Card';
import Badge from '../../../components/ui/Badge';
import { formatCurrency } from '../../../lib/formatters';
import { Flame } from 'lucide-react';

export function TopCategoriesCard({ categories = [] }) {
  return (
    <Card className="border-slate-800">
      <CardHeader className="pb-3">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-md bg-amber-950/60 text-amber-400">
            <Flame className="w-4 h-4" />
          </div>
          <div>
            <CardTitle className="text-sm font-semibold">Top Spending Categories</CardTitle>
            <CardDescription>Highest expense drivers this month</CardDescription>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {categories.length === 0 ? (
          <div className="py-6 text-center text-xs text-slate-400">
            No expense categories recorded.
          </div>
        ) : (
          <div className="space-y-3 pt-1">
            {categories.map((cat, idx) => {
              const itemColor = cat.color || '#6366f1';
              const count = cat.transactionCount ?? 0;

              return (
                <div
                  key={cat.categoryId || idx}
                  className="flex items-center justify-between p-2.5 rounded-lg bg-slate-900/60 border border-slate-800/60 hover:border-slate-700/80 transition-colors"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div
                      className="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0 text-white"
                      style={{ backgroundColor: itemColor }}
                    >
                      #{idx + 1}
                    </div>

                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-200 truncate">
                        {cat.categoryName || 'Uncategorized'}
                      </p>
                      <p className="text-[10px] text-slate-400">
                        {count} {count === 1 ? 'transaction' : 'transactions'}
                      </p>
                    </div>
                  </div>

                  <div className="text-right shrink-0 pl-2">
                    <span className="text-xs font-bold text-slate-100 font-tabular">
                      {formatCurrency(cat.totalAmount)}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default TopCategoriesCard;
