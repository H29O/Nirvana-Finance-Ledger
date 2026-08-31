import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { LayoutDashboard } from 'lucide-react';

export function DashboardPlaceholder() {
  return (
    <PlaceholderPage
      title="Financial Dashboard"
      subtitle="Comprehensive overview of net balance, monthly income/expense burn rates, and category distribution."
      icon={LayoutDashboard}
      capabilities={[
        'Monthly cashflow summary & net balance cards',
        'Real-time income vs expense breakdowns',
        'Top spending categories visualization',
        'Recent transaction quick feed',
      ]}
    />
  );
}

export default DashboardPlaceholder;
