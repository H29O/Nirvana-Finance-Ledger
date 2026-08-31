import apiClient from './api.client';

export const dashboardService = {
  /**
   * Get monthly financial summary (income, expense, net savings, transaction count)
   * @param {number} [year]
   * @param {number} [month]
   * @returns {Promise<{ totalIncome: number, totalExpense: number, netSavings: number, totalTransactions: number }>}
   */
  async getMonthlySummary(year, month) {
    const params = {};
    if (year !== undefined && year !== null) params.year = year;
    if (month !== undefined && month !== null) params.month = month;

    const response = await apiClient.get('/dashboard/monthly-summary', { params });
    return response.data;
  },

  /**
   * Get income vs expense comparative data
   * @param {number} [year]
   * @param {number} [month]
   * @returns {Promise<{ totalIncome: number, totalExpense: number, balance: number }>}
   */
  async getIncomeVsExpense(year, month) {
    const params = {};
    if (year !== undefined && year !== null) params.year = year;
    if (month !== undefined && month !== null) params.month = month;

    const response = await apiClient.get('/dashboard/income-vs-expense', { params });
    return response.data;
  },

  /**
   * Get expense breakdown categorized by category
   * @param {number} [year]
   * @param {number} [month]
   * @returns {Promise<{ totalExpense: number, categories: Array<{ categoryId: number, categoryName: string, color: string, icon: string, totalAmount: number }> }>}
   */
  async getExpenseByCategory(year, month) {
    const params = {};
    if (year !== undefined && year !== null) params.year = year;
    if (month !== undefined && month !== null) params.month = month;

    const response = await apiClient.get('/dashboard/expense-by-category', { params });
    return response.data;
  },

  /**
   * Get top expense categories ranked by amount
   * @param {number} [year]
   * @param {number} [month]
   * @param {number} [limit=5]
   * @returns {Promise<Array<{ categoryId: number, categoryName: string, color: string, icon: string, totalAmount: number, transactionCount: number }>>}
   */
  async getTopCategories(year, month, limit = 5) {
    const params = { limit };
    if (year !== undefined && year !== null) params.year = year;
    if (month !== undefined && month !== null) params.month = month;

    const response = await apiClient.get('/dashboard/top-categories', { params });
    return response.data;
  },
};

export default dashboardService;
