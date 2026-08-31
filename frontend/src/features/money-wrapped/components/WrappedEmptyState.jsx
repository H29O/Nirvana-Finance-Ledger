import React from 'react';
import Card from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { Sparkles, PlusCircle, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

const MONTH_NAMES = [
  '',
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

export function WrappedEmptyState({ year, month, onResetToCurrent }) {
  const monthName = MONTH_NAMES[month] || `Month ${month}`;

  return (
    <Card className="p-12 text-center bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] space-y-5 max-w-xl mx-auto my-8">
      <div className="w-14 h-14 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center mx-auto text-indigo-600">
        <Sparkles className="w-7 h-7" />
      </div>

      <div className="space-y-1.5">
        <h3 className="text-lg font-bold text-slate-900">
          Not Enough Activity for {monthName} {year}
        </h3>
        <p className="text-xs text-slate-500 max-w-sm mx-auto leading-relaxed">
          There are not enough financial transactions recorded in this period to construct a meaningful Wrapped retrospective. Log your daily income and expenses to unlock your personalized story!
        </p>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
        <Button
          variant="outline"
          size="sm"
          onClick={onResetToCurrent}
          className="text-xs w-full sm:w-auto"
        >
          View Current Month
        </Button>

        <Link to="/transactions" className="w-full sm:w-auto">
          <Button
            variant="primary"
            size="sm"
            className="text-xs w-full flex items-center gap-1.5 justify-center shadow-sm"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Add Transaction</span>
          </Button>
        </Link>
      </div>
    </Card>
  );
}

export default WrappedEmptyState;
