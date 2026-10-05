import React from 'react';
import { NavLink } from 'react-router-dom';
import { NAV_ITEMS } from './Sidebar';
import { cn } from '../lib/utils';
import NivaraBrand from '../components/ui/NivaraLogo';
import {
  LayoutDashboard,
  ArrowLeftRight,
  Tags,
  HandCoins,
  Receipt,
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
          <NivaraBrand size="sm" subtitle="Personal Finance" to="/" />

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
          <span>Nivara</span>
          <span>v1.0.0</span>
        </div>
      </div>
    </div>
  );
}

export default MobileNav;
