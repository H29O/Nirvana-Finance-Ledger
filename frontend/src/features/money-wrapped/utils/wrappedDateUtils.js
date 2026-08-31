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
 * Check if a given year and month is strictly completed (i.e. strictly before current month)
 * @param {number} year
 * @param {number} month
 * @returns {boolean}
 */
export function isMonthCompleted(year, month) {
  const current = getCurrentYearMonth();
  if (year < current.year) return true;
  if (year === current.year && month < current.month) return true;
  return false;
}

/**
 * Get a list of recent completed months for the user to browse
 * @param {number} [limit=12]
 * @returns {Array<{ year: number, month: number, label: string, monthName: string, key: string }>}
 */
export function getAvailableCompletedMonths(limit = 12) {
  const current = getCurrentYearMonth();
  const months = [];

  let curYear = current.year;
  let curMonth = current.month - 1; // Start from previous month

  if (curMonth < 1) {
    curMonth = 12;
    curYear -= 1;
  }

  while (months.length < limit && curYear >= 2020) {
    const monthName = MONTH_NAMES[curMonth];
    months.push({
      year: curYear,
      month: curMonth,
      monthName,
      label: `${monthName} ${curYear}`,
      key: `${curYear}-${String(curMonth).padStart(2, '0')}`,
    });

    curMonth -= 1;
    if (curMonth < 1) {
      curMonth = 12;
      curYear -= 1;
    }
  }

  return months;
}

/**
 * Get information regarding the upcoming/in-progress month
 * @returns {{ year: number, month: number, monthName: string, label: string, releaseDate: string, daysRemaining: number }}
 */
export function getIncompleteMonthInfo() {
  const current = getCurrentYearMonth();
  const now = new Date();
  const nextMonthDate = new Date(current.year, current.month, 1);
  const diffTime = nextMonthDate.getTime() - now.getTime();
  const daysRemaining = Math.max(1, Math.ceil(diffTime / (1000 * 60 * 60 * 24)));

  const releaseMonthName = MONTH_NAMES[nextMonthDate.getMonth() + 1];

  return {
    year: current.year,
    month: current.month,
    monthName: MONTH_NAMES[current.month],
    label: `${MONTH_NAMES[current.month]} ${current.year}`,
    releaseDate: `${releaseMonthName} 1, ${nextMonthDate.getFullYear()}`,
    daysRemaining,
  };
}
