import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import authService from '../../services/auth.service';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import Spinner from '../../components/ui/Spinner';
import NivaraBrand from '../../components/ui/NivaraLogo';
import { Lock, AlertCircle, ArrowRight, Eye, EyeOff, CheckCircle2, RotateCcw } from 'lucide-react';

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: '',
  });
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // States
  const [isValidatingToken, setIsValidatingToken] = useState(true);
  const [tokenError, setTokenError] = useState('');
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  // Validate token on mount
  useEffect(() => {
    let isMounted = true;

    async function checkToken() {
      if (!token || token.trim() === '') {
        if (isMounted) {
          setTokenError('This password reset link is invalid or has expired.');
          setIsValidatingToken(false);
        }
        return;
      }

      try {
        await authService.validateResetToken(token.trim());
        if (isMounted) {
          setTokenError('');
        }
      } catch (err) {
        if (isMounted) {
          setTokenError(
            err.normalizedMessage ||
              err.response?.data?.error ||
              'This password reset link is invalid or has expired.'
          );
        }
      } finally {
        if (isMounted) {
          setIsValidatingToken(false);
        }
      }
    }

    checkToken();

    return () => {
      isMounted = false;
    };
  }, [token]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (formError) setFormError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const { newPassword, confirmPassword } = formData;

    if (!newPassword || !confirmPassword) {
      setFormError('Please fill in both password fields.');
      return;
    }

    if (newPassword.length < 6) {
      setFormError('Password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setFormError('New password and confirmation do not match.');
      return;
    }

    setIsSubmitting(true);
    setFormError('');

    try {
      await authService.resetPassword({
        token: token.trim(),
        newPassword: newPassword.trim(),
        confirmPassword: confirmPassword.trim(),
      });
      setIsSuccess(true);
    } catch (err) {
      const errorMsg =
        err.normalizedMessage ||
        err.response?.data?.error ||
        err.response?.data?.message ||
        'Failed to reset password. Please try again.';

      if (
        errorMsg.toLowerCase().includes('invalid') ||
        errorMsg.toLowerCase().includes('expired') ||
        errorMsg.toLowerCase().includes('not found')
      ) {
        setTokenError('This password reset link is invalid or has expired.');
      } else {
        setFormError(errorMsg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center items-center px-4 py-12 bg-slate-50">
      {/* Brand Header */}
      <div className="w-full max-w-md mb-8 text-center space-y-2">
        <NivaraBrand
          size="lg"
          layout="vertical"
          subtitle="Create a new secure password"
        />
      </div>

      {/* Card Container */}
      <Card className="w-full max-w-md bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        <CardHeader className="p-6 pb-4">
          <CardTitle className="text-lg">
            {isSuccess
              ? 'Password Changed'
              : tokenError
              ? 'Invalid Reset Link'
              : 'Reset Your Password'}
          </CardTitle>
          <CardDescription>
            {isSuccess
              ? 'Your password has been updated successfully'
              : tokenError
              ? 'The link you followed is no longer active'
              : 'Choose a strong password to protect your account'}
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-0">
          {/* 1. Loading Verification State */}
          {isValidatingToken ? (
            <div className="py-10 flex flex-col items-center justify-center space-y-3">
              <Spinner size="lg" className="text-indigo-600" />
              <p className="text-xs text-slate-500 font-medium">
                Validating security link...
              </p>
            </div>
          ) : tokenError ? (
            /* 2. Token Error State */
            <div className="space-y-5">
              <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-3 text-xs text-rose-800">
                <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <p className="font-semibold text-rose-900">
                    {tokenError}
                  </p>
                  <p className="text-rose-700/90 leading-relaxed">
                    Password reset links are single-use and expire after 15 minutes for your security.
                  </p>
                </div>
              </div>

              <div className="flex flex-col gap-2 pt-2">
                <Link to="/forgot-password">
                  <Button
                    variant="primary"
                    className="w-full text-xs flex items-center justify-center gap-1.5"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Request a new link</span>
                  </Button>
                </Link>

                <Link to="/login">
                  <Button variant="ghost" className="w-full text-xs text-slate-600">
                    Return to Login
                  </Button>
                </Link>
              </div>
            </div>
          ) : isSuccess ? (
            /* 3. Success State */
            <div className="space-y-5 text-center">
              <div className="w-12 h-12 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-600 flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-6 h-6" />
              </div>

              <div className="space-y-1.5">
                <h3 className="text-sm font-bold text-slate-900">
                  Password changed successfully.
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed max-w-sm mx-auto">
                  Your credentials have been securely updated. You can now use your new password to sign into your finance workspace.
                </p>
              </div>

              <div className="pt-2">
                <Button
                  variant="primary"
                  onClick={() =>
                    navigate('/login', {
                      replace: true,
                      state: { message: 'Password changed successfully. Please log in.' },
                    })
                  }
                  className="w-full text-xs flex items-center justify-center gap-1.5"
                  rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                >
                  Continue to Login
                </Button>
              </div>
            </div>
          ) : (
            /* 4. Reset Password Form */
            <form onSubmit={handleSubmit} className="space-y-4">
              {formError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                  <span>{formError}</span>
                </div>
              )}

              <Input
                label="New Password"
                name="newPassword"
                type={showNewPassword ? 'text' : 'password'}
                placeholder="Minimum 6 characters"
                autoComplete="new-password"
                value={formData.newPassword}
                onChange={handleChange}
                leftIcon={<Lock className="w-4 h-4" />}
                rightIcon={
                  <button
                    type="button"
                    onClick={() => setShowNewPassword((prev) => !prev)}
                    className="text-slate-400 hover:text-slate-600 focus:outline-none transition-colors p-0.5 rounded"
                    aria-label={showNewPassword ? 'Hide password' : 'Show password'}
                  >
                    {showNewPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                }
                helperText="Must be at least 6 characters"
                required
                autoFocus
              />

              <Input
                label="Confirm New Password"
                name="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Re-enter your new password"
                autoComplete="new-password"
                value={formData.confirmPassword}
                onChange={handleChange}
                leftIcon={<Lock className="w-4 h-4" />}
                rightIcon={
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                    className="text-slate-400 hover:text-slate-600 focus:outline-none transition-colors p-0.5 rounded"
                    aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                }
                required
              />

              <Button
                type="submit"
                className="w-full mt-2"
                isLoading={isSubmitting}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Reset Password
              </Button>

              <div className="mt-6 pt-4 border-t border-slate-100 text-center">
                <Link
                  to="/login"
                  className="text-xs font-semibold text-slate-500 hover:text-slate-700 hover:underline transition-colors"
                >
                  Cancel and return to Login
                </Link>
              </div>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default ResetPasswordPage;
