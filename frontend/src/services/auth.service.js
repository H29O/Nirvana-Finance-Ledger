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
   * @returns {Promise<{ token?: string, user: { id: number, name: string, email: string, emailVerified: boolean }, verificationRequired: boolean }>}
   */
  async register({ name, email, password }) {
    const response = await apiClient.post('/auth/register', { name, email, password });
    return response.data;
  },

  /**
   * Verify email with 6-digit OTP
   * @param {{ email: string, otp: string }} data
   * @returns {Promise<{ token: string, user: { id: number, name: string, email: string, emailVerified: boolean } }>}
   */
  async verifyEmail({ email, otp }) {
    const response = await apiClient.post('/auth/verify-email', { email, otp });
    return response.data;
  },

  /**
   * Resend verification email
   * @param {{ email: string }} data
   * @returns {Promise<{ message: string }>}
   */
  async resendVerification({ email }) {
    const response = await apiClient.post('/auth/resend-verification', { email });
    return response.data;
  },

  /**
   * Request password reset link
   * @param {{ email: string }} data
   * @returns {Promise<{ message: string, emailDeliverySuccess: boolean }>}
   */
  async forgotPassword({ email }) {
    const response = await apiClient.post('/auth/forgot-password', { email });
    return response.data;
  },

  /**
   * Validate reset token status before displaying reset form
   * @param {string} token
   * @returns {Promise<{ valid: boolean }>}
   */
  async validateResetToken(token) {
    const response = await apiClient.get('/auth/validate-reset-token', {
      params: { token },
    });
    return response.data;
  },

  /**
   * Reset password using raw reset token
   * @param {{ token: string, newPassword: string, confirmPassword: string }} data
   * @returns {Promise<{ message: string, success: boolean }>}
   */
  async resetPassword({ token, newPassword, confirmPassword }) {
    const response = await apiClient.post('/auth/reset-password', {
      token,
      newPassword,
      confirmPassword,
    });
    return response.data;
  },

  /**
   * Change password for authenticated user
   * @param {{ currentPassword: string, newPassword: string, confirmPassword: string }} data
   * @returns {Promise<{ message: string, success: boolean }>}
   */
  async changePassword({ currentPassword, newPassword, confirmPassword }) {
    const response = await apiClient.post('/auth/change-password', {
      currentPassword,
      newPassword,
      confirmPassword,
    });
    return response.data;
  },
};

export default authService;
