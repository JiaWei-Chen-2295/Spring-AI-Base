import { Collapse, Tag } from 'antd';

export default function ToolCallCard({ toolCalls }) {
  if (!toolCalls || !toolCalls.length) return null;

  const items = toolCalls.map((tc, i) => ({
    key: String(i),
    label: (
      <div className="tc-header">
        <span className="tc-icon">⚙</span>
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
