import React from 'react';
import Card, { CardContent } from '../../../components/ui/Card';
import { formatCurrency } from '../../../lib/formatters';
import { Smartphone, Banknote, CreditCard, Sparkles } from 'lucide-react';

export function WrappedPaymentStory({ paymentMethodBreakdown }) {
  const digitalExpense = Number(paymentMethodBreakdown?.digitalExpense || 0);
  const cashExpense = Number(paymentMethodBreakdown?.cashExpense || 0);
  const digitalPct = Number(paymentMethodBreakdown?.digitalPercentage || 0);
  const cashPct = Number(paymentMethodBreakdown?.cashPercentage || 0);
  const dominant = paymentMethodBreakdown?.dominantMethod || 'DIGITAL';

  const total = digitalExpense + cashExpense;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="p-1.5 rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
          <CreditCard className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Payment Mediums & Execution
          </h3>
          <p className="text-xs text-slate-500">
            Digital payments vs physical cash transactions
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Digital Payments Card */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                <Smartphone className="w-3.5 h-3.5 text-indigo-600" />
                <span>UPI / Digital Payments</span>
              </span>
              {dominant === 'DIGITAL' && (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-100">
                  Dominant Method
                </span>
              )}
            </div>

            <div className="space-y-0.5">
              <h4 className="text-2xl font-bold font-tabular text-slate-900">
                {formatCurrency(digitalExpense)}
              </h4>
              <p className="text-xs font-semibold text-indigo-600">
                {digitalPct.toFixed(1)}% of total payments
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 mt-3">
            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-indigo-600 rounded-full transition-all duration-500"
                style={{ width: `${digitalPct}%` }}
              />
            </div>
          </div>
        </Card>

        {/* Cash Payments Card */}
        <Card className="p-5 bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] hover:border-slate-300 transition-all flex flex-col justify-between">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                <Banknote className="w-3.5 h-3.5 text-emerald-600" />
                <span>Cash Transactions</span>
              </span>
              {dominant === 'CASH' && (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-100">
                  Dominant Method
                </span>
              )}
            </div>

            <div className="space-y-0.5">
              <h4 className="text-2xl font-bold font-tabular text-slate-900">
                {formatCurrency(cashExpense)}
              </h4>
              <p className="text-xs font-semibold text-emerald-600">
                {cashPct.toFixed(1)}% of total payments
              </p>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100 mt-3">
            <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-emerald-500 rounded-full transition-all duration-500"
                style={{ width: `${cashPct}%` }}
              />
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

export default WrappedPaymentStory;
