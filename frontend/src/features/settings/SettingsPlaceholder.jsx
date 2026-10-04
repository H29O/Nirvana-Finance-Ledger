import React from 'react';
import PlaceholderPage from '../../components/common/PlaceholderPage';
import { Settings } from 'lucide-react';

export function SettingsPlaceholder() {
  return (
    <PlaceholderPage
      title="Settings & Profile"
      subtitle="Manage your personal profile and account settings."
      icon={Settings}
      capabilities={[
        'User profile details (name, email, member since timestamp)',
        'Device installation and application management',
        'Data export and ledger maintenance tools',
      ]}
    />
  );
}

export default SettingsPlaceholder;
