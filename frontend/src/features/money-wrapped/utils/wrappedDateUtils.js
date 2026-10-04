export const MONTH_NAMES = [
  '',
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

export const MONTH_SHORT_NAMES = [
  '',
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

/**
 * Get current year and month based on local system time
 * @returns {{ year: number, month: number }}
 */
export function getCurrentYearMonth() {
  const now = new Date();
  return {
    year: now.getFullYear(),
    month: now.getMonth() + 1,
  };
}

/**
 * Get the rolling 3 months available for Money Wrapped:
 * [Current Month, 1 Month Ago, 2 Months Ago]
 *
 * For example, if current month is October 2026:
 * Returns October 2026, September 2026, August 2026.
 *
 * @returns {Array<{ year: number, month: number, label: string, monthName: string, shortMonthName: string, key: string, isCurrent: boolean }>}
 */
export function getRollingWrappedMonths() {
  const now = new Date();
  const months = [];

  for (let offset = 0; offset < 3; offset++) {
    // Subtract offset months from current month
    const d = new Date(now.getFullYear(), now.getMonth() - offset, 1);
    const yr = d.getFullYear();
    const mo = d.getMonth() + 1;
    const monthName = MONTH_NAMES[mo];
    const shortMonthName = MONTH_SHORT_NAMES[mo];

    months.push({
      year: yr,
      month: mo,
      monthName,
      shortMonthName,
      label: `${monthName} ${yr}`,
      key: `${yr}-${String(mo).padStart(2, '0')}`,
      isCurrent: offset === 0,
    });
  }

  return months;
}

/**
 * Check if a given year and month falls strictly inside the allowed rolling 3-month window
 * @param {number} year
 * @param {number} month
 * @returns {boolean}
 */
export function isMonthInRollingWindow(year, month) {
  const allowed = getRollingWrappedMonths();
  return allowed.some((m) => m.year === year && m.month === month);
}

/**
 * Legacy helper maintained for compatibility: returns the rolling 3 months
 */
export function getAvailableCompletedMonths() {
  return getRollingWrappedMonths();
}
