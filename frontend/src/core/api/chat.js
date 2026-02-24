import { apiClient } from './client';

export async function fetchModels() {
  const resp = await apiClient.get('/api/models');
  return resp.data || [];
}

export async function fetchTools() {
  const resp = await apiClient.get('/api/tools');
  return resp.data || [];
}

export async function fetchSkills() {
  const resp = await apiClient.get('/api/skills');
  return resp.data || [];
}

export async function fetchAdminSkills() {
  const resp = await apiClient.get('/api/admin/skills');
  return resp.data || [];
}

export async function upsertSkill(payload) {
  const resp = await apiClient.post('/api/admin/skills', payload);
  return resp.data;
}

export async function deleteSkill(skillName, version) {
  await apiClient.delete('/api/admin/skills', { params: { skillName, version } });
}

export async function importSkillsSh(script) {
  const resp = await apiClient.post('/api/admin/skills/import-sh', { script });
  return resp.data;
}

export async function importSkillsSource(source) {
  const resp = await apiClient.post('/api/admin/skills/import-source', { source });
  return resp.data;
}

export async function chatOnce(payload) {
  const resp = await apiClient.post('/api/chat', payload);
  return resp.data;
}

export function buildStreamUrl({ conversationId, model, message, tools = [], skills = [] }) {
  const params = new URLSearchParams({
    conversationId,
    model,
    message,
  });
  tools.forEach((tool) => params.append('tools', tool));
  skills.forEach((skill) => params.append('skills', skill));
  return `/api/chat/stream?${params.toString()}`;
}

export async function fetchConversations() {
  const resp = await apiClient.get('/api/conversations');
  return resp.data || [];
}

export async function fetchConversationMessages(conversationId) {
  const resp = await apiClient.get(`/api/conversations/${encodeURIComponent(conversationId)}/messages`);
  return resp.data || [];
}

export async function clearConversation(conversationId) {
  await apiClient.delete(`/api/conversations/${encodeURIComponent(conversationId)}`);
}

// ── Model Admin API ──────────────────────────────────────

export async function fetchAdminModels() {
  const resp = await apiClient.get('/api/admin/models');
  return resp.data || [];
}

export async function upsertModel(payload) {
  const resp = await apiClient.post('/api/admin/models', payload);
  return resp.data;
}

export async function deleteModel(modelId) {
  await apiClient.delete('/api/admin/models', { params: { modelId } });
}

export async function toggleModel(modelId) {
  const resp = await apiClient.patch(`/api/admin/models/${encodeURIComponent(modelId)}/toggle`);
  return resp.data;
}
