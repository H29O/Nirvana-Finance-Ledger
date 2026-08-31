import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { Receipt } from 'lucide-react';

export function ReceiptsPlaceholder() {
  return (
    <PlaceholderPage
      title="Receipt Vault & OCR"
      subtitle="Upload bills and invoices with automatic Tesseract OCR text extraction and transaction linking."
      icon={Receipt}
      capabilities={[
        'Drag-and-drop receipt image upload (up to 10MB)',
        'Server-side OCR processing with extracted text viewer',
        'Link/unlink receipts directly to ledger transactions',
        'Secure inline file viewing and storage management',
      ]}
    />
  );
}

export default ReceiptsPlaceholder;
