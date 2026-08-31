import React from 'react';
import { cn } from '../../lib/utils';

export function Spinner({ size = 'md', className, ...props }) {
  const sizeClasses = {
    sm: 'w-4 h-4 border-2',
    md: 'w-5 h-5 border-2',
    lg: 'w-8 h-8 border-3',
    xl: 'w-12 h-12 border-4',
  };

  return (
    <div
      role="status"
      aria-label="Loading"
      className={cn(
        'inline-block animate-spin rounded-full border-slate-200 border-t-indigo-600',
        sizeClasses[size] || sizeClasses.md,
        className
      )}
      {...props}
    >
      <span className="sr-only">Loading...</span>
    </div>
  );
}

export default Spinner;
