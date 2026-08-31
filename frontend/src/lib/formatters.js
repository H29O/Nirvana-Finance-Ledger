/**
 * Format numerical amount to standard currency string (INR by default)
 */
export function formatCurrency(amount, currency = 'INR', locale = 'en-IN') {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '₹0.00';
  }
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

/**
 * Format date string / object into localized human-readable date
 */
export function formatDate(dateInput, options = {}) {
  if (!dateInput) return '—';
  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return '—';

  const defaultOptions = {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    ...options,
  };

  return new Intl.DateTimeFormat('en-US', defaultOptions).format(date);
}

/**
 * Format date and time
 */
export function formatDateTime(dateInput) {
  if (!dateInput) return '—';
  const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
  if (isNaN(date.getTime())) return '—';

  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

/**
 * Format compact numbers (e.g. 1.2k, 4.5M)
 */
export function formatCompactNumber(number) {
  if (number === null || number === undefined || isNaN(number)) return '0';
  return new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(number);
}

/**
 * Format percentage with positive/negative sign
 */
export function formatPercentage(value, includeSign = true) {
  if (value === null || value === undefined || isNaN(value)) return '0.0%';
  const formatted = `${Math.abs(value).toFixed(1)}%`;
  if (!includeSign) return formatted;
  if (value > 0) return `+${formatted}`;
  if (value < 0) return `-${formatted}`;
  return formatted;
}
