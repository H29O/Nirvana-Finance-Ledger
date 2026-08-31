import apiClient from './api.client';

export const lendingService = {
  /**
   * Fetch paginated and filterable loan records
   * @param {Object} [params]
   * @param {'LENT'|'BORROWED'} [params.type]
   * @param {'ACTIVE'|'SETTLED'} [params.status]
   * @param {number} [params.page=0]
   * @param {number} [params.size=50]
   * @param {string} [params.sort='createdAt,desc']
   * @returns {Promise<{ content: Array<any>, totalElements: number, totalPages: number, number: number, size: number }>}
   */
  async getLoanRecords(params = {}) {
    const response = await apiClient.get('/loans', { params });
    return response.data;
  },

  /**
   * Get loan record by ID
   * @param {number} id
   * @returns {Promise<any>}
   */
  async getLoanRecordById(id) {
    const response = await apiClient.get(`/loans/${id}`);
    return response.data;
  },

  /**
   * Create a new loan record
   * @param {Object} data
   * @param {'LENT'|'BORROWED'} data.type
   * @param {string} data.personName
   * @param {number} data.originalAmount
   * @param {string} [data.description]
   * @param {string} [data.dueDate] YYYY-MM-DD
   * @returns {Promise<any>}
   */
  async createLoanRecord({ type, personName, originalAmount, description, dueDate }) {
    const payload = {
      type,
      personName: personName.trim(),
      originalAmount: Number(originalAmount),
      description: description ? description.trim() : null,
      dueDate: dueDate || null,
    };

    const response = await apiClient.post('/loans', payload);
    return response.data;
  },

  /**
   * Update an existing loan record
   * @param {number} id
   * @param {Object} data
   * @param {'LENT'|'BORROWED'} data.type
   * @param {string} data.personName
   * @param {string} [data.description]
   * @param {string} [data.dueDate] YYYY-MM-DD
   * @returns {Promise<any>}
   */
  async updateLoanRecord(id, { type, personName, description, dueDate }) {
    const payload = {
      type,
      personName: personName.trim(),
      description: description ? description.trim() : null,
      dueDate: dueDate || null,
    };

    const response = await apiClient.put(`/loans/${id}`, payload);
    return response.data;
  },

  /**
   * Record a partial or full payment on an active loan record
   * @param {number} id
   * @param {Object} data
   * @param {number} data.amount
   * @returns {Promise<any>}
   */
  async recordPayment(id, { amount }) {
    const payload = {
      amount: Number(amount),
    };

    const response = await apiClient.patch(`/loans/${id}/payment`, payload);
    return response.data;
  },

  /**
   * Delete a loan record
   * @param {number} id
   * @returns {Promise<void>}
   */
  async deleteLoanRecord(id) {
    await apiClient.delete(`/loans/${id}`);
  },
};

export default lendingService;
