import { apiClient } from './client';

export const getSettings = () => apiClient.get('/api/admin/settings').then(r => r.data);

export const setSetting = (key, value) =>
  apiClient.put(`/api/admin/settings/${encodeURIComponent(key)}`, { value }).then(r => r.data);
