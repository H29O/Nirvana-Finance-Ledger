import React from 'react';
import PageHeader from '../ui/PageHeader';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../ui/Card';
import Badge from '../ui/Badge';
import { Layers, CheckCircle2 } from 'lucide-react';

export function PlaceholderPage({
  title,
  subtitle,
  badge = 'Foundation Ready',
  capabilities = [],
  icon: Icon = Layers,
}) {
  return (
    <div className="space-y-6">
      <PageHeader
        title={title}
        subtitle={subtitle}
        badge={<Badge variant="info" dot>{badge}</Badge>}
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 bg-white border-slate-200/80 shadow-sm">
          <CardHeader className="p-6 pb-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600">
                <Icon className="w-5 h-5" />
              </div>
              <div>
                <CardTitle>{title} Module</CardTitle>
                <CardDescription>
                  Route shell and service contracts established
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-6 pt-0 space-y-4">
            <p className="text-sm text-slate-600 leading-relaxed">
              This area is connected to the application shell and authenticated routing infrastructure.
              Feature components and dedicated API services will be mounted here in subsequent phases.
            </p>

            {capabilities.length > 0 && (
              <div className="mt-4 pt-4 border-t border-slate-100">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3">
                  Planned Capabilities
                </h4>
                <ul className="space-y-2.5">
                  {capabilities.map((item, index) => (
                    <li key={index} className="flex items-start gap-2.5 text-xs text-slate-700">
                      <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-white border-slate-200/80 shadow-sm">
          <CardHeader className="p-6 pb-3">
            <CardTitle className="text-sm font-semibold text-slate-900">Architecture Status</CardTitle>
            <CardDescription>Backend integration specs</CardDescription>
          </CardHeader>
          <CardContent className="p-6 pt-0 space-y-3 text-xs text-slate-600">
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-400">Status:</span>
              <span className="text-emerald-600 font-medium">Foundation Active</span>
            </div>
            <div className="flex justify-between py-1.5 border-b border-slate-100">
              <span className="text-slate-400">Auth Guard:</span>
              <span className="text-slate-800 font-mono">JWT Bearer</span>
            </div>
            <div className="flex justify-between py-1.5">
              <span className="text-slate-400">Backend Port:</span>
              <span className="text-slate-800 font-mono">2906</span>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default PlaceholderPage;
