import { Collapse, Tag } from 'antd';
import { CheckCircleFilled, CloseCircleFilled, CodeOutlined, LoadingOutlined } from '@ant-design/icons';

function getToolAccent(toolName) {
  if (!toolName) return '#13c2c2';
  const name = toolName.toLowerCase();
  if (name.includes('shell') || name.includes('exec') || name.includes('run')) return '#fa8c16';
  if (name.includes('web') || name.includes('search') || name.includes('http') || name.includes('fetch')) return '#1677ff';
  if (name.includes('read') || name.includes('skill') || name.includes('memory')) return '#722ed1';
  if (name.includes('write') || name.includes('save') || name.includes('create')) return '#52c41a';
  return '#13c2c2';
}

export default function ToolCallCard({ toolCalls }) {
  if (!toolCalls || !toolCalls.length) return null;
  const runningCount = toolCalls.filter((tc) => tc.status === 'running').length;
  const doneCount = toolCalls.filter((tc) => tc.status !== 'running').length;

  const items = toolCalls.map((tc, i) => {
    const accent = getToolAccent(tc.toolName);
    const status = tc.status || 'done';
    const statusIcon = status === 'running'
      ? <LoadingOutlined style={{ color: accent, fontSize: 13, flexShrink: 0 }} />
      : status === 'error'
        ? <CloseCircleFilled style={{ color: '#ff4d4f', fontSize: 13, flexShrink: 0 }} />
        : <CheckCircleFilled style={{ color: accent, fontSize: 13, flexShrink: 0 }} />;
    const durationLabel = typeof tc.durationMs === 'number' ? `${tc.durationMs}ms` : 'running';
    return {
      key: String(i),
      label: (
        <div className="tc-header">
          {statusIcon}
          <span className="tc-name">{tc.toolName}</span>
          <Tag color={status === 'running' ? 'processing' : status === 'error' ? 'error' : 'success'} style={{ marginLeft: 'auto', fontSize: 11, flexShrink: 0 }}>
            {durationLabel}
          </Tag>
        </div>
      ),
      style: {
        borderLeft: `3px solid ${accent}`,
        marginBottom: 4,
        borderRadius: '0 6px 6px 0',
        background: '#fff',
      },
      children: (
        <div className="tc-body">
          <div className="tc-section">
            <div className="tc-label">输入</div>
            <pre className="tc-pre">{tc.input || '(空)'}</pre>
          </div>
          <div className="tc-section" style={{ marginBottom: 0 }}>
            <div className="tc-label">输出</div>
            <pre className="tc-pre">{tc.output || '(空)'}</pre>
          </div>
        </div>
      ),
    };
  });

  return (
    <div className="tool-call-card">
      <div className="tc-title-row">
        <CodeOutlined style={{ marginRight: 6, color: '#555', fontSize: 12 }} />
        <span className="tc-badge">工具调用</span>
        {runningCount > 0 && (
          <Tag color="processing" style={{ marginLeft: 8, fontSize: 11 }} icon={<LoadingOutlined />}>
            {runningCount} 运行中
          </Tag>
        )}
        <Tag color="success" style={{ marginLeft: 8, fontSize: 11 }} icon={<CheckCircleFilled />}>
          {doneCount} 已完成
        </Tag>
      </div>
      <Collapse
        size="small"
        items={items}
        bordered={false}
        defaultActiveKey={toolCalls.length <= 2 ? toolCalls.map((_, i) => String(i)) : ['0']}
        style={{ background: 'transparent' }}
      />
    </div>
  );
}
