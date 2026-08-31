import React from 'react';
import Card, { CardContent } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import { Handshake, ArrowUpRight, ArrowDownRight, Scale } from 'lucide-react';

export function WrappedDebtSection({ debtStatistics }) {
  if (!debtStatistics) return null;

  const lentTotal = Number(debtStatistics.activeLentTotal || 0);
  const borrowedTotal = Number(debtStatistics.activeBorrowedTotal || 0);
  const netPosition = Number(debtStatistics.netDebtPosition || 0);
  const lentCount = debtStatistics.activeLentCount ?? 0;
  const borrowedCount = debtStatistics.activeBorrowedCount ?? 0;

  if (lentTotal === 0 && borrowedTotal === 0) return null;

  const isNetPositive = netPosition >= 0;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-1.5 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
          <Handshake className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Lending & Borrowing Ledger
          </h3>
          <p className="text-xs text-slate-500">
            Outstanding peer-to-peer receivables and payables (kept distinct from ordinary expenses)
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Money Lent */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Active Money Lent
            </span>
            <h4 className="text-2xl font-bold font-tabular text-emerald-700">
              {formatCurrency(lentTotal)}
            </h4>
            <p className="text-[11px] text-slate-500">
              {lentCount} active {lentCount === 1 ? 'record' : 'records'}
            </p>
          </div>
          <div className="text-[11px] text-emerald-600 font-semibold pt-3 border-t border-slate-100 mt-2">
            Money to be received
          </div>
        </Card>

        {/* Money Borrowed */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Active Money Borrowed
            </span>
            <h4 className="text-2xl font-bold font-tabular text-rose-700">
              {formatCurrency(borrowedTotal)}
            </h4>
            <p className="text-[11px] text-slate-500">
              {borrowedCount} active {borrowedCount === 1 ? 'record' : 'records'}
            </p>
          </div>
          <div className="text-[11px] text-rose-600 font-semibold pt-3 border-t border-slate-100 mt-2">
            Money you owe
          </div>
        </Card>

        {/* Net Debt Position */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Net Outstanding Position
            </span>
            <h4
              className={`text-2xl font-bold font-tabular ${
                isNetPositive ? 'text-indigo-700' : 'text-amber-700'
              }`}
            >
              {formatCurrency(netPosition)}
            </h4>
            <p className="text-[11px] text-slate-500">
              {isNetPositive ? 'Net positive receivable' : 'Net payable position'}
            </p>
          </div>
          <div className="text-[11px] text-slate-500 pt-3 border-t border-slate-100 mt-2 flex items-center gap-1">
            <Scale className="w-3.5 h-3.5 text-slate-400" />
            <span>Balance of peer loans</span>
          </div>
        </Card>
      </div>
    </div>
  );
}

export default WrappedDebtSection;
