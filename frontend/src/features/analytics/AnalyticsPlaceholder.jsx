import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { BarChart3 } from 'lucide-react';

export function AnalyticsPlaceholder() {
  return (
    <PlaceholderPage
      title="Financial Analytics"
      subtitle="In-depth financial intelligence, periodic comparisons, category breakdowns, and monthly cash trends."
      icon={BarChart3}
      capabilities={[
        'Date-range bounded financial overview analytics',
        'Category breakdown charts for income and expenses',
        'Multi-month historical income vs expense trends',
        'Period-over-period delta comparison metrics',
      ]}
    />
  );
}

export default AnalyticsPlaceholder;
