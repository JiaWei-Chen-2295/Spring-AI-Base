import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { formatTime } from '../utils/format';
import ToolCallCard from './ToolCallCard';

export default function MessageBubble({ msg, isLatest, sending }) {
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
