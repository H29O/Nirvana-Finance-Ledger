import React from 'react';
import { Link } from 'react-router-dom';
import Card, { CardTitle, CardDescription } from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { PlusCircle, Receipt, WalletCards } from 'lucide-react';

export function DashboardEmptyState({ monthName, year }) {
  return (
    <Card className="p-8 md:p-12 text-center bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <div className="max-w-md mx-auto space-y-4">
        <div className="mx-auto w-14 h-14 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shadow-sm">
          <WalletCards className="w-7 h-7" />
        </div>

        <div className="space-y-1.5">
          <CardTitle className="text-lg font-bold text-slate-900">
            No activity in {monthName} {year}
          </CardTitle>
          <CardDescription className="text-xs text-slate-500 max-w-sm mx-auto">
            You haven&apos;t logged any income or expenses for this month yet.
            Add your first transaction to see your spending breakdown.
          </CardDescription>
        </div>

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
          <Link to="/transactions">
            <Button
              variant="primary"
              size="sm"
              leftIcon={<PlusCircle className="w-4 h-4" />}
            >
              Add Transaction
            </Button>
          </Link>

          <Link to="/receipts">
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Receipt className="w-4 h-4" />}
            >
              Upload Receipt
            </Button>
          </Link>
        </div>
      </div>
    </Card>
  );
}

export default DashboardEmptyState;
