import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import userService from '../../services/user.service';
import authService from '../../services/auth.service';
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
  Lock,
  KeyRound,
  Eye,
  EyeOff,
  RotateCcw,
  X,
} from 'lucide-react';

export function SettingsPage() {
  const navigate = useNavigate();
  const { user: authUser, logout } = useAuth();

  const [profile, setProfile] = useState(authUser);
  const [isLoading, setIsLoading] = useState(!authUser);
  const [error, setError] = useState('');

  // Change password modal state
  const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState(false);
  const [changePasswordForm, setChangePasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [changePasswordError, setChangePasswordError] = useState('');
  const [isIncorrectCurrentPassword, setIsIncorrectCurrentPassword] = useState(false);
  const [isSendingResetEmail, setIsSendingResetEmail] = useState(false);
  const [resetEmailSentMessage, setResetEmailSentMessage] = useState('');

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

  const handleConfirmChangePassword = async (e) => {
    e.preventDefault();
    const { currentPassword, newPassword, confirmPassword } = changePasswordForm;

    if (!currentPassword || !newPassword || !confirmPassword) {
      setChangePasswordError('Please fill in all password fields.');
      return;
    }

    if (newPassword.length < 6) {
      setChangePasswordError('New password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setChangePasswordError('New password and confirmation do not match.');
      return;
    }

    if (newPassword === currentPassword) {
      setChangePasswordError('New password must be different from your current password.');
      return;
    }

    setIsChangingPassword(true);
    setChangePasswordError('');
    setIsIncorrectCurrentPassword(false);

    try {
      await authService.changePassword({
        currentPassword,
        newPassword: newPassword.trim(),
        confirmPassword: confirmPassword.trim(),
      });
      setIsChangePasswordModalOpen(false);
      // Because sessions/JWTs are invalidated after password change, logout and redirect to login
      logout();
      navigate('/login', {
        replace: true,
        state: { message: 'Password changed successfully. Please log in with your new password.' },
      });
    } catch (err) {
      const msg =
        err.normalizedMessage ||
        err.response?.data?.error ||
        err.response?.data?.message ||
        'Failed to change password. Please check your current password.';

      setChangePasswordError(msg);
      if (
        msg.toLowerCase().includes('current password is incorrect') ||
        msg.toLowerCase().includes('current password')
      ) {
        setIsIncorrectCurrentPassword(true);
      }
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleForgotPasswordFromModal = async () => {
    if (profile?.email) {
      setIsSendingResetEmail(true);
      setChangePasswordError('');
      try {
        await authService.forgotPassword({ email: profile.email });
        setResetEmailSentMessage(
          `We've sent a password reset link to ${profile.email}. Please check your inbox.`
        );
      } catch (err) {
        setResetEmailSentMessage(
          `We've sent a password reset link to ${profile.email}. Please check your inbox.`
        );
      } finally {
        setIsSendingResetEmail(false);
      }
    } else {
      setIsChangePasswordModalOpen(false);
      navigate('/forgot-password');
    }
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

      {/* 3. Password & Security Section */}
      <Card className="p-6 md:p-7 bg-white border border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.03)]">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <KeyRound className="w-4 h-4 text-indigo-600" />
              <span>Password & Security</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Update your account password to keep your personal finance workspace secure
            </p>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setChangePasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
              setChangePasswordError('');
              setIsIncorrectCurrentPassword(false);
              setResetEmailSentMessage('');
              setIsChangePasswordModalOpen(true);
            }}
            className="flex items-center gap-1.5 shadow-sm self-start sm:self-center border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            <Lock className="w-4 h-4 text-slate-500" />
            <span>Change Password</span>
          </Button>
        </div>
      </Card>

      {/* 4. Account Session Actions / Logout Section */}
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

      {/* Change Password Modal */}
      {isChangePasswordModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
            onClick={() => {
              if (!isChangingPassword && !isSendingResetEmail) {
                setIsChangePasswordModalOpen(false);
                setChangePasswordError('');
                setIsIncorrectCurrentPassword(false);
                setResetEmailSentMessage('');
              }
            }}
            aria-hidden="true"
          />

          {/* Modal Container */}
          <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 p-6 z-10 space-y-4 animate-scale-in">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
                  <KeyRound className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Change Password
                  </h3>
                  <p className="text-xs text-slate-500">
                    Protect your account with a new secure password
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  if (!isChangingPassword && !isSendingResetEmail) {
                    setIsChangePasswordModalOpen(false);
                    setChangePasswordError('');
                    setIsIncorrectCurrentPassword(false);
                    setResetEmailSentMessage('');
                  }
                }}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg transition-colors"
                aria-label="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {resetEmailSentMessage ? (
              <div className="space-y-4 pt-2">
                <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs space-y-2">
                  <div className="flex items-center gap-2 font-bold text-emerald-900">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Password Reset Link Sent</span>
                  </div>
                  <p className="leading-relaxed">{resetEmailSentMessage}</p>
                  <p className="text-emerald-700/90 text-[11px]">
                    The reset link will expire in 15 minutes.
                  </p>
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => {
                      setIsChangePasswordModalOpen(false);
                      setResetEmailSentMessage('');
                    }}
                    className="text-xs"
                  >
                    Close
                  </Button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleConfirmChangePassword} className="space-y-4 pt-1">
                {changePasswordError && (
                  <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 space-y-2 text-xs text-rose-700">
                    <div className="flex items-start gap-2">
                      <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                      <span className="flex-1 font-medium">{changePasswordError}</span>
                    </div>

                    {isIncorrectCurrentPassword && (
                      <div className="pt-2 border-t border-rose-200/80 flex items-center justify-between">
                        <span className="text-[11px] text-rose-800">
                          Forgotten your current password?
                        </span>
                        <button
                          type="button"
                          onClick={handleForgotPasswordFromModal}
                          disabled={isSendingResetEmail}
                          className="font-bold text-indigo-700 hover:text-indigo-900 underline text-xs transition-colors"
                        >
                          {isSendingResetEmail ? 'Sending link...' : 'Forgot password?'}
                        </button>
                      </div>
                    )}
                  </div>
                )}

                <Input
                  label="Current Password"
                  name="currentPassword"
                  type={showCurrentPassword ? 'text' : 'password'}
                  placeholder="Enter current password"
                  autoComplete="current-password"
                  value={changePasswordForm.currentPassword}
                  onChange={(e) => {
                    setChangePasswordForm((prev) => ({ ...prev, currentPassword: e.target.value }));
                    if (changePasswordError) setChangePasswordError('');
                    if (isIncorrectCurrentPassword) setIsIncorrectCurrentPassword(false);
                  }}
                  leftIcon={<Lock className="w-4 h-4" />}
                  rightIcon={
                    <button
                      type="button"
                      onClick={() => setShowCurrentPassword((prev) => !prev)}
                      className="text-slate-400 hover:text-slate-600 focus:outline-none transition-colors p-0.5 rounded"
                      aria-label={showCurrentPassword ? 'Hide password' : 'Show password'}
                    >
                      {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  }
                  required
                />

                <Input
                  label="New Password"
                  name="newPassword"
                  type={showNewPassword ? 'text' : 'password'}
                  placeholder="Minimum 6 characters"
                  autoComplete="new-password"
                  value={changePasswordForm.newPassword}
                  onChange={(e) => {
                    setChangePasswordForm((prev) => ({ ...prev, newPassword: e.target.value }));
                    if (changePasswordError) setChangePasswordError('');
                  }}
                  leftIcon={<Lock className="w-4 h-4" />}
                  rightIcon={
                    <button
                      type="button"
                      onClick={() => setShowNewPassword((prev) => !prev)}
                      className="text-slate-400 hover:text-slate-600 focus:outline-none transition-colors p-0.5 rounded"
                      aria-label={showNewPassword ? 'Hide password' : 'Show password'}
                    >
                      {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  }
                  helperText="Must be at least 6 characters"
                  required
                />

                <Input
                  label="Confirm New Password"
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  placeholder="Re-enter new password"
                  autoComplete="new-password"
                  value={changePasswordForm.confirmPassword}
                  onChange={(e) => {
                    setChangePasswordForm((prev) => ({ ...prev, confirmPassword: e.target.value }));
                    if (changePasswordError) setChangePasswordError('');
                  }}
                  leftIcon={<Lock className="w-4 h-4" />}
                  rightIcon={
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((prev) => !prev)}
                      className="text-slate-400 hover:text-slate-600 focus:outline-none transition-colors p-0.5 rounded"
                      aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                    >
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  }
                  required
                />

                <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setIsChangePasswordModalOpen(false);
                      setChangePasswordError('');
                      setIsIncorrectCurrentPassword(false);
                    }}
                    disabled={isChangingPassword}
                    className="text-xs"
                  >
                    Cancel
                  </Button>

                  <Button
                    type="submit"
                    variant="primary"
                    size="sm"
                    isLoading={isChangingPassword}
                    className="text-xs"
                  >
                    Change Password
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default SettingsPage;
