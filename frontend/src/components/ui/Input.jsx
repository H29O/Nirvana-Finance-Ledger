import React from 'react';
import { cn } from '../../lib/utils';

export const Input = React.forwardRef(
  (
    {
      className,
      type = 'text',
      label,
      error,
      helperText,
      leftIcon,
      rightIcon,
      id,
      disabled = false,
      required = false,
      ...props
    },
    ref
  ) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <div className="flex items-center justify-between">
            <label
              htmlFor={inputId}
              className="block text-xs font-medium text-slate-700 select-none"
            >
              {label}
              {required && <span className="text-rose-500 ml-1">*</span>}
            </label>
          </div>
        )}

        <div className="relative rounded-xl">
          {leftIcon && (
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400">
              {leftIcon}
            </div>
          )}

          <input
            ref={ref}
            id={inputId}
            type={type}
            disabled={disabled}
            required={required}
            className={cn(
              'block w-full rounded-xl border bg-white text-sm text-slate-900 placeholder-slate-400 transition-all duration-150',
              'focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500',
              'disabled:cursor-not-allowed disabled:opacity-50 disabled:bg-slate-50',
              leftIcon ? 'pl-10' : 'pl-3.5',
              rightIcon ? 'pr-10' : 'pr-3.5',
              'py-2.5',
              error
                ? 'border-rose-300 focus:ring-rose-200 focus:border-rose-500'
                : 'border-slate-200 hover:border-slate-300',
              className
            )}
            {...props}
          />

          {rightIcon && (
            <div className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-slate-400">
              {rightIcon}
            </div>
          )}
        </div>

        {error && (
          <p className="text-xs text-rose-500 flex items-center gap-1 mt-1">
            <span>{error}</span>
          </p>
        )}

        {!error && helperText && (
          <p className="text-xs text-slate-500 mt-1">{helperText}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
export default Input;
