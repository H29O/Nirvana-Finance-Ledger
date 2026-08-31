import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import { User, Mail, Lock, AlertCircle, ArrowRight, Eye, EyeOff } from 'lucide-react';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name || !formData.email || !formData.password) {
      setError('Please fill in all required fields.');
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await register(formData);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.normalizedMessage || 'Registration failed. Please try again.');
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
          Create Account
        </h1>
        <p className="text-xs text-slate-500">
          Start tracking your daily expenses with clarity
        </p>
      </div>

      {/* Registration Card */}
      <Card className="w-full max-w-md bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        <CardHeader className="p-6 pb-4">
          <CardTitle className="text-lg">Sign Up</CardTitle>
          <CardDescription>
            Create your isolated finance workspace
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-0">
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                <span>{error}</span>
              </div>
            )}

            <Input
              label="Full Name"
              name="name"
              type="text"
              placeholder="e.g. Rahul Sharma"
              autoComplete="name"
              value={formData.name}
              onChange={handleChange}
              leftIcon={<User className="w-4 h-4" />}
              required
            />

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
              placeholder="Minimum 6 characters"
              autoComplete="new-password"
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
              helperText="Must be at least 6 characters"
              required
            />

            <Button
              type="submit"
              className="w-full mt-2"
              isLoading={isLoading}
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Create Account
            </Button>
          </form>

          <div className="mt-6 pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
            Already have an account?{' '}
            <Link
              to="/login"
              className="text-indigo-600 hover:text-indigo-700 font-semibold hover:underline transition-colors"
            >
              Sign In
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default RegisterPage;
