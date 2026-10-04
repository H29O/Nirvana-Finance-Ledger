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
 * @param {Date} [referenceDate]
 * @returns {{ year: number, month: number }}
 */
export function getCurrentYearMonth(referenceDate = new Date()) {
  return {
    year: referenceDate.getFullYear(),
    month: referenceDate.getMonth() + 1,
  };
}

/**
 * Returns the last calendar day of a given year and month (1-12)
 * Uses native JS Date calculation without hardcoding month lengths or leap years.
 * (e.g. 2028 Feb -> 29, 2029 Feb -> 28, Apr -> 30, Oct -> 31)
 *
 * @param {number} year
 * @param {number} month (1-12)
 * @returns {number} day of month (28, 29, 30, or 31)
 */
export function getLastCalendarDay(year, month) {
  return new Date(year, month, 0).getDate();
}

/**
 * A month's Money Wrapped is released ONLY on or after the last calendar day of that month.
 * It must not be openable before the final calendar day.
 *
 * Examples:
 * - Oct 30 -> Oct Wrapped is NOT released
 * - Oct 31 -> Oct Wrapped IS released
 * - Nov 1  -> Oct Wrapped remains released (completed)
 *
 * @param {number} year
 * @param {number} month (1-12)
 * @param {Date} [referenceDate] defaults to current local Date
 * @returns {boolean}
 */
export function isWrappedReleased(year, month, referenceDate = new Date()) {
  const lastDay = getLastCalendarDay(year, month);
  // Release threshold: 00:00:00 on the last calendar day of the target month
  const targetReleaseDate = new Date(year, month - 1, lastDay, 0, 0, 0, 0);

  // Current calendar day normalized to 00:00:00 for strict calendar day comparison
  const currentCalendarDate = new Date(
    referenceDate.getFullYear(),
    referenceDate.getMonth(),
    referenceDate.getDate(),
    0, 0, 0, 0
  );

  return currentCalendarDate.getTime() >= targetReleaseDate.getTime();
}

/**
 * Get the rolling 3 months for Money Wrapped:
 * [Current Month, 1 Month Ago, 2 Months Ago]
 *
 * For example, in October 2026:
 * - October 2026 (Current, releases Oct 31)
 * - September 2026 (Completed, Available)
 * - August 2026 (Completed, Available)
 *
 * @param {Date} [referenceDate]
 * @returns {Array<{ year: number, month: number, label: string, monthName: string, shortMonthName: string, key: string, isCurrent: boolean, isReleased: boolean, isOpenable: boolean, lastDay: number, releaseDateLabel: string }>}
 */
export function getRollingWrappedMonths(referenceDate = new Date()) {
  const months = [];
  const currentYear = referenceDate.getFullYear();
  const currentMonth = referenceDate.getMonth(); // 0-indexed

  for (let offset = 0; offset < 3; offset++) {
    const d = new Date(currentYear, currentMonth - offset, 1);
    const yr = d.getFullYear();
    const mo = d.getMonth() + 1;
    const monthName = MONTH_NAMES[mo];
    const shortMonthName = MONTH_SHORT_NAMES[mo];
    const lastDay = getLastCalendarDay(yr, mo);
    const isReleased = isWrappedReleased(yr, mo, referenceDate);
    const isCurrent = offset === 0;

    months.push({
      year: yr,
      month: mo,
      monthName,
      shortMonthName,
      label: `${monthName} ${yr}`,
      key: `${yr}-${String(mo).padStart(2, '0')}`,
      isCurrent,
      isReleased,
      isOpenable: isReleased,
      lastDay,
      releaseDateLabel: `${monthName} ${lastDay}`,
    });
  }

  return months;
}

/**
 * Check if a given year and month falls strictly inside the allowed rolling 3-month window
 * @param {number} year
 * @param {number} month
 * @param {Date} [referenceDate]
 * @returns {boolean}
 */
export function isMonthInRollingWindow(year, month, referenceDate = new Date()) {
  const allowed = getRollingWrappedMonths(referenceDate);
  return allowed.some((m) => m.year === year && m.month === month);
}

/**
 * Check if a month is both in the rolling window AND released (openable).
 * @param {number} year
 * @param {number} month
 * @param {Date} [referenceDate]
 * @returns {boolean}
 */
export function isWrappedOpenable(year, month, referenceDate = new Date()) {
  return (
    isMonthInRollingWindow(year, month, referenceDate) &&
    isWrappedReleased(year, month, referenceDate)
  );
}

/**
 * Returns the most recent openable/released month in the rolling window.
 * @param {Date} [referenceDate]
 * @returns {{ year: number, month: number } | null}
 */
export function getLatestOpenableMonth(referenceDate = new Date()) {
  const months = getRollingWrappedMonths(referenceDate);
  return months.find((m) => m.isReleased) || null;
}

/**
 * Legacy helper maintained for compatibility: returns rolling months
 */
export function getAvailableCompletedMonths(referenceDate = new Date()) {
  return getRollingWrappedMonths(referenceDate);
}
