import { apiClient } from './client';

export const authApi = {
  login: (username, password) =>
    apiClient.post('/api/auth/login', { username, password }),

  logout: () =>
    apiClient.post('/api/auth/logout'),

  refresh: (refreshToken) =>
    apiClient.post('/api/auth/refresh', refreshToken, {
      headers: { 'Content-Type': 'text/plain' }
    }),

  getCurrentUser: () =>
    apiClient.get('/api/auth/me'),

  changePassword: (oldPassword, newPassword) =>
    apiClient.put('/api/auth/password', { oldPassword, newPassword }),
};
