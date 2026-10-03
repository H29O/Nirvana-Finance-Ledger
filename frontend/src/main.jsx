import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './app/App';
import AppProviders from './app/providers';
import './styles/index.css';

// In development, ensure any stale Service Workers from previous builds are unregistered
if (import.meta.env.DEV && 'serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then((registrations) => {
    for (const registration of registrations) {
      registration.unregister();
    }
  });
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </React.StrictMode>
);
