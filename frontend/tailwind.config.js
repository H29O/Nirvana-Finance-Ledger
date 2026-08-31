/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: '#090d16',
        surface: {
          DEFAULT: '#0f172a',
          subtle: '#111c35',
          elevated: '#172342',
          border: '#1e293b',
          'border-subtle': '#162032',
        },
        brand: {
          50: '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
          950: '#1e1b4b',
        },
        finance: {
          income: '#10b981', // emerald-500
          'income-bg': 'rgba(16, 185, 129, 0.1)',
          expense: '#f43f5e', // rose-500
          'expense-bg': 'rgba(244, 63, 94, 0.1)',
          neutral: '#64748b', // slate-500
          accent: '#0ea5e9', // sky-500
        }
      },
      fontFamily: {
        sans: [
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          'Roboto',
          'sans-serif',
        ],
        mono: [
          'JetBrains Mono',
          'Fira Code',
          'ui-monospace',
          'SFMono-Regular',
          'Menlo',
          'monospace',
        ],
      },
      boxShadow: {
        'subtle': '0 1px 2px 0 rgba(0, 0, 0, 0.3)',
        'card': '0 4px 20px -2px rgba(0, 0, 0, 0.4), 0 2px 6px -1px rgba(0, 0, 0, 0.2)',
        'elevated': '0 10px 30px -5px rgba(0, 0, 0, 0.6), 0 4px 10px -2px rgba(0, 0, 0, 0.3)',
      }
    },
  },
  plugins: [],
}
