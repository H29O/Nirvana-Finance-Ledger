import apiClient from './api.client';

export const receiptService = {
  /**
   * Upload a new receipt file (JPEG, PNG, PDF up to 10MB)
   * @param {File} file
   * @param {number} [transactionId]
   * @returns {Promise<{ id: number, originalFileName: string, contentType: string, fileSize: number, uploadedAt: string, transactionId: number | null }>}
   */
  async uploadReceipt(file, transactionId) {
    const formData = new FormData();
    formData.append('file', file);
    if (transactionId) {
      formData.append('transactionId', transactionId);
    }

    const response = await apiClient.post('/receipts', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  /**
   * Get paginated receipts
   * @param {Object} [params]
   * @param {number} [params.page=0]
   * @param {number} [params.size=10]
   * @param {string} [params.sort='uploadedAt,desc']
   * @returns {Promise<{ content: Array<any>, totalElements: number, totalPages: number, number: number, size: number, first: boolean, last: boolean }>}
   */
  async getReceipts({ page = 0, size = 10, sort = 'uploadedAt,desc' } = {}) {
    const response = await apiClient.get('/receipts', {
      params: { page, size, sort },
    });
    return response.data;
  },

  /**
   * Get receipt metadata by ID
   * @param {number} id
   * @returns {Promise<any>}
   */
  async getReceiptMetadata(id) {
    const response = await apiClient.get(`/receipts/${id}`);
    return response.data;
  },

  /**
   * Get receipt file preview as an Object URL blob
   * @param {number} id
   * @returns {Promise<{ objectUrl: string, contentType: string }>}
   */
  async getReceiptBlob(id) {
    const response = await apiClient.get(`/receipts/${id}/file`, {
      responseType: 'blob',
    });
    const contentType = response.headers['content-type'] || 'image/jpeg';
    const objectUrl = URL.createObjectURL(response.data);
    return { objectUrl, contentType };
  },

  /**
   * Link a receipt to an existing transaction
   * @param {number} receiptId
   * @param {number} transactionId
   * @returns {Promise<any>}
   */
  async linkTransaction(receiptId, transactionId) {
    const response = await apiClient.patch(`/receipts/${receiptId}/transaction/${transactionId}`);
    return response.data;
  },

  /**
   * Unlink a receipt from its transaction
   * @param {number} receiptId
   * @returns {Promise<any>}
   */
  async unlinkTransaction(receiptId) {
    const response = await apiClient.delete(`/receipts/${receiptId}/transaction`);
    return response.data;
  },

  /**
   * Delete a receipt
   * @param {number} receiptId
   * @returns {Promise<void>}
   */
  async deleteReceipt(receiptId) {
    await apiClient.delete(`/receipts/${receiptId}`);
  },

  /**
   * Trigger OCR processing on a receipt
   * @param {number} receiptId
   * @returns {Promise<{ id: number, receiptId: number, status: string, rawText: string | null, detectedMerchant: string | null, detectedAmount: number | null, detectedDate: string | null, failureReason: string | null, processedAt: string }>}
   */
  async processOcr(receiptId) {
    const response = await apiClient.post(`/receipts/${receiptId}/ocr`);
    return response.data;
  },

  /**
   * Get stored OCR data for a receipt
   * @param {number} receiptId
   * @returns {Promise<any>}
   */
  async getOcrData(receiptId) {
    const response = await apiClient.get(`/receipts/${receiptId}/ocr`);
    return response.data;
  },

  /**
   * Update OCR data for a receipt with user-corrected values
   * @param {number} receiptId
   * @param {{ merchant: string, amount: number, date?: string, categoryId?: number, categoryName?: string }} data
   * @returns {Promise<any>}
   */
  async updateOcrData(receiptId, data) {
    const response = await apiClient.put(`/receipts/${receiptId}/ocr`, data);
    return response.data;
  },
};

export default receiptService;
