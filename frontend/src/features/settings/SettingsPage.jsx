import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import userService from '../../services/user.service';
import PageHeader from '../../components/ui/PageHeader';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { formatDate } from '../../lib/formatters';
import {
  User,
  Calendar,
  Download,
  LogOut,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Trash2,
  Smartphone,
} from 'lucide-react';

export function SettingsPage() {
  const navigate = useNavigate();
  const { user: authUser, logout } = useAuth();

  const [profile, setProfile] = useState(authUser);
  const [isLoading, setIsLoading] = useState(!authUser);
  const [error, setError] = useState('');

  // PWA install state
  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [isAppInstalled, setIsAppInstalled] = useState(false);
  const [isInstalling, setIsInstalling] = useState(false);

  // Logout modal state
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);

  // Delete account modal state
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

  // Load fresh user profile from backend /api/users/me
  useEffect(() => {
    let isMounted = true;

    async function fetchProfile() {
      try {
        const freshUser = await userService.getCurrentUser();
        if (isMounted) {
          setProfile(freshUser);
        }
      } catch (err) {
        if (isMounted) {
          setError(
            err.normalizedMessage || 'Failed to refresh user profile from server.'
          );
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    fetchProfile();

    return () => {
      isMounted = false;
    };
  }, []);

  // Listen for browser PWA beforeinstallprompt event
  useEffect(() => {
    // Check if running in standalone mode (already installed)
    if (
      window.matchMedia('(display-mode: standalone)').matches ||
      window.navigator.standalone === true
    ) {
      setIsAppInstalled(true);
    }

    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };

    const handleAppInstalled = () => {
      setIsAppInstalled(true);
      setDeferredPrompt(null);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const handleInstallApp = async () => {
    if (!deferredPrompt) return;

    setIsInstalling(true);
    try {
      deferredPrompt.prompt();
      const choiceResult = await deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        setIsAppInstalled(true);
      }
      setDeferredPrompt(null);
    } catch (err) {
      console.warn('PWA install prompt error', err);
    } finally {
      setIsInstalling(false);
    }
  };

  const handleConfirmLogout = () => {
    setIsLogoutModalOpen(false);
    logout();
  };

  const handleConfirmDeleteAccount = async () => {
    if (deleteConfirmText !== 'DELETE' || isDeleting) return;

    setIsDeleting(true);
    setDeleteError('');

    try {
      await userService.deleteAccount();
      logout();
      sessionStorage.clear();
      setIsDeleteModalOpen(false);
      navigate('/login', {
        replace: true,
        state: { message: 'Your account has been permanently deleted.' },
      });
    } catch (err) {
      setDeleteError(
        err.normalizedMessage ||
          err.response?.data?.error ||
          err.response?.data?.message ||
          'Failed to delete account. Please try again.'
      );
      setIsDeleting(false);
    }
  };

  const userInitial = profile?.name
    ? profile.name.charAt(0).toUpperCase()
    : 'U';

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-16">
      {/* Page Header */}
      <PageHeader
        title="Settings"
        subtitle="Manage your account profile and session."
      />

      {/* Global Error Banner */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setError('');
              userService.getCurrentUser().then(setProfile).catch(() => {});
            }}
            className="text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Retry
          </Button>
        </div>
      )}

      {/* 1. Profile Section */}
      <Card className="p-6 md:p-7 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)] space-y-6">
        <CardHeader className="p-0 pb-4 flex flex-row items-center justify-between border-b border-slate-100">
          <div>
            <CardTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
              <User className="w-4 h-4 text-indigo-600" />
              <span>User Profile</span>
            </CardTitle>
            <CardDescription className="text-xs text-slate-500 mt-0.5">
              Personal identity details registered with your ledger
            </CardDescription>
          </div>

          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/80">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>{profile?.emailVerified ? 'Verified Account' : 'Active Account'}</span>
          </span>
        </CardHeader>

        <CardContent className="p-0">
          {isLoading ? (
            <div className="flex items-center gap-4 animate-pulse py-2">
              <div className="w-16 h-16 rounded-2xl bg-slate-200" />
              <div className="space-y-2 flex-1">
                <div className="h-4 w-40 bg-slate-200 rounded" />
                <div className="h-3 w-56 bg-slate-100 rounded" />
              </div>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
              {/* Avatar Initial Badge */}
              <div className="w-16 h-16 rounded-2xl bg-indigo-600 text-white font-extrabold text-2xl flex items-center justify-center shadow-md shrink-0">
                {userInitial}
              </div>

              {/* User Details Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 flex-1 w-full text-xs">
                <div className="space-y-1 p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                    Full Name
                  </span>
                  <p className="text-sm font-bold text-slate-900">
                    {profile?.name || '—'}
                  </p>
                </div>

                <div className="space-y-1 p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                    Email Address
                  </span>
                  <p className="text-sm font-bold text-slate-900 truncate">
                    {profile?.email || '—'}
                  </p>
                </div>

                <div className="space-y-1 p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                    Member Since
                  </span>
                  <p className="text-xs font-semibold text-slate-700 flex items-center gap-1.5 pt-0.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>{profile?.createdAt ? formatDate(profile.createdAt) : '—'}</span>
                  </p>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 2. Device App Installation (Shown only when install prompt is available) */}
      {deferredPrompt && !isAppInstalled && (
        <Card className="p-6 md:p-7 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)]">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <Smartphone className="w-4 h-4 text-indigo-600" />
                <span>Install Application</span>
              </h3>
              <p className="text-xs text-slate-500">
                Install Nivara on this device for quick access from your desktop or home screen
              </p>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={handleInstallApp}
              isLoading={isInstalling}
              className="text-xs flex items-center gap-1.5 shadow-sm shrink-0 self-start sm:self-center"
            >
              <Download className="w-4 h-4" />
              <span>Install App</span>
            </Button>
          </div>
        </Card>
      )}

      {/* 3. Account Session Actions / Logout Section */}
      <Card className="p-6 md:p-7 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)]">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <LogOut className="w-4 h-4 text-indigo-600" />
              <span>Account Session</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Safely sign out of your current session on this browser
            </p>
          </div>

          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsLogoutModalOpen(true)}
            className="flex items-center gap-1.5 shadow-sm self-start sm:self-center"
          >
            <LogOut className="w-4 h-4" />
            <span>Log Out</span>
          </Button>
        </div>
      </Card>

      {/* 4. Danger Zone Section */}
      <Card className="p-6 md:p-7 bg-white border border-rose-200 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)]">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-rose-600 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-rose-600" />
              <span>Danger Zone</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Permanently delete your account and all associated personal data
            </p>
          </div>

          <Button
            variant="danger"
            size="sm"
            onClick={() => {
              setDeleteConfirmText('');
              setDeleteError('');
              setIsDeleteModalOpen(true);
            }}
            className="flex items-center gap-1.5 shadow-sm self-start sm:self-center bg-rose-600 hover:bg-rose-700 focus:ring-rose-500"
          >
            <Trash2 className="w-4 h-4" />
            <span>Delete Account</span>
          </Button>
        </div>
      </Card>

      {/* Logout Confirmation Modal */}
      {isLogoutModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
            onClick={() => setIsLogoutModalOpen(false)}
            aria-hidden="true"
          />

          {/* Modal Container */}
          <div className="relative w-full max-w-sm bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-4 animate-scale-in">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600 shrink-0">
                <LogOut className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Log Out of Ledger?
                </h3>
                <p className="text-xs text-slate-500">
                  You will need to sign back in to access your records
                </p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Are you sure you want to log out from{' '}
              <strong className="text-slate-900">{profile?.email || 'this session'}</strong>?
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsLogoutModalOpen(false)}
                className="text-xs"
              >
                Cancel
              </Button>

              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmLogout}
                className="text-xs"
              >
                Confirm Log Out
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Account Deletion Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
            onClick={() => {
              if (!isDeleting) {
                setIsDeleteModalOpen(false);
                setDeleteConfirmText('');
                setDeleteError('');
              }
            }}
            aria-hidden="true"
          />

          {/* Modal Container */}
          <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-rose-100 p-6 z-10 space-y-5 animate-scale-in">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600 shrink-0 mt-0.5">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Delete your account?
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Permanent and irreversible account removal
                </p>
              </div>
            </div>

            <div className="space-y-2.5 text-xs text-slate-600 leading-relaxed">
              <p>
                This will permanently delete your Nivara account and all associated data, including transactions, categories, receipts, lending and borrowing records, and other account data.
              </p>
              <p className="font-semibold text-rose-600">
                This action cannot be undone.
              </p>
            </div>

            {deleteError && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                <span>{deleteError}</span>
              </div>
            )}

            <div className="space-y-2">
              <label
                htmlFor="delete-confirm-input"
                className="block text-xs font-medium text-slate-700"
              >
                Type <span className="font-mono font-bold text-rose-600">DELETE</span> to confirm:
              </label>
              <Input
                id="delete-confirm-input"
                type="text"
                placeholder="DELETE"
                value={deleteConfirmText}
                onChange={(e) => setDeleteConfirmText(e.target.value)}
                disabled={isDeleting}
                className="font-mono text-center tracking-wider font-semibold"
                autoFocus
              />
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setIsDeleteModalOpen(false);
                  setDeleteConfirmText('');
                  setDeleteError('');
                }}
                disabled={isDeleting}
                className="text-xs"
              >
                Cancel
              </Button>

              <Button
                variant="danger"
                size="sm"
                onClick={handleConfirmDeleteAccount}
                disabled={deleteConfirmText !== 'DELETE' || isDeleting}
                isLoading={isDeleting}
                className="text-xs bg-rose-600 hover:bg-rose-700 focus:ring-rose-500"
              >
                Delete Account Permanently
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default SettingsPage;
