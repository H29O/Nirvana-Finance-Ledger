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
};

export default userService;
