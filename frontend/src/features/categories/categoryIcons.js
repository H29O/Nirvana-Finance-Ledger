import {
  ShoppingBag,
  ShoppingCart,
  Utensils,
  Home,
  Car,
  HeartPulse,
  Film,
  GraduationCap,
  Briefcase,
  Plane,
  Gift,
  Dumbbell,
  Coffee,
  Sparkles,
  Tag,
  Wifi,
  Zap,
  Smartphone,
  Receipt,
  Wallet,
  Shirt,
  Shield,
  BookOpen,
  CalendarCheck,
  MoreHorizontal,
} from 'lucide-react';

export const CATEGORY_ICONS = [
  { name: 'ShoppingBag', label: 'Shopping', icon: ShoppingBag },
  { name: 'ShoppingCart', label: 'Groceries', icon: ShoppingCart },
  { name: 'Utensils', label: 'Food & Dining', icon: Utensils },
  { name: 'Coffee', label: 'Cafe & Drinks', icon: Coffee },
  { name: 'Home', label: 'Housing & Rent', icon: Home },
  { name: 'Car', label: 'Transportation', icon: Car },
  { name: 'Zap', label: 'Bills & Utilities', icon: Zap },
  { name: 'Wifi', label: 'Internet', icon: Wifi },
  { name: 'Smartphone', label: 'Mobile & Recharge', icon: Smartphone },
  { name: 'HeartPulse', label: 'Healthcare & Medical', icon: HeartPulse },
  { name: 'Film', label: 'Entertainment', icon: Film },
  { name: 'GraduationCap', label: 'Education', icon: GraduationCap },
  { name: 'Briefcase', label: 'Work & Business', icon: Briefcase },
  { name: 'Plane', label: 'Travel', icon: Plane },
  { name: 'CalendarCheck', label: 'Subscriptions', icon: CalendarCheck },
  { name: 'MoreHorizontal', label: 'Other', icon: MoreHorizontal },
  { name: 'Gift', label: 'Gifts & Donations', icon: Gift },
  { name: 'Dumbbell', label: 'Fitness & Sports', icon: Dumbbell },
  { name: 'Shirt', label: 'Clothing', icon: Shirt },
  { name: 'Shield', label: 'Insurance & Taxes', icon: Shield },
  { name: 'BookOpen', label: 'Books & Learning', icon: BookOpen },
  { name: 'Wallet', label: 'Savings & Income', icon: Wallet },
  { name: 'Receipt', label: 'General Expense', icon: Receipt },
  { name: 'Tag', label: 'Tag', icon: Tag },
  { name: 'Sparkles', label: 'Personal Care', icon: Sparkles },
];

export const CATEGORY_COLORS = [
  '#4f46e5', // Indigo
  '#06b6d4', // Cyan
  '#10b981', // Emerald
  '#f59e0b', // Amber
  '#f43f5e', // Rose
  '#8b5cf6', // Violet
  '#ec4899', // Pink
  '#3b82f6', // Blue
  '#14b8a6', // Teal
  '#f97316', // Orange
  '#64748b', // Slate
  '#84cc16', // Lime
];

export function getCategoryIconComponent(iconName) {
  const item = CATEGORY_ICONS.find((i) => i.name === iconName);
  return item ? item.icon : Tag;
}
