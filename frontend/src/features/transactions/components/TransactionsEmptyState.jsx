import React from 'react';
import Card, { CardTitle, CardDescription } from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { ArrowLeftRight, PlusCircle, FilterX } from 'lucide-react';

export function TransactionsEmptyState({ isFiltered = false, onAddTransaction, onResetFilters }) {
  if (isFiltered) {
    return (
      <Card className="p-8 md:p-12 text-center bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        <div className="max-w-md mx-auto space-y-4">
          <div className="mx-auto w-12 h-12 rounded-2xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
            <FilterX className="w-6 h-6" />
          </div>

          <div className="space-y-1">
            <CardTitle className="text-base font-bold text-slate-900">
              No matching transactions
            </CardTitle>
            <CardDescription className="text-xs text-slate-500">
              No transactions match your currently applied filters. Try adjusting or clearing your filters.
            </CardDescription>
          </div>

          <div className="pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={onResetFilters}
              leftIcon={<FilterX className="w-4 h-4" />}
            >
              Reset Filters
            </Button>
          </div>
        </div>
      </Card>
    );
  }

  return (
    <Card className="p-8 md:p-12 text-center bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <div className="max-w-md mx-auto space-y-4">
        <div className="mx-auto w-14 h-14 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
          <ArrowLeftRight className="w-7 h-7" />
        </div>

        <div className="space-y-1">
          <CardTitle className="text-lg font-bold text-slate-900">
            No transactions yet
          </CardTitle>
          <CardDescription className="text-xs text-slate-500 max-w-sm mx-auto">
            Your ledger is currently empty. Start by logging your first expense or income entry.
          </CardDescription>
        </div>

        <div className="pt-2">
          <Button
            variant="primary"
            size="md"
            onClick={onAddTransaction}
            leftIcon={<PlusCircle className="w-4 h-4" />}
          >
            Add Your First Transaction
          </Button>
        </div>
      </div>
    </Card>
  );
}

export default TransactionsEmptyState;
