import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import authService from '../../services/auth.service';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import NivaraBrand from '../../components/ui/NivaraLogo';
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
        <NivaraBrand
          size="lg"
          layout="vertical"
          subtitle="Account password recovery"
        />
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
