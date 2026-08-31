import React from 'react';
import { Link } from 'react-router-dom';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/Card';
import Badge from '../../../components/ui/Badge';
import { formatCurrency, formatDate } from '../../../lib/formatters';
import { ArrowLeftRight, ArrowUpRight, ArrowDownRight, ChevronRight, PlusCircle } from 'lucide-react';

export function RecentTransactionsCard({ transactions = [] }) {
  return (
    <Card className="border-slate-800">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-1.5 rounded-md bg-slate-800 text-slate-300">
              <ArrowLeftRight className="w-4 h-4" />
            </div>
            <div>
              <CardTitle className="text-sm font-semibold">Recent Activity</CardTitle>
              <CardDescription>Latest ledger transactions</CardDescription>
            </div>
          </div>

          <Link
            to="/transactions"
            className="inline-flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 font-medium transition-colors"
          >
            <span>View All</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </CardHeader>

      <CardContent>
        {transactions.length === 0 ? (
          <div className="py-8 text-center space-y-3">
            <p className="text-xs text-slate-400">No transactions recorded yet.</p>
            <Link to="/transactions">
              <button
                type="button"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 text-slate-200 hover:bg-slate-700 transition-colors"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                Add First Transaction
              </button>
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-slate-800/60">
            {transactions.map((tx) => {
              const isIncome = tx.transactionType === 'INCOME';
              const title = tx.merchant || tx.notes || tx.categoryName || 'Transaction';

              return (
                <div
                  key={tx.id}
                  className="py-3 flex items-center justify-between gap-3 first:pt-1 last:pb-1"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                        isIncome
                          ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40'
                          : 'bg-rose-950/60 text-rose-400 border border-rose-800/40'
                      }`}
                    >
                      {isIncome ? (
                        <ArrowUpRight className="w-4 h-4" />
                      ) : (
                        <ArrowDownRight className="w-4 h-4" />
                      )}
                    </div>

                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-slate-200 truncate">
                        {title}
                      </p>
                      <div className="flex items-center gap-2 text-[10px] text-slate-400 mt-0.5">
                        <span>{formatDate(tx.transactionDate)}</span>
                        {tx.categoryName && (
                          <>
                            <span>•</span>
                            <span className="text-slate-300 font-medium truncate">
                              {tx.categoryName}
                            </span>
                          </>
                        )}
                        {tx.paymentMethod && (
                          <>
                            <span>•</span>
                            <span className="text-slate-400 font-mono text-[9px] uppercase">
                              {tx.paymentMethod}
                            </span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <span
                      className={`text-xs font-bold font-tabular ${
                        isIncome ? 'text-emerald-400' : 'text-slate-100'
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

export default RecentTransactionsCard;
