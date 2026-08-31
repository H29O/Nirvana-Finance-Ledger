import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { ArrowLeftRight } from 'lucide-react';

export function TransactionsPlaceholder() {
  return (
    <PlaceholderPage
      title="Transactions"
      subtitle="Complete ledger of financial transactions with granular filtering, pagination, and sorting."
      icon={ArrowLeftRight}
      capabilities={[
        'Full CRUD support for INCOME and EXPENSE entries',
        'Filtering by date ranges, transaction types, and payment methods (CASH, CARD, UPI, BANK_TRANSFER)',
        'Server-side paginated table with sorting by date, amount, or category',
        'Direct link to uploaded receipts and OCR data',
      ]}
    />
  );
}

export default TransactionsPlaceholder;
