import React from 'react';
import Card, { CardTitle, CardDescription } from '../../../components/ui/Card';
import Button from '../../../components/ui/Button';
import { Tags, PlusCircle } from 'lucide-react';

export function CategoriesEmptyState({ onAddClick }) {
  return (
    <Card className="p-8 md:p-14 text-center bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
      <div className="max-w-md mx-auto space-y-5">
        <div className="mx-auto w-16 h-16 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shadow-sm">
          <Tags className="w-8 h-8" />
        </div>

        <div className="space-y-1.5">
          <CardTitle className="text-xl font-bold text-slate-900">
            No categories created yet
          </CardTitle>
          <CardDescription className="text-xs sm:text-sm text-slate-500 max-w-sm mx-auto leading-relaxed">
            Categories help you organize and understand your spending patterns across groceries, utilities, entertainment, and more.
          </CardDescription>
        </div>

        <div className="pt-2">
          <Button
            variant="primary"
            size="md"
            onClick={onAddClick}
            leftIcon={<PlusCircle className="w-4 h-4" />}
          >
            Create Your First Category
          </Button>
        </div>
      </div>
    </Card>
  );
}

export default CategoriesEmptyState;
