import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE || '';

export const apiClient = axios.create({
  baseURL,
  timeout: 60000,
});

apiClient.interceptors.request.use((config) => {
  const requestId = `web-${Date.now()}`;
  config.headers['x-request-id'] = requestId;
  return config;
});
