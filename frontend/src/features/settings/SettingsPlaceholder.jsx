import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { Settings } from 'lucide-react';

export function SettingsPlaceholder() {
  return (
    <PlaceholderPage
      title="Settings & Profile"
      subtitle="Manage your personal profile, security credentials, currency preferences, and application settings."
      icon={Settings}
      capabilities={[
        'User profile details (name, email, member since timestamp)',
        'Currency format and localized date formatting preferences',
        'Offline PWA caching and device management',
        'Data export and ledger maintenance tools',
      ]}
    />
  );
}

export default SettingsPlaceholder;
