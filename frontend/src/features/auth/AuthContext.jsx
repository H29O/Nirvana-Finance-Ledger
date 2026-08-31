import React, { createContext, useState, useEffect, useCallback } from 'react';
import { STORAGE_KEYS } from '../../lib/constants';
import authService from '../../services/auth.service';
import userService from '../../services/user.service';
import { registerUnauthorizedHandler } from '../../services/api.client';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(STORAGE_KEYS.TOKEN));
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Logout handler
  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
    localStorage.removeItem(STORAGE_KEYS.USER);
    setToken(null);
    setUser(null);
  }, []);

  // Register unauthorized response handler with API client
  useEffect(() => {
    registerUnauthorizedHandler(() => {
      logout();
    });
  }, [logout]);

  // Verify session on initial mount if token exists
  useEffect(() => {
    let isMounted = true;

    async function initializeAuth() {
      const storedToken = localStorage.getItem(STORAGE_KEYS.TOKEN);

      if (!storedToken) {
        if (isMounted) {
          setUser(null);
          setIsLoading(false);
        }
        return;
      }

      try {
        // Fetch current user from backend /api/users/me as the source of truth
        const currentUser = await userService.getCurrentUser();
        if (isMounted) {
          setUser(currentUser);
          setToken(storedToken);
        }
      } catch (error) {
        // Token is expired or invalid on the backend
        if (isMounted) {
          logout();
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    initializeAuth();

    return () => {
      isMounted = false;
    };
  }, [logout]);

  // Login action
  const login = useCallback(async ({ email, password }) => {
    setIsLoading(true);
    try {
      const response = await authService.login({ email, password });
      const { token: receivedToken, user: receivedUser } = response;

      localStorage.setItem(STORAGE_KEYS.TOKEN, receivedToken);
      setToken(receivedToken);
      setUser(receivedUser);

      return response;
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Register action
  const register = useCallback(async ({ name, email, password }) => {
    setIsLoading(true);
    try {
      const response = await authService.register({ name, email, password });
      const { token: receivedToken, user: receivedUser } = response;

      localStorage.setItem(STORAGE_KEYS.TOKEN, receivedToken);
      setToken(receivedToken);
      setUser(receivedUser);

      return response;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthContext;
