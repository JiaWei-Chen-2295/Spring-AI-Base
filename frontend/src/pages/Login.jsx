import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Form,
  Input,
  Button,
  Card,
  Typography,
  Alert,
  Space,
  Spin,
} from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useAuthStore } from '../core/state/authStore';

const { Title, Text } = Typography;

export default function Login() {
  const navigate = useNavigate();
  const { login, isLoading, error, clearError } = useAuthStore();
  const [form] = Form.useForm();

  const handleSubmit = async (values) => {
    clearError();
    const success = await login(values.username, values.password);
    if (success) {
      navigate('/dashboard');
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)',
        padding: 24,
      }}
    >
      <Card
        style={{
          width: 400,
          maxWidth: '100%',
          borderRadius: 12,
          boxShadow: '0 8px 32px rgba(0,0,0,0.15)',
        }}
        bodyStyle={{ padding: 40 }}
      >
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          {/* Logo */}
          <div style={{ textAlign: 'center' }}>
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: 16,
                background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 16,
              }}
            >
              <span style={{ color: '#fff', fontSize: 24, fontWeight: 700 }}>AI</span>
            </div>
            <Title level={3} style={{ margin: 0, color: '#1677ff' }}>
              AI Template
            </Title>
            <Text type="secondary">管理员登录</Text>
          </div>

          {/* Error Alert */}
          {error && (
            <Alert
              message={error}
              type="error"
              showIcon
              closable
              onClose={clearError}
            />
          )}

          {/* Login Form */}
          <Form
            form={form}
            name="login"
            onFinish={handleSubmit}
            autoComplete="off"
            size="large"
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="用户名"
                autoFocus
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="密码"
              />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
              <Button
                type="primary"
                htmlType="submit"
                block
                loading={isLoading}
                size="large"
              >
                {isLoading ? <Spin size="small" /> : '登录'}
              </Button>
            </Form.Item>
          </Form>

          {/* Demo Account */}
          <div style={{ textAlign: 'center' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              默认账号: admin / admin123
            </Text>
          </div>
        </Space>
      </Card>
    </div>
  );
}
