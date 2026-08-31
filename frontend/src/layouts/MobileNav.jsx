import React from 'react';
import { NavLink } from 'react-router-dom';
import { NAV_ITEMS } from './Sidebar';
import { cn } from '../lib/utils';
import {
  LayoutDashboard,
  ArrowLeftRight,
  Tags,
  HandCoins,
  Receipt,
  BarChart3,
  Sparkles,
  Settings,
  X,
} from 'lucide-react';

const ICON_MAP = {
  LayoutDashboard,
  ArrowLeftRight,
  Tags,
  HandCoins,
  Receipt,
  BarChart3,
  Sparkles,
  Settings,
};

export function MobileNav({ isOpen, onClose }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 md:hidden flex">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Drawer */}
      <div className="relative flex flex-col w-4/5 max-w-xs bg-white border-r border-slate-200 p-5 z-10 h-full shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div className="flex items-center gap-2.5">
            <div className="p-1.5 rounded-xl bg-indigo-50 border border-indigo-100">
              <svg className="w-5 h-5" viewBox="0 0 100 100" fill="none">
                <rect width="100" height="100" rx="24" fill="#4f46e5" />
                <path d="M28 32C28 29.7909 29.7909 28 32 28H68C70.2091 28 72 29.7909 72 32V68C72 70.2091 70.2091 72 68 72H32C29.7909 72 28 70.2091 28 68V32Z" stroke="#ffffff" strokeWidth="6" />
                <path d="M38 42H62" stroke="#e0e7ff" strokeWidth="6" />
                <path d="M38 52H52" stroke="#10b981" strokeWidth="6" />
              </svg>
            </div>
            <div>
              <span className="font-bold text-sm text-slate-900">Finance Ledger</span>
              <p className="text-[10px] text-slate-400">Personal Expenses</p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100"
            aria-label="Close menu"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation list */}
        <nav className="flex-1 py-4 space-y-1 overflow-y-auto">
          {NAV_ITEMS.map((item) => {
            const Icon = ICON_MAP[item.icon] || LayoutDashboard;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/'}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all',
                    isActive
                      ? 'bg-indigo-50 text-indigo-700 font-semibold border-l-2 border-indigo-600'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  )
                }
              >
                <Icon className="w-4 h-4" />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="pt-3 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
          <span>Finance Ledger</span>
          <span>v1.0.0</span>
        </div>
      </div>
    </div>
  );
}

export default MobileNav;
