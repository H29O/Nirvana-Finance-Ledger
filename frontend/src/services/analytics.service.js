import apiClient from './api.client';

export const analyticsService = {
  /**
   * Fetch overview totals (Income, Expense, Net Savings, Transaction Count)
   * @param {string} startDate - 'YYYY-MM-DD'
   * @param {string} endDate - 'YYYY-MM-DD'
   * @returns {Promise<{ totalIncome: number, totalExpense: number, netSavings: number, transactionCount: number }>}
   */
  async getOverview(startDate, endDate) {
    const response = await apiClient.get('/analytics/overview', {
      params: { startDate, endDate },
    });
    return response.data;
  },

  /**
   * Fetch category-wise breakdown and percentage distribution
   * @param {string} startDate - 'YYYY-MM-DD'
   * @param {string} endDate - 'YYYY-MM-DD'
   * @param {'EXPENSE' | 'INCOME'} [type='EXPENSE']
   * @returns {Promise<Array<{ categoryId: number, categoryName: string, amount: number, percentage: number, transactionCount: number }>>}
   */
  async getCategoryBreakdown(startDate, endDate, type = 'EXPENSE') {
    const params = { startDate, endDate };
    if (type) {
      params.type = type;
    }
    const response = await apiClient.get('/analytics/category-breakdown', {
      params,
    });
    return response.data;
  },

  /**
   * Fetch month-by-month income, expense, and net savings series
   * @param {string} startDate - 'YYYY-MM-DD'
   * @param {string} endDate - 'YYYY-MM-DD'
   * @returns {Promise<Array<{ year: number, month: number, income: number, expense: number, netSavings: number }>>}
   */
  async getMonthlyTrend(startDate, endDate) {
    const response = await apiClient.get('/analytics/monthly-trend', {
      params: { startDate, endDate },
    });
    return response.data;
  },

  /**
   * Fetch period-over-period comparison and percentage changes
   * @param {string} startDate - 'YYYY-MM-DD'
   * @param {string} endDate - 'YYYY-MM-DD'
   * @returns {Promise<{
   *   currentPeriod: { income: number, expense: number, netSavings: number },
   *   previousPeriod: { income: number, expense: number, netSavings: number },
   *   incomeChangePercentage: number,
   *   expenseChangePercentage: number,
   *   savingsChangePercentage: number
   * }>}
   */
  async getPeriodComparison(startDate, endDate) {
    const response = await apiClient.get('/analytics/period-comparison', {
      params: { startDate, endDate },
    });
    return response.data;
  },
};

export default analyticsService;
