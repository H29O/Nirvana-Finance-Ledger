import React from 'react';
import { cn } from '../../lib/utils';
import Card from './Card';

export function StatCard({
  title,
  value,
  subtitle,
  change,
  changeType = 'neutral',
  icon,
  className,
  ...props
}) {
  const changeColors = {
    positive: 'text-emerald-600',
    negative: 'text-rose-600',
    neutral: 'text-slate-500',
  };

  return (
    <Card className={cn('p-5 flex flex-col justify-between hover:border-slate-300 transition-colors', className)} {...props}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-semibold text-slate-500 tracking-wide uppercase">
          {title}
        </span>
        {icon && (
          <div className="p-2 rounded-xl bg-slate-50 text-slate-600 border border-slate-100 shrink-0">
            {icon}
          </div>
        )}
      </div>

      <div className="mt-3 space-y-1">
        <div className="text-2xl font-bold tracking-tight text-slate-900 font-tabular">
          {value}
        </div>
        {(change || subtitle) && (
          <div className="flex items-center gap-2 text-xs">
            {change && (
              <span className={cn('font-semibold font-tabular', changeColors[changeType])}>
                {change}
              </span>
            )}
            {subtitle && <span className="text-slate-500">{subtitle}</span>}
          </div>
        )}
      </div>
    </Card>
  );
}

export default StatCard;
