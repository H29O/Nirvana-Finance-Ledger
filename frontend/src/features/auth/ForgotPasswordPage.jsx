import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import authService from '../../services/auth.service';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import { Mail, ArrowLeft, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';

export function ForgotPasswordPage() {
  const location = useLocation();

  const [email, setEmail] = useState(location.state?.email || '');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      setError('Please enter your email address.');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmedEmail)) {
      setError('Please enter a valid email address.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await authService.forgotPassword({ email: trimmedEmail });
      setSubmitted(true);
    } catch (err) {
      // Always maintain neutral response to avoid leaking account existence
      setSubmitted(true);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center items-center px-4 py-12 bg-slate-50">
      {/* Brand Header */}
      <div className="w-full max-w-md mb-8 text-center space-y-2">
        <div className="inline-flex items-center justify-center p-3 rounded-2xl bg-indigo-50 border border-indigo-100 shadow-sm">
          <svg className="w-8 h-8" viewBox="0 0 100 100" fill="none">
            <rect width="100" height="100" rx="24" fill="#4f46e5" />
            <path
              d="M28 32C28 29.7909 29.7909 28 32 28H68C70.2091 28 72 29.7909 72 32V68C72 70.2091 70.2091 72 68 72H32C29.7909 72 28 70.2091 28 68V32Z"
              stroke="#ffffff"
              strokeWidth="4"
              strokeLinejoin="round"
            />
            <path d="M38 42H62" stroke="#e0e7ff" strokeWidth="4" strokeLinecap="round" />
            <path d="M38 52H52" stroke="#10b981" strokeWidth="4" strokeLinecap="round" />
            <path d="M38 62H58" stroke="#ffffff" strokeWidth="4" strokeLinecap="round" />
          </svg>
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Finance Ledger
        </h1>
        <p className="text-xs text-slate-500">
          Account password recovery
        </p>
      </div>

      {/* Card Container */}
      <Card className="w-full max-w-md bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        <CardHeader className="p-6 pb-4">
          <CardTitle className="text-lg">Forgot your password?</CardTitle>
          <CardDescription>
            {submitted
              ? 'Check your inbox for password reset instructions'
              : 'Enter your registered email address to receive a secure password reset link'}
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-0">
          {submitted ? (
            <div className="space-y-5">
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200/80 text-emerald-800 text-xs leading-relaxed space-y-2">
                <div className="flex items-center gap-2 font-semibold text-emerald-900">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>Reset link dispatched</span>
                </div>
                <p>
                  If an account exists for this email, we&apos;ve sent a password reset link.
                </p>
                <p className="text-emerald-700/90">
                  The link will expire in <strong>15 minutes</strong>. Please check your inbox and spam folder.
                </p>
              </div>

              <div className="pt-2 flex flex-col gap-2">
                <Link to="/login" state={{ email: email.trim() }}>
                  <Button variant="primary" className="w-full text-xs">
                    Return to Login
                  </Button>
                </Link>

                <button
                  type="button"
                  onClick={() => setSubmitted(false)}
                  className="w-full py-2 text-xs font-medium text-slate-500 hover:text-slate-700 text-center transition-colors"
                >
                  Send to a different email
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                  <span>{error}</span>
                </div>
              )}

              <Input
                label="Email Address"
                name="email"
                type="email"
                placeholder="you@example.com"
                autoComplete="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (error) setError('');
                }}
                leftIcon={<Mail className="w-4 h-4" />}
                required
                autoFocus
              />

              <Button
                type="submit"
                className="w-full mt-2"
                isLoading={isLoading}
                rightIcon={<ArrowRight className="w-4 h-4" />}
              >
                Send Reset Link
              </Button>

              <div className="mt-6 pt-4 border-t border-slate-100 text-center">
                <Link
                  to="/login"
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-700 hover:underline transition-colors"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Back to Sign In</span>
                </Link>
              </div>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default ForgotPasswordPage;
