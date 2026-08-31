import React, { useState } from 'react';
import Card, {
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from '../../../components/ui/Card';
import { formatCurrency, formatCompactNumber } from '../../../lib/formatters';
import {
  TrendingUp,
  BarChart3,
  Calendar,
  Layers,
  Circle,
} from 'lucide-react';

const MONTH_NAMES = [
  '',
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

export function SpendingTrendChart({ monthlyTrend = [] }) {
  const [hoveredMonth, setHoveredMonth] = useState(null);

  const validData = (monthlyTrend || []).map((d) => ({
    ...d,
    income: Number(d.income || 0),
    expense: Number(d.expense || 0),
    netSavings: Number(d.netSavings || 0),
    monthLabel: `${MONTH_NAMES[d.month] || d.month} '${String(d.year).slice(2)}`,
  }));

  const hasActivity = validData.some(
    (d) => d.income > 0 || d.expense > 0 || d.netSavings !== 0
  );

  // SVG Chart Layout dimensions - maximizing useful internal plotting area
  const svgWidth = 680;
  const svgHeight = 260;
  const paddingLeft = 52;
  const paddingRight = 16;
  const paddingTop = 16;
  const paddingBottom = 34;

  const chartWidth = svgWidth - paddingLeft - paddingRight;
  const chartHeight = svgHeight - paddingTop - paddingBottom;

  // Compute maximum value for Y scale
  const maxVal = Math.max(
    ...validData.map((d) => Math.max(d.income, d.expense)),
    1000
  );

  // Generate 4 vertical grid intervals
  const yTicks = [0, maxVal * 0.333, maxVal * 0.666, maxVal];

  const getYCoord = (val) => {
    const clamped = Math.max(0, val);
    return paddingTop + chartHeight - (clamped / maxVal) * chartHeight;
  };

  const barGroupWidth =
    validData.length > 0 ? chartWidth / validData.length : chartWidth;
  const barWidth = Math.min(32, Math.max(16, (barGroupWidth - 20) / 2));

  return (
    <Card className="p-5 sm:p-6 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] flex flex-col justify-between">
      {/* Header */}
      <CardHeader className="p-0 pb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-100">
        <div>
          <CardTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-indigo-600" />
            <span>Monthly Financial Trend</span>
          </CardTitle>
          <CardDescription className="text-xs text-slate-500 mt-0.5">
            Month-over-month income vs expense comparison
          </CardDescription>
        </div>

        {/* Legend */}
        <div className="flex items-center gap-4 text-xs font-bold">
          <div className="flex items-center gap-1.5 text-emerald-700">
            <span className="w-3 h-3 rounded-full bg-emerald-500 shadow-sm shrink-0" />
            <span>Income</span>
          </div>
          <div className="flex items-center gap-1.5 text-rose-700">
            <span className="w-3 h-3 rounded-full bg-rose-500 shadow-sm shrink-0" />
            <span>Expenses</span>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-0 pt-4">
        {!hasActivity ? (
          <div className="py-12 text-center space-y-2">
            <div className="w-12 h-12 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-6 h-6" />
            </div>
            <p className="text-sm font-bold text-slate-700">
              No trend data available
            </p>
            <p className="text-xs text-slate-400 max-w-xs mx-auto">
              As you log transactions across months, your comparative financial trends will populate here.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {/* Responsive SVG Chart */}
            <div className="w-full overflow-x-auto">
              <svg
                viewBox={`0 0 ${svgWidth} ${svgHeight}`}
                className="w-full h-56 sm:h-64 md:h-72 select-none"
              >
                {/* Horizontal Gridlines and Y-Axis Labels */}
                {yTicks.map((tick, i) => {
                  const y = getYCoord(tick);
                  const isBaseLine = i === 0;
                  return (
                    <g key={i}>
                      <line
                        x1={paddingLeft}
                        y1={y}
                        x2={svgWidth - paddingRight}
                        y2={y}
                        stroke={isBaseLine ? '#cbd5e1' : '#e2e8f0'}
                        strokeWidth={isBaseLine ? '1.5' : '1'}
                        strokeDasharray={isBaseLine ? 'none' : '3 3'}
                      />
                      <text
                        x={paddingLeft - 8}
                        y={y + 4}
                        textAnchor="end"
                        className="text-[12px] fill-slate-600 font-tabular font-bold"
                      >
                        {formatCompactNumber(tick)}
                      </text>
                    </g>
                  );
                })}

                {/* Bars for Each Month */}
                {validData.map((d, index) => {
                  const groupCenterX =
                    paddingLeft + index * barGroupWidth + barGroupWidth / 2;
                  const incomeX = groupCenterX - barWidth - 2;
                  const expenseX = groupCenterX + 2;

                  const incomeY = getYCoord(d.income);
                  const incomeH = Math.max(0, paddingTop + chartHeight - incomeY);

                  const expenseY = getYCoord(d.expense);
                  const expenseH = Math.max(0, paddingTop + chartHeight - expenseY);

                  const isHovered = hoveredMonth === index;

                  return (
                    <g
                      key={index}
                      onMouseEnter={() => setHoveredMonth(index)}
                      onMouseLeave={() => setHoveredMonth(null)}
                      className="cursor-pointer"
                    >
                      {/* Hover Column Background Highlight */}
                      {isHovered && (
                        <rect
                          x={paddingLeft + index * barGroupWidth + 4}
                          y={paddingTop}
                          width={barGroupWidth - 8}
                          height={chartHeight}
                          fill="#f1f5f9"
                          rx="6"
                        />
                      )}

                      {/* Income Bar */}
                      <rect
                        x={incomeX}
                        y={incomeY}
                        width={barWidth}
                        height={incomeH}
                        fill={isHovered ? '#059669' : '#10b981'}
                        rx="4"
                        className="transition-all duration-150"
                      />

                      {/* Expense Bar */}
                      <rect
                        x={expenseX}
                        y={expenseY}
                        width={barWidth}
                        height={expenseH}
                        fill={isHovered ? '#e11d48' : '#f43f5e'}
                        rx="4"
                        className="transition-all duration-150"
                      />

                      {/* X-Axis Month Label */}
                      <text
                        x={groupCenterX}
                        y={svgHeight - 10}
                        textAnchor="middle"
                        className={`text-[13px] font-bold transition-colors ${
                          isHovered
                            ? 'fill-indigo-950 font-extrabold'
                            : 'fill-slate-700'
                        }`}
                      >
                        {d.monthLabel}
                      </text>
                    </g>
                  );
                })}
              </svg>
            </div>

            {/* Hover Floating Details Card */}
            {hoveredMonth !== null && validData[hoveredMonth] && (
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs sm:text-sm animate-fade-in shadow-sm">
                <div className="flex items-center gap-2.5">
                  <Calendar className="w-4 h-4 text-indigo-600" />
                  <span className="font-bold text-slate-900 text-sm">
                    {validData[hoveredMonth].monthLabel} Summary
                  </span>
                </div>

                <div className="flex items-center gap-5 font-tabular flex-wrap">
                  <span className="text-emerald-700 font-bold">
                    Income: {formatCurrency(validData[hoveredMonth].income)}
                  </span>
                  <span className="text-rose-700 font-bold">
                    Expenses: {formatCurrency(validData[hoveredMonth].expense)}
                  </span>
                  <span className="text-indigo-700 font-extrabold">
                    Net: {formatCurrency(validData[hoveredMonth].netSavings)}
                  </span>
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default SpendingTrendChart;
