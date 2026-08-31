import React from 'react';
import Card, { CardTitle, CardDescription } from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { Receipt, Camera, UploadCloud, Sparkles } from 'lucide-react';

export function ReceiptsEmptyState({ onScanClick }) {
  return (
    <Card className="p-8 md:p-14 text-center bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <div className="max-w-md mx-auto space-y-6">
        <div className="mx-auto w-16 h-16 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shadow-sm">
          <Receipt className="w-8 h-8" />
        </div>

        <div className="space-y-2">
          <CardTitle className="text-xl font-bold text-slate-900">
            Your Receipt Vault is empty
          </CardTitle>
          <CardDescription className="text-xs sm:text-sm text-slate-500 max-w-sm mx-auto leading-relaxed">
            Snap a photo of your paper bill or upload a payment screenshot. Our OCR will extract the merchant, amount, and date so you can log expenses in one click.
          </CardDescription>
        </div>

        <div className="grid grid-cols-2 gap-3 pt-2 text-left">
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-800">
              <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
              <span>Smart OCR</span>
            </div>
            <p className="text-[11px] text-slate-500 leading-snug">
              Auto-detects merchant, total bill amount, and date.
            </p>
          </div>

          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-100 space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-800">
              <Camera className="w-3.5 h-3.5 text-emerald-600" />
              <span>Direct Link</span>
            </div>
            <p className="text-[11px] text-slate-500 leading-snug">
              Create a new transaction or attach to an existing entry.
            </p>
          </div>
        </div>

        <div className="pt-2">
          <Button
            variant="primary"
            size="md"
            onClick={onScanClick}
            leftIcon={<UploadCloud className="w-4 h-4" />}
          >
            Scan / Upload First Receipt
          </Button>
        </div>
      </div>
    </Card>
  );
}

export default ReceiptsEmptyState;
