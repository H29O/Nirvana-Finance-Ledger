import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import authService from '../../services/auth.service';
import Card, { CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { Mail, CheckCircle2, AlertCircle, ArrowLeft, RotateCw } from 'lucide-react';

/**
 * Maps verification errors from backend into clear user-facing messages.
 */
export function mapVerificationError(err) {
  if (!err) return 'Something went wrong. Please try again.';

  if (!err.response) {
    return 'Something went wrong. Please try again.';
  }

  const status = err.response.status;
  const backendMsg = (err.response.data?.error || err.normalizedMessage || '').toLowerCase();

  if (backendMsg.includes('expired') || backendMsg.includes('no active verification')) {
    return 'This code has expired. Please request a new code.';
  }

  if (status === 429 || backendMsg.includes('too many') || backendMsg.includes('invalidated')) {
    return 'Too many attempts. Please request a new code.';
  }

  if (backendMsg.includes('already verified')) {
    return 'Your email is already verified. Please sign in.';
  }

  if (status === 400 || backendMsg.includes('invalid verification code') || backendMsg.includes('attempt')) {
    return 'Invalid verification code. Please try again.';
  }

  if (status >= 500) {
    return 'Something went wrong. Please try again.';
  }

  return err.response.data?.error || 'Invalid verification code. Please try again.';
}

export function VerifyEmailPage() {
  const { verifyEmail } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Retrieve email and delivery status passed from RegisterPage or LoginPage
  const email = location.state?.email || '';
  const initialMessage = location.state?.message || '';
  const initialDeliverySuccess = location.state?.emailDeliverySuccess;

  const isInitialDeliveryFailure =
    initialDeliverySuccess === false ||
    (initialMessage && initialMessage.toLowerCase().includes('could not be sent'));

  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState(
    isInitialDeliveryFailure ? "We couldn't send the verification email. Please try again." : ''
  );
  const [successMessage, setSuccessMessage] = useState(
    isInitialDeliveryFailure
      ? ''
      : initialMessage && initialMessage.toLowerCase().includes('sent')
      ? 'Verification code sent.'
      : ''
  );
  const [isVerified, setIsVerified] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [cooldown, setCooldown] = useState(isInitialDeliveryFailure ? 0 : 60);

  const inputRefs = useRef([]);

  // Auto-focus first input on mount
  useEffect(() => {
    if (!isVerified && inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, [isVerified]);

  // Cooldown countdown timer
  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => {
      setCooldown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  const handleDigitChange = (index, value) => {
    // Clear any previous error or success banners as user types
    if (error) setError('');
    if (successMessage) setSuccessMessage('');

    // Only accept numeric digit
    const cleaned = value.replace(/\D/g, '');
    if (!cleaned) {
      const nextDigits = [...otpDigits];
      nextDigits[index] = '';
      setOtpDigits(nextDigits);
      return;
    }

    const digit = cleaned.slice(-1);
    const nextDigits = [...otpDigits];
    nextDigits[index] = digit;
    setOtpDigits(nextDigits);

    // Advance focus to next input
    if (index < 5 && inputRefs.current[index + 1]) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (error) setError('');
    if (successMessage) setSuccessMessage('');

    if (e.key === 'Backspace') {
      if (!otpDigits[index] && index > 0 && inputRefs.current[index - 1]) {
        inputRefs.current[index - 1].focus();
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1].focus();
    } else if (e.key === 'ArrowRight' && index < 5) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    if (error) setError('');
    if (successMessage) setSuccessMessage('');

    const pastedData = e.clipboardData.getData('text').trim().replace(/\D/g, '');
    if (!pastedData) return;

    const digits = pastedData.slice(0, 6).split('');
    const nextDigits = ['', '', '', '', '', ''];
    digits.forEach((d, idx) => {
      if (idx < 6) nextDigits[idx] = d;
    });
    setOtpDigits(nextDigits);

    // Focus last filled or 6th input
    const targetIdx = Math.min(digits.length, 5);
    if (inputRefs.current[targetIdx]) {
      inputRefs.current[targetIdx].focus();
    }
  };

  const fullOtp = otpDigits.join('');

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    if (isLoading || isVerified) return;

    if (!email) {
      setError('Missing email address. Please return to the login screen.');
      setSuccessMessage('');
      return;
    }
    if (fullOtp.length !== 6) {
      setError('Please enter all 6 digits of the verification code.');
      setSuccessMessage('');
      return;
    }

    setIsLoading(true);
    setError('');
    setSuccessMessage('');

    try {
      await verifyEmail({ email, otp: fullOtp });
      setIsVerified(true);
      setError('');
      setSuccessMessage('');
    } catch (err) {
      const msg = mapVerificationError(err);
      setError(msg);
      setSuccessMessage('');
      // Clear OTP inputs so user can enter again
      setOtpDigits(['', '', '', '', '', '']);
      setTimeout(() => {
        inputRefs.current[0]?.focus();
      }, 0);

      if (err.response?.data?.retryAfterSeconds) {
        setCooldown(err.response.data.retryAfterSeconds);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || isResending || isLoading) return;
    if (!email) {
      setError('Missing email address. Please return to the login screen.');
      setSuccessMessage('');
      return;
    }

    setIsResending(true);
    setError('');
    setSuccessMessage('');

    try {
      await authService.resendVerification({ email });
      setSuccessMessage('Verification code sent.');
      setError('');
      setCooldown(60);
      setOtpDigits(['', '', '', '', '', '']);
      setTimeout(() => {
        inputRefs.current[0]?.focus();
      }, 0);
    } catch (err) {
      setSuccessMessage('');
      if (err.response?.data?.retryAfterSeconds) {
        setCooldown(err.response.data.retryAfterSeconds);
        setError(err.response.data.error || 'Please wait before requesting a new code.');
      } else if (err.response?.status === 502 || err.response?.data?.emailDeliverySuccess === false) {
        setError("We couldn't send the verification email. Please try again.");
      } else if (err.response?.status >= 500) {
        setError("Something went wrong. Please try again.");
      } else if (!err.response) {
        setError("Something went wrong. Please try again.");
      } else {
        setError(err.response?.data?.error || "We couldn't send the verification email. Please try again.");
      }
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center items-center px-4 py-12 bg-slate-50">
      {/* Brand Header */}
      <div className="w-full max-w-md mb-8 text-center space-y-2">
        <div className="inline-flex items-center justify-center p-3 rounded-2xl bg-indigo-50 border border-indigo-100 shadow-sm">
          <Mail className="w-8 h-8 text-indigo-600" />
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Verify your email
        </h1>
        <p className="text-xs text-slate-500">
          6-digit verification code for
        </p>
        <p className="text-sm font-semibold text-slate-800 break-all">
          {email || 'your email address'}
        </p>
      </div>

      {/* Verification Card */}
      <Card className="w-full max-w-md bg-white border-slate-200/80 shadow-[0_1px_3px_0_rgba(0,0,0,0.04)]">
        {isVerified ? (
          <CardContent className="p-8 text-center space-y-6">
            <div className="inline-flex items-center justify-center p-3.5 rounded-2xl bg-emerald-50 border border-emerald-100 shadow-sm mx-auto">
              <CheckCircle2 className="w-8 h-8 text-emerald-600" />
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold tracking-tight text-slate-900">
                Email verified successfully
              </h2>
              <p className="text-sm text-slate-500 max-w-xs mx-auto">
                Your email has been verified. You can now sign in to Nivara.
              </p>
            </div>

            <Button
              type="button"
              variant="primary"
              className="w-full justify-center py-2.5 rounded-xl font-medium shadow-sm hover:shadow"
              onClick={() => navigate('/login', { state: { email, verified: true }, replace: true })}
            >
              Continue to Login
            </Button>
          </CardContent>
        ) : (
          <>
            <CardHeader className="p-6 pb-4">
              <CardTitle className="text-lg">Enter Code</CardTitle>
              <CardDescription>
                Enter the 6-digit code to activate your account
              </CardDescription>
            </CardHeader>

            <CardContent className="p-6 pt-0 space-y-5">
              {/* Notification Banners: strictly mutually exclusive */}
              {error ? (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-700">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
                  <span>{error}</span>
                </div>
              ) : successMessage ? (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-start gap-2.5 text-xs text-emerald-700">
                  <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5 text-emerald-500" />
                  <span>{successMessage}</span>
                </div>
              ) : null}

              <form onSubmit={handleSubmit} className="space-y-6">
                {/* 6-Digit OTP Box Grid */}
                <div className="flex justify-between items-center gap-2 sm:gap-3" onPaste={handlePaste}>
                  {otpDigits.map((digit, idx) => (
                    <input
                      key={idx}
                      ref={(el) => (inputRefs.current[idx] = el)}
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={1}
                      value={digit}
                      onChange={(e) => handleDigitChange(idx, e.target.value)}
                      onKeyDown={(e) => handleKeyDown(idx, e)}
                      disabled={isLoading}
                      className="w-12 h-14 sm:w-14 sm:h-16 text-center text-xl sm:text-2xl font-bold text-slate-800 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:border-indigo-600 focus:ring-4 focus:ring-indigo-100 outline-none transition-all disabled:opacity-50"
                      aria-label={`Digit ${idx + 1}`}
                    />
                  ))}
                </div>

                <div className="text-center">
                  <span className="text-xs text-slate-400">
                    Code expires in <strong className="text-slate-600">5 minutes</strong>
                  </span>
                </div>

                {/* Verify Button */}
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full justify-center py-2.5 rounded-xl font-medium shadow-sm hover:shadow"
                  isLoading={isLoading}
                  disabled={isLoading || fullOtp.length !== 6}
                >
                  Verify & Continue
                </Button>
              </form>

              {/* Resend & Secondary Actions */}
              <div className="pt-2 border-t border-slate-100 flex flex-col items-center gap-3">
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={cooldown > 0 || isResending || isLoading}
                  className={`inline-flex items-center gap-1.5 text-xs font-medium transition-colors ${
                    cooldown > 0 || isResending || isLoading
                      ? 'text-slate-400 cursor-not-allowed'
                      : 'text-indigo-600 hover:text-indigo-700 hover:underline'
                  }`}
                >
                  <RotateCw className={`w-3.5 h-3.5 ${isResending ? 'animate-spin' : ''}`} />
                  {cooldown > 0
                    ? `Resend code in ${cooldown}s`
                    : isResending
                    ? 'Sending code...'
                    : 'Resend verification code'}
                </button>

                <Link
                  to="/login"
                  className="inline-flex items-center gap-1 text-xs text-slate-500 hover:text-slate-800 transition-colors"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  Back to Sign In
                </Link>
              </div>
            </CardContent>
          </>
        )}
      </Card>
    </div>
  );
}

export default VerifyEmailPage;
