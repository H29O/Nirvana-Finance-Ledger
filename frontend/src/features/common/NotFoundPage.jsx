import React from 'react';
import { Link } from 'react-router-dom';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { Compass, Home } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="min-h-[70vh] flex items-center justify-center p-4">
      <Card className="max-w-md w-full text-center bg-white border-slate-200 p-8 space-y-4 shadow-sm">
        <div className="mx-auto w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
          <Compass className="w-6 h-6 animate-pulse" />
        </div>
        <CardHeader className="p-0 space-y-1">
          <CardTitle className="text-xl font-bold text-slate-900">Page Not Found</CardTitle>
          <CardDescription className="text-xs text-slate-500">
            The requested page does not exist in the Finance Ledger.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0 pt-2">
          <Link to="/">
            <Button variant="primary" size="md" leftIcon={<Home className="w-4 h-4" />}>
              Back to Dashboard
            </Button>
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}

export default NotFoundPage;
