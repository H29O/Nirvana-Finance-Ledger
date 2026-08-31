import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { Sparkles } from 'lucide-react';

export function MoneyWrappedPlaceholder() {
  return (
    <PlaceholderPage
      title="Money Wrapped"
      subtitle="Interactive narrative recap of your monthly and yearly spending habits, biggest saves, and financial milestones."
      icon={Sparkles}
      capabilities={[
        'Interactive slide-based monthly financial recap',
        'Top spending spikes and biggest single transactions',
        'Highest spend days and unique spending streaks',
        'Savings rate and personalized spending personality badges',
      ]}
    />
  );
}

export default MoneyWrappedPlaceholder;
