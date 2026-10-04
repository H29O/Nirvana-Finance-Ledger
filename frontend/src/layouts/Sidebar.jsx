import React from 'react';
import { NavLink } from 'react-router-dom';
import useAuth from '../hooks/useAuth';
import { cn } from '../lib/utils';
import {
  LayoutDashboard,
  ArrowLeftRight,
  Tags,
  HandCoins,
  Receipt,
  Sparkles,
  Settings,
  LogOut,
  User,
} from 'lucide-react';

const ICON_MAP = {
  LayoutDashboard,
  ArrowLeftRight,
  Tags,
  HandCoins,
  Receipt,
  Sparkles,
  Settings,
};

export const NAV_ITEMS = [
  { name: 'Dashboard', path: '/', icon: 'LayoutDashboard' },
  { name: 'Transactions', path: '/transactions', icon: 'ArrowLeftRight' },
  { name: 'Categories', path: '/categories', icon: 'Tags' },
  { name: 'Lending & Borrowing', path: '/lending', icon: 'HandCoins' },
  { name: 'Receipt Vault', path: '/receipts', icon: 'Receipt' },
  { name: 'Money Wrapped', path: '/money-wrapped', icon: 'Sparkles' },
  { name: 'Settings', path: '/settings', icon: 'Settings' },
];

export function Sidebar({ className }) {
  const { user, logout } = useAuth();

  return (
    <aside
      className={cn(
        'w-64 bg-white border-r border-slate-200/80 flex flex-col justify-between shrink-0 h-screen sticky top-0',
        className
      )}
    >
      {/* Brand Header */}
      <div className="p-5 pb-3">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-indigo-50 border border-indigo-100 shadow-sm">
            <svg className="w-6 h-6" viewBox="0 0 100 100" fill="none">
              <rect width="100" height="100" rx="24" fill="#4f46e5" />
              <path d="M28 32C28 29.7909 29.7909 28 32 28H68C70.2091 28 72 29.7909 72 32V68C72 70.2091 70.2091 72 68 72H32C29.7909 72 28 70.2091 28 68V32Z" stroke="#ffffff" strokeWidth="5" strokeLinejoin="round" />
              <path d="M38 42H62" stroke="#e0e7ff" strokeWidth="5" strokeLinecap="round" />
              <path d="M38 52H52" stroke="#10b981" strokeWidth="5" strokeLinecap="round" />
              <path d="M38 62H58" stroke="#ffffff" strokeWidth="5" strokeLinecap="round" />
            </svg>
          </div>
          <div>
            <span className="font-bold text-sm tracking-tight text-slate-900">
              Finance Ledger
            </span>
            <p className="text-[11px] text-slate-400 font-medium">
              Personal Expenses
            </p>
          </div>
        </div>
      </div>

      {/* Navigation Items */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {NAV_ITEMS.map((item) => {
          const Icon = ICON_MAP[item.icon] || LayoutDashboard;

          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all group select-none',
                  isActive
                    ? 'bg-indigo-50 text-indigo-700 font-semibold border-l-2 border-indigo-600'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                )
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    className={cn(
                      'w-4 h-4 shrink-0 transition-colors',
                      isActive ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600'
                    )}
                  />
                  <span>{item.name}</span>
                </>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* User Area & Logout Footer */}
      <div className="p-3 border-t border-slate-100 bg-slate-50/50">
        <div className="flex items-center justify-between p-2 rounded-xl bg-white border border-slate-200/80 shadow-sm">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs shrink-0">
              {user?.name ? user.name.charAt(0).toUpperCase() : <User className="w-4 h-4" />}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold text-slate-900 truncate">
                {user?.name || 'User'}
              </p>
              <p className="text-[10px] text-slate-400 truncate">
                {user?.email || '—'}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={logout}
            title="Sign out"
            aria-label="Sign out"
            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors shrink-0"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}

export default Sidebar;
