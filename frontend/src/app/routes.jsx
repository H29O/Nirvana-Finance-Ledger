import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';

// Layout & Guards
import AppShell from '../layouts/AppShell';
import ProtectedRoute from '../components/common/ProtectedRoute';
import PublicRoute from '../components/common/PublicRoute';

// Auth Features
import LoginPage from '../features/auth/LoginPage';
import RegisterPage from '../features/auth/RegisterPage';
import VerifyEmailPage from '../features/auth/VerifyEmailPage';
import ForgotPasswordPage from '../features/auth/ForgotPasswordPage';
import ResetPasswordPage from '../features/auth/ResetPasswordPage';

// Features
import DashboardPage from '../features/dashboard/DashboardPage';
import TransactionsPage from '../features/transactions/TransactionsPage';
import CategoriesPage from '../features/categories/CategoriesPage';
import LendingPage from '../features/lending/LendingPage';
import ReceiptsPage from '../features/receipts/ReceiptsPage';
import MoneyWrappedPage from '../features/money-wrapped/MoneyWrappedPage';
import SettingsPage from '../features/settings/SettingsPage';
import NotFoundPage from '../features/common/NotFoundPage';

export function AppRoutes() {
  return (
    <Routes>
      {/* Public Guest Routes */}
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicRoute>
            <RegisterPage />
          </PublicRoute>
        }
      />
      <Route
        path="/verify-email"
        element={
          <PublicRoute>
            <VerifyEmailPage />
          </PublicRoute>
        }
      />
      <Route
        path="/forgot-password"
        element={
          <PublicRoute>
            <ForgotPasswordPage />
          </PublicRoute>
        }
      />
      <Route
        path="/reset-password"
        element={
          <PublicRoute>
            <ResetPasswordPage />
          </PublicRoute>
        }
      />

      {/* Protected Authenticated Routes */}
      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="transactions" element={<TransactionsPage />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="lending" element={<LendingPage />} />
        <Route path="receipts" element={<ReceiptsPage />} />
        <Route path="money-wrapped" element={<MoneyWrappedPage />} />
        <Route path="settings" element={<SettingsPage />} />

        {/* Not Found Page inside protected shell if authenticated */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

export default AppRoutes;
