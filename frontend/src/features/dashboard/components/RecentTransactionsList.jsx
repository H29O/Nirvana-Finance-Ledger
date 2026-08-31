import React from 'react';
import { Link } from 'react-router-dom';
import Card, { CardContent, CardHeader, CardTitle } from '../../../components/ui/Card';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import { ArrowUpRight, ArrowDownRight, ChevronRight, PlusCircle } from 'lucide-react';

export function RecentTransactionsList({ transactions = [] }) {
  return (
    <Card className="p-6 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <CardHeader className="p-0 pb-4 flex flex-row items-center justify-between">
        <CardTitle className="text-base font-bold text-slate-900">
          Recent Transactions
        </CardTitle>

        <Link
          to="/transactions"
          className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-700 transition-colors"
        >
          <span>View all</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </Link>
      </CardHeader>

      <CardContent className="p-0">
        {transactions.length === 0 ? (
          <div className="py-8 text-center space-y-3">
            <p className="text-xs text-slate-400">No transactions recorded yet.</p>
            <Link to="/transactions">
              <button
                type="button"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                Add First Transaction
              </button>
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {transactions.map((tx) => {
              const isIncome = tx.transactionType === 'INCOME';
              const title =
                tx.merchant || tx.notes || tx.categoryName || 'Transaction';

              return (
                <div
                  key={tx.id}
                  className="py-3 flex items-center justify-between gap-3 first:pt-1 last:pb-1"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                        isIncome
                          ? 'bg-emerald-50 text-emerald-600'
                          : 'bg-rose-50 text-rose-600'
                      }`}
                    >
                      {isIncome ? (
                        <ArrowUpRight className="w-4 h-4" />
                      ) : (
                        <ArrowDownRight className="w-4 h-4" />
                      )}
                    </div>

                    <div className="min-w-0 space-y-0.5">
                      <p className="text-xs font-semibold text-slate-900 truncate">
                        {title}
                      </p>
                      <div className="flex items-center gap-2 text-[11px] text-slate-400">
                        <span>{formatDate(tx.transactionDate)}</span>
                        {tx.categoryName && (
                          <>
                            <span>•</span>
                            <span className="text-slate-600 font-medium truncate">
                              {tx.categoryName}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <span
                      className={`text-xs sm:text-sm font-bold font-tabular ${
                        isIncome ? 'text-emerald-600' : 'text-slate-900'
                      }`}
                    >
                      {isIncome ? '+' : '-'}
                      {formatCurrency(tx.amount)}
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

export default RecentTransactionsList;
