import React from 'react';

/**
 * Reusable decorative SVG primitives for Nivara Money Wrapped.
 * Designed to provide Spotify Wrapped energy while keeping financial information crystal clear.
 */

export function WrappedOrb({
  color = '#7C3AED',
  size = 180,
  className = '',
  opacity = 0.25,
  blur = 40,
  style = {},
}) {
  return (
    <div
      className={`absolute rounded-full pointer-events-none transition-all duration-700 ${className}`}
      style={{
        width: size,
        height: size,
        background: `radial-gradient(circle, ${color} 0%, transparent 70%)`,
        opacity,
        filter: blur > 0 ? `blur(${blur}px)` : undefined,
        ...style,
      }}
      aria-hidden="true"
    />
  );
}

export function WrappedRing({
  size = 120,
  color = '#7C3AED',
  strokeWidth = 2,
  opacity = 0.2,
  className = '',
  style = {},
}) {
  return (
    <div
      className={`absolute pointer-events-none rounded-full border transition-all duration-700 ${className}`}
      style={{
        width: size,
        height: size,
        borderColor: color,
        borderWidth: strokeWidth,
        opacity,
        ...style,
      }}
      aria-hidden="true"
    />
  );
}

export function WrappedDotField({
  rows = 4,
  cols = 5,
  color = '#7C3AED',
  spacing = 16,
  dotSize = 3,
  opacity = 0.25,
  className = '',
  style = {},
}) {
  return (
    <div
      className={`absolute pointer-events-none grid gap-2.5 transition-all duration-500 ${className}`}
      style={{
        gridTemplateColumns: `repeat(${cols}, ${dotSize}px)`,
        gap: `${spacing}px`,
        opacity,
        ...style,
      }}
      aria-hidden="true"
    >
      {Array.from({ length: rows * cols }).map((_, i) => (
        <span
          key={i}
          className="rounded-full"
          style={{
            width: dotSize,
            height: dotSize,
            backgroundColor: color,
          }}
        />
      ))}
    </div>
  );
}

export function WrappedSpark({
  size = 16,
  color = '#FFD166',
  className = '',
  opacity = 0.8,
  style = {},
}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      className={`absolute pointer-events-none ${className}`}
      style={{ opacity, ...style }}
      aria-hidden="true"
    >
      <path
        d="M12 0L14.5 9.5L24 12L14.5 14.5L12 24L9.5 14.5L0 12L9.5 9.5L12 0Z"
        fill={color}
      />
    </svg>
  );
}

export function WrappedAccentPill({
  label,
  icon: Icon,
  color = '#7C3AED',
  bg = 'rgba(124, 58, 237, 0.1)',
  textColor = '#7C3AED',
  className = '',
}) {
  return (
    <div
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-satoshi font-bold tracking-widest uppercase shadow-xs transition-all ${className}`}
      style={{
        backgroundColor: bg,
        color: textColor,
        border: `1px solid ${color}30`,
      }}
    >
      {Icon && <Icon className="w-3.5 h-3.5" style={{ color }} />}
      <span>{label}</span>
    </div>
  );
}
