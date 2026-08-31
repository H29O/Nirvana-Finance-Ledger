import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import Spinner from '../ui/Spinner';

export function PublicRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen w-full flex-col items-center justify-center bg-slate-50 text-slate-700">
        <Spinner size="lg" className="border-slate-300 border-t-indigo-600" />
      </div>
    );
  }

  if (isAuthenticated) {
    const destination = location.state?.from?.pathname || '/';
    return <Navigate to={destination} replace />;
  }

  return children ? children : <Outlet />;
}

export default PublicRoute;
