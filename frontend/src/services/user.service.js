import apiClient from './api.client';

export const userService = {
  /**
   * Get current authenticated user profile
   * @returns {Promise<{ id: number, name: string, email: string, createdAt: string, updatedAt: string }>}
   */
  async getCurrentUser() {
    const response = await apiClient.get('/users/me');
    return response.data;
  },

  /**
   * Delete current authenticated user account and all owned data permanently
   * @returns {Promise<{ message: string, success: boolean }>}
   */
  async deleteAccount() {
    const response = await apiClient.delete('/users/me');
    return response.data;
  },
};

export default userService;
