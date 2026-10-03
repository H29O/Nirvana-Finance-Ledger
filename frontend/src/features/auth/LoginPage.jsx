import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import { Mail, Lock, AlertCircle, ArrowRight, Eye, EyeOff, CheckCircle2 } from 'lucide-react';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    email: location.state?.email || '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const destination = location.state?.from?.pathname || '/';
  const isVerifiedNotice = location.state?.verified;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.email || !formData.password) {
      setError('Please enter both email and password.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await login(formData);
      navigate(destination, { replace: true });
    } catch (err) {
      if (err.response?.data?.verificationRequired) {
        navigate('/verify-email', {
          state: {
            email: formData.email.trim(),
            message: err.response.data.error || 'Please verify your email before signing in.',
          },
        });
        return;
      }
      setError(err.normalizedMessage || 'Login failed. Please check your credentials.');
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
            <path d="M28 32C28 29.7909 29.7909 28 32 28H68C70.2091 28 72 29.7909 72 32V68C72 70.2091 70.2091 72 68 72H32C29.7909 72 28 70.2091 28 68V32Z" stroke="#ffffff" strokeWidth="4" strokeLinejoin="round" />
            <path d="M38 42H62" stroke="#e0e7ff" strokeWidth="4" strokeLinecap="round" />
            <path d="M38 52H52" stroke="#10b981" strokeWidth="4" strokeLinecap="round" />
            <path d="M38 62H58" stroke="#ffffff" strokeWidth="4" strokeLinecap="round" />
          </svg>
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Finance Ledger
        </h1>
        <p className="text-xs text-slate-500">
          Sign in to access your personal finance tracker
        </p>
      </div>

      {/* Login Card */}
      <Card className="w-full max-w-md bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        <CardHeader className="p-6 pb-4">
          <CardTitle className="text-lg">Welcome back</CardTitle>
          <CardDescription>
            Enter your credentials to access your transactions
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-0">
          <form onSubmit={handleSubmit} className="space-y-4">
            {error ? (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                <span>{error}</span>
              </div>
            ) : isVerifiedNotice ? (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-start gap-2.5 text-xs text-emerald-700">
                <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5 text-emerald-500" />
                <span>Email verified successfully. Please sign in to continue.</span>
              </div>
            ) : null}

            <Input
              label="Email Address"
              name="email"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              value={formData.email}
              onChange={handleChange}
              leftIcon={<Mail className="w-4 h-4" />}
              required
            />

            <Input
              label="Password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              autoComplete="current-password"
              value={formData.password}
              onChange={handleChange}
              leftIcon={<Lock className="w-4 h-4" />}
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="text-slate-400 hover:text-slate-600 focus:outline-none transition-colors p-0.5 rounded"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? (
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
              isLoading={isLoading}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Sign In
            </Button>
          </form>

          <div className="mt-6 pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
            Don&apos;t have an account?{' '}
            <Link
              to="/register"
              className="text-indigo-600 hover:text-indigo-700 font-semibold hover:underline transition-colors"
            >
              Create an account
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default LoginPage;
