import apiClient from './api.client';

export const authService = {
  /**
   * Log in user with email & password
   * @param {{ email: string, password: string }} credentials
   * @returns {Promise<{ token: string, user: { id: number, name: string, email: string, createdAt: string, updatedAt: string } }>}
   */
  async login({ email, password }) {
    const response = await apiClient.post('/auth/login', { email, password });
    return response.data;
  },

  /**
   * Register new user account
   * @param {{ name: string, email: string, password: string }} data
   * @returns {Promise<{ token: string, user: { id: number, name: string, email: string, createdAt: string, updatedAt: string } }>}
   */
  async register({ name, email, password }) {
    const response = await apiClient.post('/auth/register', { name, email, password });
    return response.data;
  },
};

export default authService;
