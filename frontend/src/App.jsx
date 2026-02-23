import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Collapse, Input, Select, Space, Switch, Tag, Typography } from 'antd';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {
  buildStreamUrl,
  chatOnce,
  clearConversation,
  deleteSkill,
  fetchAdminSkills,
  fetchModels,
  fetchTools,
  importSkillsSh,
  importSkillsSource,
  upsertSkill,
} from './core/api/chat';
import { useChatStore } from './core/state/chatStore';

const { TextArea } = Input;
const { Title, Text } = Typography;

/* ─── helpers ─── */

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function truncate(s, max = 120) {
  if (!s) return '';
  return s.length <= max ? s : s.slice(0, max) + '...';
}

/* ─── sub-components ─── */

function ToolCallCard({ toolCalls }) {
  if (!toolCalls || !toolCalls.length) return null;

  const items = toolCalls.map((tc, i) => ({
    key: String(i),
    label: (
      <div className="tc-header">
        <span className="tc-icon">&#9881;</span>
        <span className="tc-name">{tc.toolName}</span>
        <Tag color="processing" style={{ marginLeft: 8, fontSize: 11 }}>{tc.durationMs}ms</Tag>
      </div>
    ),
    children: (
      <div className="tc-body">
        <div className="tc-section">
          <div className="tc-label">Input</div>
          <pre className="tc-pre">{tc.input || '(empty)'}</pre>
        </div>
        <div className="tc-section">
          <div className="tc-label">Output</div>
          <pre className="tc-pre">{tc.output || '(empty)'}</pre>
        </div>
      </div>
    ),
  }));

  return (
    <div className="tool-call-card">
      <div className="tc-title-row">
        <span className="tc-badge">{toolCalls.length} Tool Call{toolCalls.length > 1 ? 's' : ''}</span>
      </div>
      <Collapse
        size="small"
        items={items}
        bordered={false}
        defaultActiveKey={toolCalls.length <= 2 ? toolCalls.map((_, i) => String(i)) : ['0']}
      />
    </div>
  );
}

function MessageBubble({ msg, isLatest, sending }) {
  const isUser = msg.role === 'user';
  const isThinking = isLatest && sending && !msg.content;

  return (
    <div className={`msg-row ${isUser ? 'msg-row-user' : 'msg-row-assistant'}`}>
      <div className={`msg-avatar ${isUser ? 'avatar-user' : 'avatar-ai'}`}>
        {isUser ? 'U' : 'AI'}
      </div>
      <div className={`msg-bubble ${isUser ? 'bubble-user' : 'bubble-ai'}`}>
        <div className="msg-meta">
          <span className="msg-role-label">{isUser ? 'You' : 'Assistant'}</span>
          {msg.ts && <span className="msg-time">{formatTime(msg.ts)}</span>}
        </div>

        {/* Tool calls displayed before the response text */}
        {!isUser && msg.toolCalls && msg.toolCalls.length > 0 && (
          <ToolCallCard toolCalls={msg.toolCalls} />
        )}

        {isThinking ? (
          <div className="thinking-indicator">
            <span className="dot" />
            <span className="dot" />
            <span className="dot" />
          </div>
        ) : isUser ? (
          <div className="msg-text">{msg.content}</div>
        ) : (
          <div className="msg-markdown">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content || ''}</ReactMarkdown>
          </div>
        )}
      </div>
    </div>
  );
}

/* ─── main app ─── */

function App() {
  const {
    models,
    modelId,
    conversationId,
    conversations,
    messages,
    streamMode,
    sending,
    loadingModels,
    loadingHistory,
    error,
    setModels,
    setModelId,
    setError,
    setSending,
    setLoadingModels,
    setStreamMode,
    loadConversations,
    switchConversation,
    newConversation,
    addUserMessage,
    addAssistantPlaceholder,
    appendAssistantText,
    setAssistantText,
    addToolCall,
    setToolCalls,
    clearMessages,
  } = useChatStore();

  const [input, setInput] = useState('');
  const [tools, setTools] = useState([]);
  const [skills, setSkills] = useState([]);
  const [selectedTools, setSelectedTools] = useState([]);
  const [selectedSkills, setSelectedSkills] = useState([]);
  const [skillNameInput, setSkillNameInput] = useState('');
  const [skillVersionInput, setSkillVersionInput] = useState('1.0.0');
  const [skillContentInput, setSkillContentInput] = useState('');
  const [skillScriptInput, setSkillScriptInput] = useState('');
  const [skillSourceInput, setSkillSourceInput] = useState('');
  const [skillAdminBusy, setSkillAdminBusy] = useState(false);
  const [skillAdminMessage, setSkillAdminMessage] = useState('');
  const [activeTab, setActiveTab] = useState('runtime');
  const [lastRequestInfo, setLastRequestInfo] = useState('');
  const [streamState, setStreamState] = useState('idle');

  const eventSourceRef = useRef(null);
  const messagesEndRef = useRef(null);

  const reloadSkills = async () => {
    const skillData = await fetchAdminSkills();
    setSkills(skillData);
  };

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        setLoadingModels(true);
        const [modelData, toolData, skillData] = await Promise.all([
          fetchModels(),
          fetchTools(),
          fetchAdminSkills(),
        ]);
        if (mounted) {
          setModels(modelData);
          setTools(toolData);
          setSkills(skillData);
          setError('');
        }
      } catch (e) {
        if (mounted) {
          setError(`模型加载失败: ${e.message}`);
        }
      } finally {
        if (mounted) {
          setLoadingModels(false);
        }
      }
    };
    load();
    loadConversations();
    return () => {
      mounted = false;
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, [setError, setLoadingModels, setModels, loadConversations]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, sending]);

  const modelOptions = useMemo(
    () => models.map((m) => ({ value: m.modelId, label: `${m.modelId} (${m.provider})` })),
    [models]
  );
  const toolOptions = useMemo(
    () => tools.map((t) => ({ value: t.toolName, label: `${t.toolName} [${t.riskLevel}]` })),
    [tools]
  );
  const skillOptions = useMemo(
    () => skills.map((s) => ({ value: `${s.skillName}@${s.version}`, label: `${s.skillName}@${s.version}` })),
    [skills]
  );
  const selectedSkillEntries = useMemo(() => {
    const refs = new Set(selectedSkills);
    return skills.filter((s) => refs.has(`${s.skillName}@${s.version}`));
  }, [skills, selectedSkills]);

  const userCount = messages.filter((m) => m.role === 'user').length;
  const assistantCount = messages.filter((m) => m.role === 'assistant').length;

  const stopStreaming = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
      setStreamState('stopped');
      setSending(false);
    }
  };

  const fillPrompt = (prompt) => setInput(prompt);

  const onSend = async () => {
    const message = input.trim();
    if (!message || !modelId || sending) return;

    setError('');
    setSending(true);
    setStreamState(streamMode ? 'connecting' : 'idle');
    addUserMessage(message);
    addAssistantPlaceholder();
    setInput('');
    setLastRequestInfo(`${modelId} | tools:${selectedTools.length} | skills:${selectedSkills.length}`);

    try {
      if (streamMode) {
        await sendStream({
          conversationId,
          modelId,
          message,
          tools: selectedTools,
          skills: selectedSkills,
          appendAssistantText,
          addToolCall,
          setError,
          setStreamState,
          eventSourceRef,
        });
      } else {
        const resp = await chatOnce({
          conversationId,
          modelId,
          message,
          tools: selectedTools,
          skills: selectedSkills,
        });
        setAssistantText(resp.content || '');
        if (resp.toolCalls && resp.toolCalls.length) {
          setToolCalls(resp.toolCalls);
        }
      }
    } catch (e) {
      setAssistantText('');
      setError(`发送失败: ${e.message}`);
    } finally {
      setSending(false);
      if (!streamMode) {
        setStreamState('idle');
      }
      loadConversations();
    }
  };

  const onUpsertSkill = async () => {
    if (!skillNameInput.trim() || !skillContentInput.trim()) {
      setSkillAdminMessage('Skill name and content are required.');
      return;
    }
    try {
      setSkillAdminBusy(true);
      await upsertSkill({
        skillName: skillNameInput.trim(),
        version: (skillVersionInput || '1.0.0').trim(),
        content: skillContentInput,
      });
      await reloadSkills();
      setSkillAdminMessage('Skill saved.');
    } catch (e) {
      setSkillAdminMessage(`Save failed: ${e.message}`);
    } finally {
      setSkillAdminBusy(false);
    }
  };

  const onImportSkillsSh = async () => {
    if (!skillScriptInput.trim()) {
      setSkillAdminMessage('Paste skills.sh content first.');
      return;
    }
    try {
      setSkillAdminBusy(true);
      const result = await importSkillsSh(skillScriptInput);
      await reloadSkills();
      if (result.errors && result.errors.length) {
        setSkillAdminMessage(`Imported ${result.imported}, with errors: ${result.errors.join(' | ')}`);
      } else {
        setSkillAdminMessage(`Imported ${result.imported} skill(s).`);
      }
    } catch (e) {
      setSkillAdminMessage(`Import failed: ${e.message}`);
    } finally {
      setSkillAdminBusy(false);
    }
  };

  const onImportSkillSource = async () => {
    if (!skillSourceInput.trim()) {
      setSkillAdminMessage('Enter owner/repo, owner/repo@skill, or skills.sh URL.');
      return;
    }
    try {
      setSkillAdminBusy(true);
      const result = await importSkillsSource(skillSourceInput.trim());
      await reloadSkills();
      if (result.errors && result.errors.length) {
        setSkillAdminMessage(`Imported ${result.imported}, with errors: ${result.errors.join(' | ')}`);
      } else {
        setSkillAdminMessage(`Imported ${result.imported} skill(s) from source.`);
      }
    } catch (e) {
      setSkillAdminMessage(`Source import failed: ${e.message}`);
    } finally {
      setSkillAdminBusy(false);
    }
  };

  const onDeleteSelectedSkill = async () => {
    if (!selectedSkills.length) {
      setSkillAdminMessage('Select a skill first.');
      return;
    }
    const first = selectedSkillEntries[0];
    if (!first) {
      setSkillAdminMessage('Selected skill not found.');
      return;
    }
    if (!first.editable) {
      setSkillAdminMessage('Builtin skill cannot be deleted.');
      return;
    }
    try {
      setSkillAdminBusy(true);
      await deleteSkill(first.skillName, first.version);
      setSelectedSkills((prev) => prev.filter((ref) => ref !== `${first.skillName}@${first.version}`));
      await reloadSkills();
      setSkillAdminMessage('Skill deleted.');
    } catch (e) {
      setSkillAdminMessage(`Delete failed: ${e.message}`);
    } finally {
      setSkillAdminBusy(false);
    }
  };

  return (
    <div className="console-shell">
      <aside className="console-sidebar">
        <div className="brand-block">
          <div className="brand-dot" />
          <div>
            <div className="brand-title">AI Control Console</div>
            <div className="brand-sub">Runbook + Chat Ops</div>
          </div>
        </div>

        <Card className="panel control-panel" variant="borderless">
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <div>
              <Text className="label">Model</Text>
              <Select
                style={{ width: '100%', marginTop: 8 }}
                loading={loadingModels}
                options={modelOptions}
                value={modelId || undefined}
                onChange={setModelId}
                placeholder="Select model"
              />
            </div>

            <div>
              <Text className="label">Conversation</Text>
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                <Select
                  style={{ flex: 1 }}
                  showSearch
                  value={conversationId || undefined}
                  onChange={(id) => switchConversation(id)}
                  placeholder="Select conversation"
                  options={[
                    ...conversations
                      .filter((c) => c !== conversationId)
                      .map((c) => ({ value: c, label: c })),
                    ...(conversationId && !conversations.includes(conversationId)
                      ? [{ value: conversationId, label: conversationId }]
                      : []),
                    ...( conversations.includes(conversationId)
                      ? [{ value: conversationId, label: conversationId }]
                      : []),
                  ].filter((v, i, a) => a.findIndex((x) => x.value === v.value) === i)}
                />
                <Button size="small" onClick={() => { newConversation(); loadConversations(); }} disabled={sending}>+</Button>
              </div>
            </div>

            <div className="switch-row">
              <Text className="label">Stream Mode</Text>
              <Switch checked={streamMode} onChange={setStreamMode} />
            </div>

            <div>
              <Text className="label">Tools</Text>
              <Select
                mode="multiple"
                style={{ width: '100%', marginTop: 8 }}
                options={toolOptions}
                value={selectedTools}
                onChange={setSelectedTools}
                placeholder="Select tools"
              />
            </div>

            <div>
              <Text className="label">Skills</Text>
              <Select
                mode="multiple"
                style={{ width: '100%', marginTop: 8 }}
                options={skillOptions}
                value={selectedSkills}
                onChange={setSelectedSkills}
                placeholder="Select skills"
              />
            </div>

            <div className="quick-tags">
              <Tag color={streamMode ? 'green' : 'default'}>{streamMode ? 'Streaming' : 'One-shot'}</Tag>
              <Tag color="blue">{selectedTools.length} Tools</Tag>
              <Tag color="gold">{selectedSkills.length} Skills</Tag>
            </div>

            <div className="actions-row">
              <Button onClick={async () => { await clearConversation(conversationId).catch(() => {}); clearMessages(); }} disabled={sending}>Clear</Button>
              <Button onClick={() => window.location.reload()} disabled={sending}>Reload</Button>
            </div>
          </Space>
        </Card>
      </aside>

      <main className="console-main">
        <header className="main-header">
          <div>
            <Title level={3} style={{ margin: 0 }}>Runtime Console</Title>
            <Text type="secondary">Control model routing, tool/skill invocation and stream lifecycle.</Text>
          </div>
          <div className="status-strip">
            <span className={`pill ${sending ? 'hot' : ''}`}>{sending ? 'Running' : 'Idle'}</span>
            <span className={`pill ${streamState === 'streaming' ? 'ok' : ''}`}>Stream: {streamState}</span>
          </div>
        </header>

        <section className="stats-grid">
          <Card className="panel stat-card" variant="borderless">
            <div className="stat-label">Messages</div>
            <div className="stat-value">{messages.length}</div>
          </Card>
          <Card className="panel stat-card" variant="borderless">
            <div className="stat-label">User Turns</div>
            <div className="stat-value">{userCount}</div>
          </Card>
          <Card className="panel stat-card" variant="borderless">
            <div className="stat-label">Assistant Turns</div>
            <div className="stat-value">{assistantCount}</div>
          </Card>
          <Card className="panel stat-card" variant="borderless">
            <div className="stat-label">Last Route</div>
            <div className="stat-inline">{lastRequestInfo || 'No request yet'}</div>
          </Card>
        </section>

        <section className="console-grid">
          <Card className="panel chat-panel" variant="borderless">
            <div className="chat-header">
              <div className="tab-row">
                <button
                  type="button"
                  className={`tab-btn ${activeTab === 'runtime' ? 'active' : ''}`}
                  onClick={() => setActiveTab('runtime')}
                >
                  Runtime
                </button>
                <button
                  type="button"
                  className={`tab-btn ${activeTab === 'skills' ? 'active' : ''}`}
                  onClick={() => setActiveTab('skills')}
                >
                  Skills Config
                </button>
              </div>
              <div className="run-actions">
                <Button size="small" disabled={!sending || !streamMode} onClick={stopStreaming}>Stop stream</Button>
              </div>
            </div>

            {activeTab === 'runtime' ? (
              <>
                <div className="messages">
                  {loadingHistory && (
                    <div className="empty-state">
                      <div className="empty-text">Loading conversation history...</div>
                    </div>
                  )}
                  {!loadingHistory && messages.length === 0 && (
                    <div className="empty-state">
                      <div className="empty-icon">&#128172;</div>
                      <div className="empty-text">Start a conversation</div>
                      <div className="empty-hint">Select a model and type your message below</div>
                    </div>
                  )}
                  {messages.map((m, idx) => (
                    <MessageBubble
                      key={`${m.role}-${idx}`}
                      msg={m}
                      isLatest={idx === messages.length - 1}
                      sending={sending}
                    />
                  ))}
                  <div ref={messagesEndRef} />
                </div>

                <div className="prompt-shortcuts">
                  <button type="button" onClick={() => fillPrompt('Summarize current conversation in 3 bullets.')}>Quick Summary</button>
                  <button type="button" onClick={() => fillPrompt('Use available tools to answer this request accurately.')}>Use Tools</button>
                  <button type="button" onClick={() => fillPrompt('Explain your reasoning steps briefly and clearly.')}>Reasoning Brief</button>
                </div>

                <div className="composer">
                  <TextArea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    autoSize={{ minRows: 3, maxRows: 7 }}
                    placeholder="Type command or ask question..."
                    onPressEnter={(e) => {
                      if (!e.shiftKey) {
                        e.preventDefault();
                        onSend();
                      }
                    }}
                  />
                  <div className="send-row">
                    <Text type="secondary">Enter send / Shift+Enter newline</Text>
                    <Button type="primary" onClick={onSend} loading={sending} disabled={!modelId}>
                      Dispatch
                    </Button>
                  </div>
                </div>
              </>
            ) : (
              <div className="skills-panel">
                <div className="insight-line">Selected: <strong>{selectedSkills.join(', ') || 'None'}</strong></div>
                <Input
                  placeholder="skill name, e.g. team/custom/research"
                  value={skillNameInput}
                  onChange={(e) => setSkillNameInput(e.target.value)}
                />
                <Input
                  style={{ marginTop: 8 }}
                  placeholder="version, e.g. 1.0.0"
                  value={skillVersionInput}
                  onChange={(e) => setSkillVersionInput(e.target.value)}
                />
                <TextArea
                  style={{ marginTop: 8 }}
                  autoSize={{ minRows: 4, maxRows: 8 }}
                  placeholder="skill content"
                  value={skillContentInput}
                  onChange={(e) => setSkillContentInput(e.target.value)}
                />
                <div className="actions-row" style={{ marginTop: 8 }}>
                  <Button size="small" onClick={onUpsertSkill} loading={skillAdminBusy}>Save Skill</Button>
                  <Button size="small" danger onClick={onDeleteSelectedSkill} loading={skillAdminBusy}>Delete</Button>
                </div>
                <TextArea
                  style={{ marginTop: 10 }}
                  autoSize={{ minRows: 4, maxRows: 8 }}
                  placeholder={"skills.sh import:\nadd_skill \"team/custom/research\" \"1.0.0\" <<'EOF'\nYou are a research assistant...\nEOF"}
                  value={skillScriptInput}
                  onChange={(e) => setSkillScriptInput(e.target.value)}
                />
                <Button style={{ marginTop: 8 }} size="small" onClick={onImportSkillsSh} loading={skillAdminBusy}>
                  Import skills.sh
                </Button>
                <Input
                  style={{ marginTop: 10 }}
                  placeholder="owner/repo, owner/repo@skill, https://skills.sh/owner/repo"
                  value={skillSourceInput}
                  onChange={(e) => setSkillSourceInput(e.target.value)}
                />
                <Button style={{ marginTop: 8 }} size="small" onClick={onImportSkillSource} loading={skillAdminBusy}>
                  Import URL/slug
                </Button>
                {skillAdminMessage ? <Alert style={{ marginTop: 10 }} type="info" showIcon message={skillAdminMessage} /> : null}
              </div>
            )}
          </Card>

          <Card className="panel insight-panel" variant="borderless">
            <div className="insight-title">Execution Snapshot</div>
            <div className="insight-line">Model: <strong>{modelId || 'N/A'}</strong></div>
            <div className="insight-line">Conversation: <strong>{conversationId}</strong></div>
            <div className="insight-line">Tools: <strong>{selectedTools.join(', ') || 'None'}</strong></div>
            <div className="insight-line">Skills: <strong>{selectedSkills.join(', ') || 'None'}</strong></div>
            <div className="insight-line">Mode: <strong>{streamMode ? 'SSE stream' : 'Single response'}</strong></div>
            <div className="payload-box">
              <pre>{JSON.stringify({
                conversationId,
                model: modelId,
                streamMode,
                tools: selectedTools,
                skills: selectedSkills,
              }, null, 2)}</pre>
            </div>
            {error ? <Alert type="error" showIcon message={error} style={{ marginTop: 12 }} /> : null}
          </Card>
        </section>
      </main>
    </div>
  );
}

/* ─── SSE streaming with tool_call support ─── */

function sendStream({
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

export default App;
