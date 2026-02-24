import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic, List, Tag, Typography, Button, Space, Badge } from 'antd';
import {
  MessageOutlined,
  RobotOutlined,
  ToolOutlined,
  BookOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { fetchModels, fetchTools, fetchAdminSkills, fetchConversations } from '../core/api/chat';
import { useChatStore } from '../core/state/chatStore';

const { Title, Text } = Typography;

export default function Dashboard() {
  const navigate = useNavigate();
  const { models, conversations } = useChatStore();
  const [tools, setTools] = useState([]);
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([fetchTools(), fetchAdminSkills()])
      .then(([t, s]) => { setTools(t); setSkills(s); })
      .finally(() => setLoading(false));
  }, []);

  const stats = [
    {
      title: '会话数',
      value: conversations.length,
      icon: <MessageOutlined style={{ color: '#1677ff', fontSize: 24 }} />,
      color: '#e6f4ff',
      borderColor: '#91caff',
      action: () => navigate('/chat'),
    },
    {
      title: '可用模型',
      value: models.length,
      icon: <RobotOutlined style={{ color: '#52c41a', fontSize: 24 }} />,
      color: '#f6ffed',
      borderColor: '#b7eb8f',
      action: () => navigate('/models'),
    },
    {
      title: '工具数量',
      value: tools.length,
      icon: <ToolOutlined style={{ color: '#fa8c16', fontSize: 24 }} />,
      color: '#fff7e6',
      borderColor: '#ffd591',
      action: () => navigate('/tools'),
    },
    {
      title: '技能数量',
      value: skills.length,
      icon: <BookOutlined style={{ color: '#722ed1', fontSize: 24 }} />,
      color: '#f9f0ff',
      borderColor: '#d3adf7',
      action: () => navigate('/skills'),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>系统概览</Title>
        <Text type="secondary">Spring AI 后台管理控制台</Text>
      </div>

      {/* Stats Cards */}
      <Row gutter={[16, 16]}>
        {stats.map((s) => (
          <Col xs={24} sm={12} lg={6} key={s.title}>
            <Card
              style={{ borderColor: s.borderColor, background: s.color, cursor: 'pointer' }}
              styles={{ body: { padding: '20px 24px' } }}
              onClick={s.action}
              hoverable
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Statistic title={s.title} value={s.value} loading={loading} />
                <div style={{
                  width: 48, height: 48, borderRadius: 12,
                  background: '#fff', display: 'flex',
                  alignItems: 'center', justifyContent: 'center',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                }}>
                  {s.icon}
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {/* Recent Conversations */}
        <Col xs={24} lg={12}>
          <Card
            title={<Space><MessageOutlined />最近会话</Space>}
            extra={<Button type="link" size="small" onClick={() => navigate('/chat')} icon={<ArrowRightOutlined />}>去对话</Button>}
          >
            {conversations.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '20px 0', color: '#999' }}>
                暂无会话记录，<Button type="link" size="small" onClick={() => navigate('/chat')}>开始对话</Button>
              </div>
            ) : (
              <List
                dataSource={conversations.slice(0, 6)}
                renderItem={(convId) => (
                  <List.Item
                    style={{ cursor: 'pointer', padding: '8px 0' }}
                    onClick={() => navigate('/chat')}
                    extra={<ArrowRightOutlined style={{ color: '#999' }} />}
                  >
                    <List.Item.Meta
                      avatar={<Badge color="blue" />}
                      title={<Text ellipsis style={{ maxWidth: 200 }}>{convId}</Text>}
                      description={<Text type="secondary" style={{ fontSize: 12 }}>点击进入</Text>}
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>

        {/* System Status */}
        <Col xs={24} lg={12}>
          <Card title={<Space><CheckCircleOutlined style={{ color: '#52c41a' }} />系统状态</Space>}>
            <List
              dataSource={[
                { label: 'AI 对话服务', status: 'success', text: '正常运行' },
                { label: '模型加载', status: models.length > 0 ? 'success' : 'warning', text: models.length > 0 ? `${models.length} 个模型就绪` : '加载中...' },
                { label: '工具服务', status: tools.length > 0 ? 'success' : 'default', text: tools.length > 0 ? `${tools.length} 个工具可用` : '无工具' },
                { label: '技能服务', status: skills.length > 0 ? 'success' : 'default', text: skills.length > 0 ? `${skills.length} 个技能已加载` : '无技能' },
                { label: '流式传输 (SSE)', status: 'success', text: '已启用' },
              ]}
              renderItem={(item) => (
                <List.Item style={{ padding: '10px 0' }}>
                  <List.Item.Meta
                    title={<Text style={{ fontSize: 14 }}>{item.label}</Text>}
                  />
                  <Tag color={item.status === 'success' ? 'green' : item.status === 'warning' ? 'orange' : 'default'}>
                    {item.text}
                  </Tag>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      {/* Quick Actions */}
      <Card title="快速入口" style={{ marginTop: 16 }}>
        <Row gutter={[12, 12]}>
          {[
            { label: '开始 AI 对话', icon: <MessageOutlined />, path: '/chat', type: 'primary' },
            { label: '管理模型', icon: <RobotOutlined />, path: '/models', type: 'default' },
            { label: '配置工具', icon: <ToolOutlined />, path: '/tools', type: 'default' },
            { label: '管理技能', icon: <BookOutlined />, path: '/skills', type: 'default' },
          ].map((btn) => (
            <Col key={btn.path}>
              <Button type={btn.type} icon={btn.icon} onClick={() => navigate(btn.path)} size="large">
                {btn.label}
              </Button>
            </Col>
          ))}
        </Row>
      </Card>
    </div>
  );
}
