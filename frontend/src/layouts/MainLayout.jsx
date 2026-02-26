import { useState, useEffect } from 'react';
import { Layout, Menu, Avatar, Breadcrumb, Button, Badge, Space, Typography, theme, Dropdown } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  MessageOutlined,
  RobotOutlined,
  ToolOutlined,
  BookOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  ApiOutlined,
  SettingOutlined,
  TeamOutlined,
  SafetyOutlined,
  LogoutOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '../core/state/authStore';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const getMenuItems = (isAdmin) => {
  const items = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: '概览' },
    { key: '/chat', icon: <MessageOutlined />, label: 'AI 对话' },
    { type: 'divider' },
    {
      key: 'resources',
      icon: <ApiOutlined />,
      label: '资源管理',
      children: [
        { key: '/models', icon: <RobotOutlined />, label: '模型管理' },
        { key: '/tools', icon: <ToolOutlined />, label: '工具管理' },
        { key: '/skills', icon: <BookOutlined />, label: '技能管理' },
      ],
    },
  ];

  // Admin only menu items
  if (isAdmin) {
    items.push(
      { type: 'divider' },
      {
        key: 'admin',
        icon: <SafetyOutlined />,
        label: '系统管理',
        children: [
          { key: '/users', icon: <TeamOutlined />, label: '用户管理' },
          { key: '/roles', icon: <SafetyOutlined />, label: '角色管理' },
        ],
      }
    );
  }

  items.push(
    { type: 'divider' },
    { key: '/settings', icon: <SettingOutlined />, label: '系统配置' }
  );

  return items;
};

const BREADCRUMBS = {
  '/dashboard': [{ title: 'AI Template' }, { title: '概览' }],
  '/chat': [{ title: 'AI Template' }, { title: 'AI 对话' }],
  '/models': [{ title: 'AI Template' }, { title: '资源管理' }, { title: '模型管理' }],
  '/tools': [{ title: 'AI Template' }, { title: '资源管理' }, { title: '工具管理' }],
  '/skills': [{ title: 'AI Template' }, { title: '资源管理' }, { title: '技能管理' }],
  '/settings': [{ title: 'AI Template' }, { title: '系统配置' }],
  '/users': [{ title: 'AI Template' }, { title: '系统管理' }, { title: '用户管理' }],
  '/roles': [{ title: 'AI Template' }, { title: '系统管理' }, { title: '角色管理' }],
};

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token: { colorBgContainer } } = theme.useToken();
  const { user, logout, isAdmin } = useAuthStore();

  // Refresh user info on mount
  useEffect(() => {
    useAuthStore.getState().refreshUser();
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: user?.name || user?.username,
      disabled: true,
    },
    {
      type: 'divider',
    },
    {
      key: 'password',
      icon: <LockOutlined />,
      label: '修改密码',
      onClick: () => navigate('/settings'),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
      danger: true,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={220}
        style={{ boxShadow: '2px 0 8px rgba(0,0,0,0.15)', zIndex: 10 }}
      >
        {/* Logo */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            padding: collapsed ? '0 24px' : '0 20px',
            gap: 12,
            cursor: 'pointer',
            borderBottom: '1px solid rgba(255,255,255,0.08)',
            flexShrink: 0,
          }}
          onClick={() => navigate('/dashboard')}
        >
          <div style={{
            width: 32, height: 32, borderRadius: 8,
            background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0, fontSize: 14, fontWeight: 700, color: '#fff',
          }}>AI</div>
          {!collapsed && (
            <div style={{ overflow: 'hidden' }}>
              <div style={{ color: '#fff', fontSize: 14, fontWeight: 600, lineHeight: '1.3', whiteSpace: 'nowrap' }}>AI Template</div>
              <div style={{ color: 'rgba(255,255,255,0.45)', fontSize: 11, whiteSpace: 'nowrap' }}>Spring AI Console</div>
            </div>
          )}
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          defaultOpenKeys={['resources', 'admin']}
          items={getMenuItems(isAdmin())}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0, marginTop: 4 }}
        />
      </Sider>

      <Layout>
        <Header style={{
          padding: '0 24px',
          background: colorBgContainer,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #f0f0f0',
          boxShadow: '0 1px 4px rgba(0,21,41,0.08)',
          position: 'sticky',
          top: 0,
          zIndex: 9,
        }}>
          <Space align="center" size={16}>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: 16, width: 40, height: 40 }}
            />
            <Breadcrumb items={BREADCRUMBS[location.pathname] || [{ title: 'AI Template' }]} />
          </Space>
          <Space align="center" size={16}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {user?.roles?.map(r => r.roleName).join(', ')}
            </Text>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Badge dot color="green" offset={[-2, 2]}>
                <Avatar 
                  size="small" 
                  icon={<UserOutlined />} 
                  style={{ background: '#1677ff', cursor: 'pointer' }} 
                />
              </Badge>
            </Dropdown>
          </Space>
        </Header>

        <Content style={{ padding: 24, overflow: 'auto', minHeight: 'calc(100vh - 64px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
