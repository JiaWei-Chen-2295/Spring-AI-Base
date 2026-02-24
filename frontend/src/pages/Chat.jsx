import { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Input,
  Row,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
  Divider,
  Tooltip,
} from 'antd';
import {
  SendOutlined,
  StopOutlined,
  PlusOutlined,
  ClearOutlined,
  ReloadOutlined,
  RobotOutlined,
  MessageOutlined,
  ToolOutlined,
  BookOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import MessageBubble from '../shared/MessageBubble';
import { sendStream } from '../utils/sendStream';
import {
  buildStreamUrl,
  chatOnce,
  clearConversation,
  fetchAdminSkills,
  fetchModels,
  fetchTools,
} from '../core/api/chat';
import { useChatStore } from '../core/state/chatStore';

const { TextArea } = Input;
const { Text, Title } = Typography;

export default function Chat() {
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
    selectedTools,
    selectedSkills,
    setModels,
    setModelId,
    setError,
    setSending,
    setLoadingModels,
    setStreamMode,
    setSelectedTools,
    setSelectedSkills,
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
  const [streamState, setStreamState] = useState('idle');
  const [lastRoute, setLastRoute] = useState('');

  const eventSourceRef = useRef(null);
  const messagesEndRef = useRef(null);

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
        if (mounted) setError(`加载失败: ${e.message}`);
      } finally {
        if (mounted) setLoadingModels(false);
      }
    };
    load();
    loadConversations();
    return () => {
      mounted = false;
      if (eventSourceRef.current) eventSourceRef.current.close();
    };
  }, []);

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

  const onSend = async () => {
    const message = input.trim();
    if (!message || !modelId || sending) return;
    setError('');
    setSending(true);
    setStreamState(streamMode ? 'connecting' : 'idle');
    addUserMessage(message);
    addAssistantPlaceholder();
    setInput('');
    setLastRoute(`${modelId} | tools:${selectedTools.length} | skills:${selectedSkills.length}`);

    try {
      if (streamMode) {
        await sendStream({
          conversationId, modelId, message,
          tools: selectedTools, skills: selectedSkills,
          appendAssistantText, addToolCall, setError, setStreamState, eventSourceRef,
        });
      } else {
        const resp = await chatOnce({ conversationId, modelId, message, tools: selectedTools, skills: selectedSkills });
        setAssistantText(resp.content || '');
        if (resp.toolCalls && resp.toolCalls.length) setToolCalls(resp.toolCalls);
      }
    } catch (e) {
      setAssistantText('');
      setError(`发送失败: ${e.message}`);
    } finally {
      setSending(false);
      if (!streamMode) setStreamState('idle');
      loadConversations();
    }
  };

  const onClear = async () => {
    await clearConversation(conversationId).catch(() => {});
    clearMessages();
  };

  const conversationOptions = useMemo(() => {
    const all = [...new Set([...conversations, conversationId])];
    return all.map((c) => ({ value: c, label: c }));
  }, [conversations, conversationId]);

  return (
    <Row gutter={[12, 12]} style={{ height: 'calc(100vh - 112px)' }} wrap={false}>
      {/* ── Left: Config Panel ─────────────────────────────────── */}
      <Col flex="260px" style={{ minWidth: 260, maxWidth: 280 }}>
        <Card
          size="small"
          style={{ height: '100%', overflow: 'auto' }}
          styles={{ body: { padding: 16 } }}
        >
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            {/* Model */}
            <div>
              <Text strong style={{ fontSize: 12 }}><RobotOutlined /> 模型</Text>
              <Select
                style={{ width: '100%', marginTop: 6 }}
                loading={loadingModels}
                options={modelOptions}
                value={modelId || undefined}
                onChange={setModelId}
                placeholder="选择模型"
                size="small"
              />
            </div>

            {/* Conversation */}
            <div>
              <Text strong style={{ fontSize: 12 }}><MessageOutlined /> 会话</Text>
              <div style={{ display: 'flex', gap: 6, marginTop: 6 }}>
                <Select
                  style={{ flex: 1 }}
                  size="small"
                  showSearch
                  value={conversationId || undefined}
                  onChange={(id) => switchConversation(id)}
                  placeholder="选择会话"
                  options={conversationOptions}
                />
                <Tooltip title="新建会话">
                  <Button
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() => { newConversation(); loadConversations(); }}
                    disabled={sending}
                  />
                </Tooltip>
              </div>
            </div>

            {/* Stream Mode */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Text strong style={{ fontSize: 12 }}><ThunderboltOutlined /> 流式模式</Text>
              <Switch size="small" checked={streamMode} onChange={setStreamMode} />
            </div>

            <Divider style={{ margin: '4px 0' }} />

            {/* Tools */}
            <div>
              <Text strong style={{ fontSize: 12 }}><ToolOutlined /> 工具</Text>
              <Select
                mode="multiple"
                size="small"
                style={{ width: '100%', marginTop: 6 }}
                options={toolOptions}
                value={selectedTools}
                onChange={setSelectedTools}
                placeholder="选择工具"
                maxTagCount="responsive"
              />
            </div>

            {/* Skills */}
            <div>
              <Text strong style={{ fontSize: 12 }}><BookOutlined /> 技能</Text>
              <Select
                mode="multiple"
                size="small"
                style={{ width: '100%', marginTop: 6 }}
                options={skillOptions}
                value={selectedSkills}
                onChange={setSelectedSkills}
                placeholder="选择技能"
                maxTagCount="responsive"
              />
            </div>

            <Divider style={{ margin: '4px 0' }} />

            {/* Status Tags */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
              <Tag color={streamMode ? 'green' : 'default'} style={{ margin: 0 }}>
                {streamMode ? '流式' : '单次'}
              </Tag>
              <Tag color="blue" style={{ margin: 0 }}>{selectedTools.length} 工具</Tag>
              <Tag color="gold" style={{ margin: 0 }}>{selectedSkills.length} 技能</Tag>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', gap: 6 }}>
              <Button size="small" icon={<ClearOutlined />} onClick={onClear} disabled={sending} style={{ flex: 1 }}>清空</Button>
              <Button size="small" icon={<ReloadOutlined />} onClick={() => window.location.reload()} disabled={sending} style={{ flex: 1 }}>刷新</Button>
            </div>
          </Space>
        </Card>
      </Col>

      {/* ── Center: Chat Area ───────────────────────────────────── */}
      <Col flex="1" style={{ minWidth: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
        {/* Stats Row */}
        <Row gutter={8}>
          {[
            { label: '消息总数', value: messages.length, color: '#1677ff' },
            { label: '用户消息', value: userCount, color: '#52c41a' },
            { label: '助手回复', value: assistantCount, color: '#722ed1' },
          ].map((s) => (
            <Col span={8} key={s.label}>
              <Card size="small" styles={{ body: { padding: '8px 12px' } }}>
                <div style={{ fontSize: 11, color: '#999' }}>{s.label}</div>
                <div style={{ fontSize: 20, fontWeight: 700, color: s.color }}>{s.value}</div>
              </Card>
            </Col>
          ))}
        </Row>

        {/* Messages */}
        <Card
          size="small"
          style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
          styles={{ body: { padding: 0, flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' } }}
          title={
            <Space>
              <span style={{ fontSize: 13 }}>对话记录</span>
              <Tag color={sending ? 'processing' : 'default'} style={{ margin: 0, fontSize: 11 }}>
                {sending ? `● ${streamState}` : '空闲'}
              </Tag>
            </Space>
          }
          extra={
            <Button
              size="small"
              icon={<StopOutlined />}
              disabled={!sending || !streamMode}
              onClick={stopStreaming}
              danger
            >
              停止
            </Button>
          }
        >
          <div className="messages" style={{ flex: 1, overflowY: 'auto', padding: '12px 16px' }}>
            {loadingHistory && (
              <div className="empty-state"><div className="empty-text">加载会话历史...</div></div>
            )}
            {!loadingHistory && messages.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">💬</div>
                <div className="empty-text">开始对话</div>
                <div className="empty-hint">选择模型并在下方输入消息</div>
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
        </Card>

        {/* Input Area */}
        <Card size="small" styles={{ body: { padding: '12px 16px' } }}>
          {/* Quick prompts */}
          <div style={{ display: 'flex', gap: 6, marginBottom: 10, flexWrap: 'wrap' }}>
            {[
              { label: '快速摘要', prompt: 'Summarize current conversation in 3 bullets.' },
              { label: '使用工具', prompt: 'Use available tools to answer this request accurately.' },
              { label: '推理说明', prompt: 'Explain your reasoning steps briefly and clearly.' },
            ].map((s) => (
              <Button
                key={s.label}
                size="small"
                type="dashed"
                onClick={() => setInput(s.prompt)}
              >{s.label}</Button>
            ))}
          </div>

          {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 10 }} closable onClose={() => setError('')} />}

          <TextArea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            autoSize={{ minRows: 3, maxRows: 6 }}
            placeholder="输入消息... (Enter 发送 / Shift+Enter 换行)"
            onPressEnter={(e) => {
              if (!e.shiftKey) { e.preventDefault(); onSend(); }
            }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {lastRoute ? `上次: ${lastRoute}` : 'Enter 发送 / Shift+Enter 换行'}
            </Text>
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={onSend}
              loading={sending}
              disabled={!modelId || !input.trim()}
            >
              发送
            </Button>
          </div>
        </Card>
      </Col>

      {/* ── Right: Execution Snapshot ───────────────────────────── */}
      <Col flex="220px" style={{ minWidth: 220, maxWidth: 240 }}>
        <Card
          size="small"
          title="执行快照"
          style={{ height: '100%', overflow: 'auto' }}
          styles={{ body: { padding: 12 } }}
        >
          <Space direction="vertical" style={{ width: '100%' }} size={8}>
            {[
              { label: '模型', value: modelId || 'N/A' },
              { label: '会话 ID', value: conversationId },
              { label: '模式', value: streamMode ? 'SSE 流式' : '单次请求' },
              { label: '工具', value: selectedTools.join(', ') || '无' },
              { label: '技能', value: selectedSkills.join(', ') || '无' },
            ].map((item) => (
              <div key={item.label}>
                <Text type="secondary" style={{ fontSize: 11 }}>{item.label}</Text>
                <div>
                  <Text strong style={{ fontSize: 12, wordBreak: 'break-all' }}>{item.value}</Text>
                </div>
              </div>
            ))}

            <Divider style={{ margin: '4px 0' }} />

            <div>
              <Text type="secondary" style={{ fontSize: 11 }}>请求载荷</Text>
              <pre style={{
                background: '#f5f5f5', borderRadius: 4, padding: 8,
                fontSize: 11, marginTop: 4, overflow: 'auto',
                maxHeight: 200, wordBreak: 'break-all', whiteSpace: 'pre-wrap',
              }}>
                {JSON.stringify({ conversationId, model: modelId, streamMode, tools: selectedTools, skills: selectedSkills }, null, 2)}
              </pre>
            </div>
          </Space>
        </Card>
      </Col>
    </Row>
  );
}
