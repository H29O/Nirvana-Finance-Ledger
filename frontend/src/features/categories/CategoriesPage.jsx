import React, { useState, useEffect, useCallback, useMemo } from 'react';
import categoryService from '../../services/category.service';
import PageHeader from '../../components/ui/PageHeader';
import Button from '../../components/ui/Button';
import CategoryCard from './components/CategoryCard';
import CategoriesSkeleton from './components/CategoriesSkeleton';
import CategoriesEmptyState from './components/CategoriesEmptyState';
import CategoryFormModal from './components/CategoryFormModal';
import CategoryDeleteModal from './components/CategoryDeleteModal';
import { PlusCircle, RotateCcw, AlertCircle, CheckCircle2, Search } from 'lucide-react';

export function CategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Modals state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [deletingCategory, setDeletingCategory] = useState(null);

  const loadCategories = useCallback(async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    else setIsRefreshing(true);
    setError('');

    try {
      const data = await categoryService.getAllCategories();
      setCategories(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(
        err.normalizedMessage || 'Failed to load categories. Please try again.'
      );
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  // Auto-dismiss success message after 4s
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => {
        setSuccessMessage('');
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  const handleOpenAddModal = () => {
    setEditingCategory(null);
    setIsFormOpen(true);
  };

  const handleOpenEditModal = (category) => {
    setEditingCategory(category);
    setIsFormOpen(true);
  };

  const handleOpenDeleteModal = (category) => {
    setDeletingCategory(category);
    setIsDeleteOpen(true);
  };

  const handleConfirmDelete = async (categoryId) => {
    await categoryService.deleteCategory(categoryId);
    setSuccessMessage('Category deleted successfully.');
    loadCategories(true);
  };

  const handleFormSuccess = (msg) => {
    setSuccessMessage(msg);
    loadCategories(true);
  };

  // Filter categories by search
  const filteredCategories = useMemo(() => {
    if (!searchQuery.trim()) return categories;
    const q = searchQuery.toLowerCase();
    return categories.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        (c.description && c.description.toLowerCase().includes(q))
    );
  }, [categories, searchQuery]);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Categories"
        subtitle="Organize spending and understand financial habits with customizable categories."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => loadCategories(true)}
              isLoading={isRefreshing}
              title="Refresh"
              aria-label="Refresh"
              className="p-2 text-slate-500 hover:text-slate-900"
            >
              <RotateCcw className="w-4 h-4" />
            </Button>

            <Button
              variant="primary"
              size="sm"
              onClick={handleOpenAddModal}
              leftIcon={<PlusCircle className="w-4 h-4" />}
            >
              Add Category
            </Button>
          </div>
        }
      />

      {/* Success Notification Banner */}
      {successMessage && (
        <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800 shadow-sm animate-fade-in">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{successMessage}</span>
          </div>

          <button
            type="button"
            onClick={() => setSuccessMessage('')}
            className="text-emerald-700 hover:text-emerald-900 text-xs font-semibold"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 flex items-center justify-between text-xs text-rose-700 shadow-sm">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-500 shrink-0" />
            <span>{error}</span>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => loadCategories(false)}
            className="text-xs border-rose-300 hover:bg-rose-100 text-rose-700"
          >
            Retry
          </Button>
        </div>
      )}

      {/* Main Content Area */}
      {isLoading ? (
        <CategoriesSkeleton />
      ) : categories.length === 0 ? (
        <CategoriesEmptyState onAddClick={handleOpenAddModal} />
      ) : (
        <div className="space-y-4">
          {/* Search bar when categories exist */}
          <div className="flex items-center justify-between gap-4 max-w-sm">
            <div className="relative w-full">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search categories..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3.5 py-2 text-xs rounded-xl border border-slate-200 bg-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
              />
            </div>
          </div>

          {filteredCategories.length === 0 ? (
            <div className="p-8 text-center bg-white border border-slate-200/80 rounded-2xl">
              <p className="text-xs text-slate-500">
                No categories matching &quot;{searchQuery}&quot;
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {filteredCategories.map((category) => (
                <CategoryCard
                  key={category.id}
                  category={category}
                  onEditClick={handleOpenEditModal}
                  onDeleteClick={handleOpenDeleteModal}
                />
              ))}
            </div>
          )}
        </div>
      )}

      {/* Add / Edit Category Form Modal */}
      <CategoryFormModal
        isOpen={isFormOpen}
        onClose={() => {
          setIsFormOpen(false);
          setEditingCategory(null);
        }}
        category={editingCategory}
        onSuccess={handleFormSuccess}
      />

      {/* Delete Category Confirmation Modal */}
      <CategoryDeleteModal
        isOpen={isDeleteOpen}
        onClose={() => {
          setIsDeleteOpen(false);
          setDeletingCategory(null);
        }}
        category={deletingCategory}
        onConfirmDelete={handleConfirmDelete}
      />
    </div>
  );
}

export default CategoriesPage;
