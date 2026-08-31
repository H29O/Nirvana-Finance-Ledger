import React, { useState, useEffect } from 'react';
import categoryService from '../../../services/category.service';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { CATEGORY_COLORS, CATEGORY_ICONS, getCategoryIconComponent } from '../categoryIcons';
import { X, AlertCircle } from 'lucide-react';

export function CategoryFormModal({
  isOpen,
  onClose,
  category, // null when creating, populated object when editing
  onSuccess,
}) {
  const isEditing = Boolean(category);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    color: '#4f46e5',
    icon: 'ShoppingBag',
  });

  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      if (category) {
        setFormData({
          name: category.name || '',
          description: category.description || '',
          color: category.color || '#4f46e5',
          icon: category.icon || 'ShoppingBag',
        });
      } else {
        setFormData({
          name: '',
          description: '',
          color: '#4f46e5',
          icon: 'ShoppingBag',
        });
      }
      setErrors({});
      setGeneralError('');
    }
  }, [isOpen, category]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
    if (generalError) setGeneralError('');
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.name || formData.name.trim() === '') {
      newErrors.name = 'Category name cannot be blank';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setGeneralError('');

    try {
      if (isEditing) {
        await categoryService.updateCategory(category.id, {
          name: formData.name,
          description: formData.description,
          color: formData.color,
          icon: formData.icon,
        });
      } else {
        await categoryService.createCategory({
          name: formData.name,
          description: formData.description,
          color: formData.color,
          icon: formData.icon,
        });
      }

      onSuccess(isEditing ? 'Category updated successfully.' : 'Category created successfully.');
      onClose();
    } catch (err) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      }
      setGeneralError(
        err.normalizedMessage || 'Failed to save category. Please check your inputs.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const PreviewIcon = getCategoryIconComponent(formData.icon);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Modal Card */}
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-5 my-8">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div>
            <h3 className="text-base font-bold text-slate-900">
              {isEditing ? 'Edit Category' : 'Add Category'}
            </h3>
            <p className="text-xs text-slate-500">
              {isEditing
                ? 'Update category details and styling'
                : 'Create a custom category to organize your finances'}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Error Alert */}
        {generalError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
            <span>{generalError}</span>
          </div>
        )}

        {/* Live Preview Bar */}
        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/70 flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 shadow-sm"
            style={{
              backgroundColor: `${formData.color}15`,
              color: formData.color,
              borderColor: `${formData.color}30`,
              borderWidth: '1px',
            }}
          >
            <PreviewIcon className="w-5 h-5" />
          </div>

          <div className="min-w-0">
            <p className="text-xs font-bold text-slate-900 truncate">
              {formData.name.trim() || 'Category Name'}
            </p>
            <p className="text-[11px] text-slate-400 truncate">
              {formData.description.trim() || 'Category description preview'}
            </p>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Category Name */}
          <Input
            label="Category Name"
            name="name"
            placeholder="e.g. Groceries, Gym, Streaming"
            value={formData.name}
            onChange={handleChange}
            error={errors.name}
            required
          />

          {/* Description */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Description (Optional)
            </label>
            <textarea
              name="description"
              rows={2}
              placeholder="What belongs in this category..."
              value={formData.description}
              onChange={handleChange}
              className="block w-full rounded-xl border border-slate-200 bg-white text-sm text-slate-900 placeholder-slate-400 py-2 px-3.5 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          {/* Color Picker */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Color Accent
            </label>
            <div className="flex flex-wrap items-center gap-2 pt-0.5">
              {CATEGORY_COLORS.map((color) => (
                <button
                  key={color}
                  type="button"
                  onClick={() => setFormData((p) => ({ ...p, color }))}
                  className={`w-7 h-7 rounded-full transition-transform ${
                    formData.color === color
                      ? 'ring-2 ring-offset-2 ring-indigo-600 scale-110'
                      : 'hover:scale-105'
                  }`}
                  style={{ backgroundColor: color }}
                  aria-label={`Select color ${color}`}
                />
              ))}
            </div>
          </div>

          {/* Icon Picker Grid */}
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-slate-700 select-none">
              Icon
            </label>
            <div className="grid grid-cols-6 sm:grid-cols-8 gap-2 max-h-36 overflow-y-auto p-1 border border-slate-100 rounded-xl bg-slate-50/50">
              {CATEGORY_ICONS.map((item) => {
                const Icon = item.icon;
                const isSelected = formData.icon === item.name;

                return (
                  <button
                    key={item.name}
                    type="button"
                    title={item.label}
                    onClick={() => setFormData((p) => ({ ...p, icon: item.name }))}
                    className={`p-2 rounded-xl flex items-center justify-center transition-all ${
                      isSelected
                        ? 'bg-white shadow-sm border border-indigo-500 text-indigo-600'
                        : 'text-slate-500 hover:text-slate-800 hover:bg-white'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                  </button>
                );
              })}
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isSubmitting}
            >
              {isEditing ? 'Save Changes' : 'Create Category'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CategoryFormModal;
