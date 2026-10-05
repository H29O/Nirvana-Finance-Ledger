import React from 'react';
import { useLocation } from 'react-router-dom';
import useAuth from '../hooks/useAuth';
import { NAV_ITEMS } from './Sidebar';
import { Menu, LogOut, User } from 'lucide-react';

export function Topbar({ onMenuClick }) {
  const location = useLocation();
  const { user, logout } = useAuth();

  const currentItem = NAV_ITEMS.find((item) =>
    item.path === '/'
      ? location.pathname === '/'
      : location.pathname.startsWith(item.path)
  ) || { name: 'Nivara' };

  return (
    <header className="h-16 border-b border-slate-200/80 bg-white/80 backdrop-blur-md sticky top-0 z-30 px-4 md:px-8 flex items-center justify-between">
      {/* Left: Mobile Menu button & Current Area Title */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onMenuClick}
          className="md:hidden p-2 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
          aria-label="Open menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div>
          <h2 className="text-base font-bold text-slate-900 tracking-tight">
            {currentItem.name}
          </h2>
        </div>
      </div>

      {/* Right: User badge & logout */}
      <div className="flex items-center gap-3">
        <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-100/80 text-xs text-slate-700 border border-slate-200/60 font-medium">
          <User className="w-3.5 h-3.5 text-indigo-600" />
          <span>{user?.name || user?.email || 'Logged in'}</span>
        </div>

        <button
          type="button"
          onClick={logout}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium text-slate-500 hover:text-rose-600 hover:bg-rose-50 transition-colors"
        >
          <LogOut className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Sign Out</span>
        </button>
      </div>
    </header>
  );
}

export default Topbar;
