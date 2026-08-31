import React from 'react';
import Card from '../../../components/ui/Card';
import { getCategoryIconComponent } from '../categoryIcons';
import { Edit2, Trash2 } from 'lucide-react';

export function CategoryCard({ category, onEditClick, onDeleteClick }) {
  const IconComponent = getCategoryIconComponent(category.icon);
  const color = category.color || '#4f46e5';

  return (
    <Card className="bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)] p-5 flex flex-col justify-between space-y-4 hover:border-slate-300 transition-all group">
      <div className="space-y-3">
        {/* Top Header with Icon, Name, and Default Badge */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 shadow-sm"
              style={{
                backgroundColor: `${color}15`,
                color: color,
                borderColor: `${color}30`,
                borderWidth: '1px',
              }}
            >
              <IconComponent className="w-5 h-5" />
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <h3
                  className="text-sm font-bold text-slate-900 truncate"
                  title={category.name}
                >
                  {category.name}
                </h3>
                {category.isDefault && (
                  <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-600 border border-slate-200">
                    Default
                  </span>
                )}
              </div>

              {category.description ? (
                <p className="text-xs text-slate-400 truncate mt-0.5" title={category.description}>
                  {category.description}
                </p>
              ) : (
                <p className="text-[11px] text-slate-300 mt-0.5 italic">
                  No description
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Footer Actions */}
      <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-1.5">
        <button
          type="button"
          onClick={() => onEditClick(category)}
          title="Edit category"
          aria-label="Edit category"
          className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
        >
          <Edit2 className="w-4 h-4" />
        </button>

        <button
          type="button"
          onClick={() => onDeleteClick(category)}
          title="Delete category"
          aria-label="Delete category"
          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </Card>
  );
}

export default CategoryCard;
