import { buildStreamUrl } from '../core/api/chat';

/**
 * Send a message via SSE streaming.
 * Returns a Promise that resolves when done or rejects on error.
 */
export function sendStream({
  conversationId,
  modelId,
  message,
  tools,
  skills,
  appendAssistantText,
  addToolCall,
  setError,
  setStreamState,
  eventSourceRef,
}) {
  return new Promise((resolve, reject) => {
    const url = buildStreamUrl({ conversationId, model: modelId, message, tools, skills });
    const source = new EventSource(url);
    eventSourceRef.current = source;
    setStreamState('connecting');

    source.addEventListener('open', () => {
      setStreamState('streaming');
    });

    source.addEventListener('tool_call', (event) => {
      try {
        const tc = JSON.parse(event.data);
        addToolCall(tc);
      } catch (_) {
        // ignore malformed tool_call events
      }
    });

    source.addEventListener('token', (event) => {
      let delta = event.data || '';
      try {
        const parsed = JSON.parse(event.data);
        if (parsed && typeof parsed.token === 'string') {
          delta = parsed.token;
        }
      } catch (_) {
        // Keep backward compatibility with plain-string SSE payloads.
      }
      appendAssistantText(delta);
    });

    source.addEventListener('done', () => {
      source.close();
      eventSourceRef.current = null;
      setStreamState('idle');
      resolve();
    });

    source.addEventListener('error', () => {
      source.close();
      eventSourceRef.current = null;
      setStreamState('error');
      setError('流式连接中断');
      reject(new Error('SSE stream disconnected'));
    });
  });
}
