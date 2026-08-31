import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { Tags } from 'lucide-react';

export function CategoriesPlaceholder() {
  return (
    <PlaceholderPage
      title="Categories"
      subtitle="Organize income streams and expenditure channels with customizable categories and color-coding."
      icon={Tags}
      capabilities={[
        'User-isolated custom categories management',
        'Distinctive icons and palette hex coding',
        'Separation of INCOME and EXPENSE categorization rules',
        'Safe deletion validation with usage check against active transactions',
      ]}
    />
  );
}

export default CategoriesPlaceholder;
