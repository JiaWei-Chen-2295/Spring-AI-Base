import { create } from 'zustand';
import { fetchConversations, fetchConversationMessages } from '../api/chat';

/**
 * Message shape:
 *  { role: 'user' | 'assistant' | 'tool_call', content: string, ts: number, toolCalls?: [] }
 *
 * tool_call messages are injected between user and assistant to show tool invocation traces.
 */
export const useChatStore = create((set, get) => ({
  models: [],
  modelId: '',
  conversationId: 'c1',
  conversations: [],
  loadingModels: false,
  loadingHistory: false,
  sending: false,
  messages: [],
  error: '',
  streamMode: true,
  selectedTools: [],
  selectedSkills: [],

  setModels: (models) => {
    const current = get().modelId;
    const stillExists = models.some((m) => m.modelId === current);
    const selected = (stillExists && current) || (models[0] && models[0].modelId) || '';
    set({ models, modelId: selected });
  },

  setModelId: (modelId) => set({ modelId }),
  setConversationId: (conversationId) => set({ conversationId }),
  setConversations: (conversations) => set({ conversations }),
  setSending: (sending) => set({ sending }),
  setError: (error) => set({ error }),
  setLoadingModels: (loadingModels) => set({ loadingModels }),
  setStreamMode: (streamMode) => set({ streamMode }),
  setSelectedTools: (selectedTools) => set({ selectedTools }),
  setSelectedSkills: (selectedSkills) => set({ selectedSkills }),

  /** Load conversation list from backend */
  loadConversations: async () => {
    try {
      const data = await fetchConversations();
      set({ conversations: data.map((c) => c.conversationId) });
    } catch (_) {
      // silently ignore — conversations list is non-critical
    }
  },

  /** Switch to a conversation and load its history from backend */
  switchConversation: async (conversationId) => {
    set({ conversationId, messages: [], loadingHistory: true, error: '' });
    try {
      const data = await fetchConversationMessages(conversationId);
      const messages = data.map((m) => ({
        role: m.role,
        content: m.content,
        ts: 0,
      }));
      set({ messages, loadingHistory: false });
    } catch (_) {
      set({ loadingHistory: false });
    }
  },

  /** Create a new conversation with a generated ID */
  newConversation: () => {
    const id = 'c-' + Date.now().toString(36);
    set({ conversationId: id, messages: [], error: '' });
  },

  addUserMessage: (content) =>
    set((state) => ({
      messages: [...state.messages, { role: 'user', content, ts: Date.now() }],
    })),

  addAssistantPlaceholder: () =>
    set((state) => ({
      messages: [...state.messages, { role: 'assistant', content: '', ts: Date.now(), toolCalls: [] }],
    })),

  /** Attach applied skills to the last assistant message */
  setAppliedSkills: (skills) =>
    set((state) => {
      if (!state.messages.length) return state;
      const next = [...state.messages];
      const last = next[next.length - 1];
      if (last.role !== 'assistant') return state;
      next[next.length - 1] = { ...last, appliedSkills: skills };
      return { messages: next };
    }),

  /** Append a tool_call trace to the last assistant message's toolCalls array */
  addToolCall: (toolCall) =>
    set((state) => {
      if (!state.messages.length) return state;
      const next = [...state.messages];
      const last = next[next.length - 1];
      if (last.role !== 'assistant') return state;
      const updatedCalls = [...(last.toolCalls || []), toolCall];
      next[next.length - 1] = { ...last, toolCalls: updatedCalls };
      return { messages: next };
    }),

  /** Upsert live tool progress by callId on the last assistant message */
  upsertToolCallProgress: (progress) =>
    set((state) => {
      if (!state.messages.length) return state;
      const next = [...state.messages];
      const last = next[next.length - 1];
      if (last.role !== 'assistant') return state;
      const toolCalls = [...(last.toolCalls || [])];
      const idx = toolCalls.findIndex((tc) => tc.callId && tc.callId === progress.callId);
      if (idx >= 0) {
        toolCalls[idx] = { ...toolCalls[idx], ...progress };
      } else {
        toolCalls.push(progress);
      }
      next[next.length - 1] = { ...last, toolCalls };
      return { messages: next };
    }),

  /** Bulk set tool calls on the last assistant message (for POST responses) */
  setToolCalls: (toolCalls) =>
    set((state) => {
      if (!state.messages.length) return state;
      const next = [...state.messages];
      const last = next[next.length - 1];
      if (last.role !== 'assistant') return state;
      next[next.length - 1] = { ...last, toolCalls: toolCalls || [] };
      return { messages: next };
    }),

  appendAssistantText: (delta) =>
    set((state) => {
      if (!state.messages.length) return state;
      const next = [...state.messages];
      const last = next[next.length - 1];
      if (last.role !== 'assistant') return state;
      next[next.length - 1] = { ...last, content: `${last.content}${delta}` };
      return { messages: next };
    }),

  setAssistantText: (content) =>
    set((state) => {
      if (!state.messages.length) return state;
      const next = [...state.messages];
      const last = next[next.length - 1];
      if (last.role !== 'assistant') return state;
      next[next.length - 1] = { ...last, content };
      return { messages: next };
    }),

  clearMessages: () => set({ messages: [] }),
}));
