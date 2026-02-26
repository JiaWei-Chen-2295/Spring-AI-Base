import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE || '';

export const apiClient = axios.create({
  baseURL,
  timeout: 60000,
});

// Request interceptor - add request ID and auth token
apiClient.interceptors.request.use((config) => {
  const requestId = `web-${Date.now()}`;
  config.headers['x-request-id'] = requestId;
  
  // Add JWT token if available
  const authStorage = localStorage.getItem('auth-storage');
  if (authStorage) {
    try {
      const auth = JSON.parse(authStorage);
      if (auth.state?.token) {
        config.headers['Authorization'] = `Bearer ${auth.state.token}`;
      }
    } catch (e) {
      console.error('Failed to parse auth storage:', e);
    }
  }
  
  return config;
});

// Response interceptor - handle 401 errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear auth and redirect to login
      localStorage.removeItem('auth-storage');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
