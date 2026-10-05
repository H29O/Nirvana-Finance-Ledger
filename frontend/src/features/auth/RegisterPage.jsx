import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import NivaraBrand from '../../components/ui/NivaraLogo';
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
  const [isDuplicateEmail, setIsDuplicateEmail] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError('');
    if (isDuplicateEmail) setIsDuplicateEmail(false);
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
    setIsDuplicateEmail(false);

    try {
      const response = await register(formData);
      if (response?.verificationRequired || !response?.token) {
        navigate('/verify-email', {
          state: {
            email: formData.email.trim(),
            name: formData.name.trim(),
            emailDeliverySuccess: response?.emailDeliverySuccess !== false,
            message: response?.emailDeliverySuccess === false
              ? "We couldn't send the verification email. Please try again."
              : 'Verification code sent.',
          },
          replace: true,
        });
      } else {
        navigate('/', { replace: true });
      }
    } catch (err) {
      const isDuplicate =
        err.response?.status === 409 ||
        (err.response?.data?.error && err.response.data.error.toLowerCase().includes('already exists'));

      if (isDuplicate) {
        setError('An account with this email already exists. Please sign in instead.');
        setIsDuplicateEmail(true);
      } else {
        setIsDuplicateEmail(false);
        setError(err.normalizedMessage || 'Registration failed. Please try again.');
      }
      // Security/UX convention: Clear password only, keep user's name and email in the form
      setFormData((prev) => ({ ...prev, password: '' }));
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
          subtitle="Create your personal finance workspace"
        />
      </div>

      {/* Registration Card */}
      <Card className="w-full max-w-md bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        <CardHeader className="p-6 pb-4">
          <CardTitle className="text-lg">Create Account</CardTitle>
          <CardDescription>
            Sign up to start tracking your daily expenses with clarity
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 pt-0">
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                <div className="flex-1">
                  <span>{error}</span>
                  {isDuplicateEmail && (
                    <div className="mt-1.5">
                      <Link
                        to="/login"
                        state={{ email: formData.email }}
                        className="font-semibold text-rose-800 hover:text-rose-950 underline inline-flex items-center gap-1"
                      >
                        Sign in now &rarr;
                      </Link>
                    </div>
                  )}
                </div>
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
              state={{ email: formData.email }}
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
