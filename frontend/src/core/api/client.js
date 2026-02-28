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

// Response interceptor - handle auth errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url;

    if (status === 401) {
      console.warn(`[Auth] 401 Unauthorized — clearing session and redirecting to login (url: ${url})`);
      localStorage.removeItem('auth-storage');
      window.location.href = '/login';
    } else if (status === 403) {
      console.warn(`[Auth] 403 Forbidden — insufficient permissions (url: ${url})`);
      // Don't redirect to login; the user is authenticated but lacks permission.
      // Let the calling component handle the error via the rejected Promise.
    } else if (status >= 500) {
      console.error(`[API] Server error ${status} for ${url}:`, error.response?.data);
    }

    return Promise.reject(error);
  }
);
