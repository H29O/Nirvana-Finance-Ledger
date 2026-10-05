import React from 'react';
import { Link } from 'react-router-dom';

/**
 * Nivara bespoke SVG emblem.
 * Balanced geometric 'N' monogram with subtle clarity accent.
 */
export function NivaraIcon({ size = 'md', variant = 'contained', className = '' }) {
  const sizeMap = {
    xs: { box: 'w-6 h-6', svg: 'w-3.5 h-3.5', radius: 'rounded-lg' },
    sm: { box: 'w-8 h-8', svg: 'w-4.5 h-4.5', radius: 'rounded-xl' },
    md: { box: 'w-9 h-9', svg: 'w-5 h-5', radius: 'rounded-xl' },
    lg: { box: 'w-12 h-12', svg: 'w-7 h-7', radius: 'rounded-2xl' },
    xl: { box: 'w-14 h-14', svg: 'w-8 h-8', radius: 'rounded-2xl' },
  };

  const dim = sizeMap[size] || sizeMap.md;

  const svgContent = (
    <svg
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`${variant === 'raw' ? (dim.box) : dim.svg} shrink-0 transition-transform`}
      aria-hidden="true"
    >
      <path
        d="M8.5 23.5V8.5L23.5 23.5V8.5"
        stroke="url(#nivara-comp-grad)"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="23.5" cy="8.5" r="1.6" fill="#10b981" />
      <defs>
        <linearGradient
          id="nivara-comp-grad"
          x1="8.5"
          y1="8.5"
          x2="23.5"
          y2="23.5"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#818cf8" />
          <stop offset="65%" stopColor="#6366f1" />
          <stop offset="100%" stopColor="#10b981" />
        </linearGradient>
      </defs>
    </svg>
  );

  if (variant === 'raw') {
    return <div className={`inline-flex items-center justify-center ${className}`}>{svgContent}</div>;
  }

  return (
    <div
      className={`inline-flex items-center justify-center bg-[#090d16] border border-indigo-950/80 shadow-sm ${dim.box} ${dim.radius} ${className}`}
    >
      {svgContent}
    </div>
  );
}

/**
 * Nivara typographic wordmark.
 * Uses refined Satoshi letterforms with calibrated kerning and balanced styling.
 */
export function NivaraWordmark({ size = 'md', className = '' }) {
  const sizeMap = {
    sm: 'text-sm tracking-tight',
    md: 'text-base tracking-tight',
    lg: 'text-2xl tracking-tight',
    xl: 'text-3xl tracking-tight',
  };

  const textClass = sizeMap[size] || sizeMap.md;

  return (
    <span
      className={`font-satoshi font-bold text-slate-900 select-none inline-flex items-baseline ${textClass} ${className}`}
    >
      <span>Nivara</span>
      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 ml-0.5 inline-block opacity-90" aria-hidden="true" />
    </span>
  );
}

/**
 * Combined Nivara branding (Emblem + Wordmark + optional Subtitle).
 * Can be rendered horizontally (navbar/sidebar) or vertically (auth screens).
 */
export function NivaraBrand({
  size = 'md',
  layout = 'horizontal',
  subtitle,
  to,
  className = '',
}) {
  const content = (
    <div
      className={`group inline-flex ${
        layout === 'vertical'
          ? 'flex-col items-center text-center space-y-3'
          : 'items-center gap-2.5'
      } ${className}`}
    >
      <NivaraIcon size={size} />

      <div className={layout === 'vertical' ? 'space-y-0.5' : 'min-w-0'}>
        <NivaraWordmark size={size} />
        {subtitle && (
          <p
            className={`text-slate-400 font-medium ${
              size === 'lg' || size === 'xl' ? 'text-xs' : 'text-[11px]'
            }`}
          >
            {subtitle}
          </p>
        )}
      </div>
    </div>
  );

  if (to) {
    return (
      <Link to={to} className="inline-flex focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded-xl">
        {content}
      </Link>
    );
  }

  return content;
}

export default NivaraBrand;
