import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { authApi, fetchConfig } from '../api/auth';

export const useAuthStore = create(
  persist(
    (set, get) => ({
      // State
      token: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      authRequired: true, // assume auth required until /api/config responds

      // Actions

      /** Check backend config to determine if auth is required. */
      checkAuthRequired: async () => {
        try {
          const config = await fetchConfig();
          set({ authRequired: !!config.authEnabled });
        } catch (_) {
          // If the call fails, keep the safe default (authRequired=true)
        }
      },

      login: async (username, password) => {
        set({ isLoading: true, error: null });
        try {
          const response = await authApi.login(username, password);
          const { accessToken, refreshToken, user } = response.data;

          set({
            token: accessToken,
            refreshToken,
            user,
            isAuthenticated: true,
            isLoading: false,
          });
          return true;
        } catch (error) {
          set({
            error: error.response?.data?.message || '登录失败',
            isLoading: false,
            isAuthenticated: false,
          });
          return false;
        }
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch (e) {
          // ignore error
        }
        set({
          token: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
          error: null,
        });
      },

      refreshUser: async () => {
        try {
          const response = await authApi.getCurrentUser();
          set({ user: response.data });
        } catch (error) {
          console.error('Failed to refresh user:', error);
        }
      },

      clearError: () => set({ error: null }),

      // Getters
      hasRole: (roleCode) => {
        const { user } = get();
        return user?.roles?.some(r => r.roleCode === roleCode) || false;
      },

      isAdmin: () => {
        return get().hasRole('ADMIN');
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
