import apiClient from './api.client';

export const transactionService = {
  /**
   * Fetch paginated and filtered transactions
   * @param {Object} [params]
   * @param {string} [params.type] - e.g. 'EXPENSE', 'INCOME', etc.
   * @param {string} [params.paymentMethod] - e.g. 'CASH', 'DIGITAL'
   * @param {string} [params.startDate] - ISO datetime string
   * @param {string} [params.endDate] - ISO datetime string
   * @param {number} [params.page=0] - 0-indexed page
   * @param {number} [params.size=10] - page size
   * @param {string} [params.sort='transactionDate,desc']
   * @returns {Promise<{ content: Array<any>, totalElements: number, totalPages: number, number: number, size: number, first: boolean, last: boolean }>}
   */
  async getTransactions({
    type,
    paymentMethod,
    startDate,
    endDate,
    page = 0,
    size = 10,
    sort = 'transactionDate,desc',
  } = {}) {
    const params = { page, size, sort };
    if (type) params.type = type;
    if (paymentMethod) params.paymentMethod = paymentMethod;
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;

    const response = await apiClient.get('/transactions', { params });
    return response.data;
  },

  /**
   * Create a new transaction
   * @param {Object} data
   * @param {number} data.amount
   * @param {string} data.transactionType - 'EXPENSE' | 'INCOME'
   * @param {string} data.paymentMethod - 'CASH' | 'DIGITAL'
   * @param {string} [data.entrySource='MANUAL'] - 'MANUAL' | 'PAYMENT_SCREENSHOT' | 'RECEPT'
   * @param {string} data.merchant - merchant or transaction description
   * @param {string} [data.notes]
   * @param {number} [data.categoryId]
   * @returns {Promise<any>}
   */
  async createTransaction({
    amount,
    transactionType,
    paymentMethod,
    entrySource = 'MANUAL',
    merchant,
    notes,
    categoryId,
  }) {
    const payload = {
      amount,
      transactionType,
      paymentMethod,
      entrySource,
      merchant,
      notes: notes || null,
      categoryId: categoryId ? Number(categoryId) : null,
    };

    const response = await apiClient.post('/transactions', payload);
    return response.data;
  },

  /**
   * Delete a transaction by ID
   * @param {number} id
   * @returns {Promise<void>}
   */
  async deleteTransaction(id) {
    await apiClient.delete(`/transactions/${id}`);
  },
};

export default transactionService;
