export const STORAGE_KEYS = {
  TOKEN: 'finance_ledger_token',
  USER: 'finance_ledger_user',
};

export const APP_CONFIG = {
  NAME: 'Nivara',
  DESCRIPTION: 'Intelligent Personal Finance & Accounting System',
  VERSION: '1.0.0',
};

export const NAVIGATION_ITEMS = [
  {
    name: 'Dashboard',
    path: '/',
    icon: 'LayoutDashboard',
    description: 'Financial overview & intelligence',
  },
  {
    name: 'Transactions',
    path: '/transactions',
    icon: 'ArrowLeftRight',
    description: 'Income and expense records',
  },
  {
    name: 'Categories',
    path: '/categories',
    icon: 'Tags',
    description: 'Income & expense taxonomy',
  },
  {
    name: 'Lending & Borrowing',
    path: '/lending',
    icon: 'HandCoins',
    description: 'Loan tracking & repayments',
  },
  {
    name: 'Receipt Vault',
    path: '/receipts',
    icon: 'Receipt',
    description: 'OCR & digital receipts',
  },
  {
    name: 'Money Wrapped',
    path: '/money-wrapped',
    icon: 'Sparkles',
    description: 'Monthly & yearly spending story',
  },
  {
    name: 'Settings',
    path: '/settings',
    icon: 'Settings',
    description: 'Account profile & session',
  },
];
