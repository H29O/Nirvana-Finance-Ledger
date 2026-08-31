import axios from 'axios';
import { STORAGE_KEYS } from '../lib/constants';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Unauthorized event listener callback registration
let unauthorizedHandler = null;

export function registerUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

// Request Interceptor: Attach JWT Bearer Token if available (except for auth endpoints)
apiClient.interceptors.request.use(
  (config) => {
    const isAuthEndpoint =
      config.url &&
      (config.url.includes('/auth/login') || config.url.includes('/auth/register'));

    if (!isAuthEndpoint) {
      const token = localStorage.getItem(STORAGE_KEYS.TOKEN);
      if (token && token.trim() !== '') {
        config.headers.Authorization = `Bearer ${token}`;
      } else if (config.headers) {
        delete config.headers.Authorization;
      }
    } else if (config.headers) {
      delete config.headers.Authorization;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Normalize Backend Error Responses and Handle 401
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response ? error.response.status : null;
    const data = error.response ? error.response.data : null;

    let normalizedMessage = 'A network or server error occurred. Please try again.';
    let validationErrors = null;

    if (data) {
      if (typeof data === 'string') {
        normalizedMessage = data;
      } else if (data.error) {
        // Backend single error response: { error: "..." }
        normalizedMessage = data.error;
      } else if (data.message) {
        normalizedMessage = data.message;
      } else if (typeof data === 'object' && Object.keys(data).length > 0) {
        // Backend validation errors map: { fieldName: "validation message", ... }
        validationErrors = data;
        const firstField = Object.keys(data)[0];
        normalizedMessage = data[firstField] || 'Invalid input provided.';
      }
    } else if (error.message) {
      normalizedMessage = error.message;
    }

    // Attach normalized information onto the error object
    error.normalizedMessage = normalizedMessage;
    error.validationErrors = validationErrors;

    // Handle 401 Unauthorized (unless requesting login or register endpoints)
    if (status === 401) {
      const isAuthEndpoint =
        error.config &&
        (error.config.url.includes('/auth/login') || error.config.url.includes('/auth/register'));

      if (!isAuthEndpoint && typeof unauthorizedHandler === 'function') {
        unauthorizedHandler();
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
