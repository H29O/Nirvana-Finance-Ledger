import apiClient from './api.client';

export const wrappedService = {
  /**
   * Fetch Money Wrapped financial retrospective for a given year and month
   * @param {number} [year] - Defaults to current year on backend
   * @param {number} [month] - Defaults to current month on backend
   * @returns {Promise<import('./types').MoneyWrappedResponse>}
   */
  async getMoneyWrapped(year, month) {
    const params = {};
    if (year != null) params.year = year;
    if (month != null) params.month = month;

    const response = await apiClient.get('/wrapped', { params });
    return response.data;
  },
};

export default wrappedService;
