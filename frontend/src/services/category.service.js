import apiClient from './api.client';

export const categoryService = {
  /**
   * Fetch all user-isolated and default categories
   * @returns {Promise<Array<{ id: number, name: string, description: string, color: string, icon: string, isDefault: boolean, createdAt: string, updatedAt: string }>>}
   */
  async getAllCategories() {
    const response = await apiClient.get('/categories');
    return response.data;
  },

  /**
   * Get category by ID
   * @param {number} id
   * @returns {Promise<any>}
   */
  async getCategoryById(id) {
    const response = await apiClient.get(`/categories/${id}`);
    return response.data;
  },

  /**
   * Create a new category
   * @param {Object} data
   * @param {string} data.name
   * @param {string} [data.description]
   * @param {string} [data.color]
   * @param {string} [data.icon]
   * @param {boolean} [data.isDefault=false]
   * @returns {Promise<any>}
   */
  async createCategory({ name, description, color, icon, isDefault = false }) {
    const payload = {
      name: name.trim(),
      description: description ? description.trim() : null,
      color: color || null,
      icon: icon || null,
      isDefault: Boolean(isDefault),
    };

    const response = await apiClient.post('/categories', payload);
    return response.data;
  },

  /**
   * Update an existing category
   * @param {number} id
   * @param {Object} data
   * @param {string} data.name
   * @param {string} [data.description]
   * @param {string} [data.color]
   * @param {string} [data.icon]
   * @param {boolean} [data.isDefault]
   * @returns {Promise<any>}
   */
  async updateCategory(id, { name, description, color, icon, isDefault }) {
    const payload = {
      name: name.trim(),
      description: description ? description.trim() : null,
      color: color || null,
      icon: icon || null,
      isDefault: isDefault != null ? Boolean(isDefault) : undefined,
    };

    const response = await apiClient.put(`/categories/${id}`, payload);
    return response.data;
  },

  /**
   * Delete a category (fails if in use by transactions)
   * @param {number} id
   * @returns {Promise<void>}
   */
  async deleteCategory(id) {
    await apiClient.delete(`/categories/${id}`);
  },
};

export default categoryService;
