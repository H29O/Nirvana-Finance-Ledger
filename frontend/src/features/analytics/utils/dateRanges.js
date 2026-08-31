/**
 * Utility to format Date to YYYY-MM-DD string
 */
export function formatDateToIso(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Get date range for This Month (1st to last day of current month)
 */
export function getThisMonth() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    startDate: formatDateToIso(start),
    endDate: formatDateToIso(end),
    label: 'This Month',
  };
}

/**
 * Get date range for Last Month
 */
export function getLastMonth() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const end = new Date(now.getFullYear(), now.getMonth(), 0);
  return {
    startDate: formatDateToIso(start),
    endDate: formatDateToIso(end),
    label: 'Last Month',
  };
}

/**
 * Get date range for Last 3 Months
 */
export function getLast3Months() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth() - 2, 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    startDate: formatDateToIso(start),
    endDate: formatDateToIso(end),
    label: 'Last 3 Months',
  };
}

/**
 * Get date range for Last 6 Months
 */
export function getLast6Months() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth() - 5, 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    startDate: formatDateToIso(start),
    endDate: formatDateToIso(end),
    label: 'Last 6 Months',
  };
}

/**
 * Get date range for This Year (Jan 1 to Dec 31)
 */
export function getThisYear() {
  const now = new Date();
  const start = new Date(now.getFullYear(), 0, 1);
  const end = new Date(now.getFullYear(), 11, 31);
  return {
    startDate: formatDateToIso(start),
    endDate: formatDateToIso(end),
    label: 'This Year',
  };
}

/**
 * Shift the given date range by N months (for < and > month navigation)
 */
export function shiftMonthRange(currentStartDate, deltaMonths) {
  const parts = currentStartDate.split('-');
  const year = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1;

  const targetDate = new Date(year, month + deltaMonths, 1);
  const start = new Date(targetDate.getFullYear(), targetDate.getMonth(), 1);
  const end = new Date(targetDate.getFullYear(), targetDate.getMonth() + 1, 0);

  const monthName = start.toLocaleString('default', { month: 'short' });
  const label = `${monthName} ${start.getFullYear()}`;

  return {
    startDate: formatDateToIso(start),
    endDate: formatDateToIso(end),
    label,
  };
}

/**
 * Format a date range into a readable string (e.g. "1 Aug 2026 – 31 Aug 2026")
 */
export function formatRangeLabel(startDate, endDate) {
  if (!startDate || !endDate) return '';
  const s = new Date(startDate);
  const e = new Date(endDate);

  const opt = { day: 'numeric', month: 'short', year: 'numeric' };
  const sStr = s.toLocaleDateString('en-IN', opt);
  const eStr = e.toLocaleDateString('en-IN', opt);

  return `${sStr} – ${eStr}`;
}
