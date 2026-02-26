import { apiClient } from './client';

export const userApi = {
  // 用户管理
  listUsers: (pageNum = 1, pageSize = 10, keyword = '') =>
    apiClient.get('/api/admin/users', {
      params: { pageNum, pageSize, keyword }
    }),

  getUser: (id) =>
    apiClient.get(`/api/admin/users/${id}`),

  createUser: (data) =>
    apiClient.post('/api/admin/users', data),

  updateUser: (id, data) =>
    apiClient.put(`/api/admin/users/${id}`, data),

  deleteUser: (id) =>
    apiClient.delete(`/api/admin/users/${id}`),

  toggleUserStatus: (id) =>
    apiClient.patch(`/api/admin/users/${id}/status`),

  assignRoles: (id, roleIds) =>
    apiClient.post(`/api/admin/users/${id}/roles`, { roleIds }),
};

export const roleApi = {
  // 角色管理
  listRoles: () =>
    apiClient.get('/api/admin/roles'),

  getRole: (id) =>
    apiClient.get(`/api/admin/roles/${id}`),

  createRole: (data) =>
    apiClient.post('/api/admin/roles', data),

  updateRole: (id, data) =>
    apiClient.put(`/api/admin/roles/${id}`, data),

  deleteRole: (id) =>
    apiClient.delete(`/api/admin/roles/${id}`),
};
