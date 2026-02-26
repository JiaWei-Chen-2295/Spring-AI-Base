import { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  DatePicker,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  LockOutlined,
  UnlockOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { userApi } from '../core/api/user';
import { roleApi } from '../core/api/user';
import dayjs from 'dayjs';

const { Option } = Select;

export default function Users() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [roleModalVisible, setRoleModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedRoles, setSelectedRoles] = useState([]);
  const [form] = Form.useForm();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, [pagination.current, pagination.pageSize, searchKeyword]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await userApi.listUsers(
        pagination.current,
        pagination.pageSize,
        searchKeyword
      );
      setUsers(response.data.records || []);
      setPagination({
        ...pagination,
        total: response.data.total || 0,
      });
    } catch (error) {
      message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const response = await roleApi.listRoles();
      setRoles(response.data || []);
    } catch (error) {
      message.error('获取角色列表失败');
    }
  };

  const handleCreate = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (user) => {
    setEditingUser(user);
    form.setFieldsValue({
      ...user,
      birthday: user.birthday ? dayjs(user.birthday) : null,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    try {
      await userApi.deleteUser(id);
      message.success('删除成功');
      fetchUsers();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleToggleStatus = async (id) => {
    try {
      await userApi.toggleUserStatus(id);
      message.success('状态更新成功');
      fetchUsers();
    } catch (error) {
      message.error('状态更新失败');
    }
  };

  const handleAssignRoles = (user) => {
    setSelectedUser(user);
    setSelectedRoles(user.roles?.map((r) => r.id) || []);
    setRoleModalVisible(true);
  };

  const handleSaveRoles = async () => {
    try {
      await userApi.assignRoles(selectedUser.id, selectedRoles);
      message.success('角色分配成功');
      setRoleModalVisible(false);
      fetchUsers();
    } catch (error) {
      message.error('角色分配失败');
    }
  };

  const handleSubmit = async (values) => {
    try {
      const data = {
        ...values,
        birthday: values.birthday?.format('YYYY-MM-DD'),
      };

      if (editingUser) {
        await userApi.updateUser(editingUser.id, data);
        message.success('更新成功');
      } else {
        await userApi.createUser(data);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchUsers();
    } catch (error) {
      message.error(editingUser ? '更新失败' : '创建失败');
    }
  };

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '性别',
      dataIndex: 'gender',
      key: 'gender',
      render: (gender) =>
        gender === 1 ? '男' : gender === 2 ? '女' : '-',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles) => (
        <Space size={4}>
          {roles?.map((role) => (
            <Tag key={role.id} color="blue">
              {role.roleName}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 1 ? 'success' : 'default'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (time) => dayjs(time).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="text"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Button
            type="text"
            icon={record.status === 1 ? <LockOutlined /> : <UnlockOutlined />}
            onClick={() => handleToggleStatus(record.id)}
          />
          <Button
            type="text"
            icon={<UserOutlined />}
            onClick={() => handleAssignRoles(record)}
          />
          <Popconfirm
            title="确认删除"
            description="确定要删除该用户吗？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Space>
            <Input.Search
              placeholder="搜索用户名/姓名/手机号"
              allowClear
              onSearch={(value) => setSearchKeyword(value)}
              style={{ width: 250 }}
            />
          </Space>
        </Col>
        <Col>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建用户
          </Button>
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={(p) =>
          setPagination({
            current: p.current,
            pageSize: p.pageSize,
            total: pagination.total,
          })
        }
      />

      {/* User Form Modal */}
      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        open={modalVisible}
        onOk={() => form.submit()}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: 1, gender: 1 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="username"
                label="用户名"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input disabled={!!editingUser} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="password"
                label={editingUser ? '密码 (留空不修改)' : '密码'}
                rules={
                  editingUser
                    ? []
                    : [{ required: true, message: '请输入密码' }]
                }
              >
                <Input.Password />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="姓名"
                rules={[{ required: true, message: '请输入姓名' }]}
              >
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="gender" label="性别">
                <Select>
                  <Option value={1}>男</Option>
                  <Option value={2}>女</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="phone" label="手机号">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="email" label="邮箱">
                <Input />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="birthday" label="出生日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态">
                <Select>
                  <Option value={1}>启用</Option>
                  <Option value={0}>禁用</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="address" label="地址">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Role Assignment Modal */}
      <Modal
        title="分配角色"
        open={roleModalVisible}
        onOk={handleSaveRoles}
        onCancel={() => setRoleModalVisible(false)}
      >
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder="选择角色"
          value={selectedRoles}
          onChange={setSelectedRoles}
        >
          {roles.map((role) => (
            <Option key={role.id} value={role.id}>
              {role.roleName} ({role.roleCode})
            </Option>
          ))}
        </Select>
      </Modal>
    </Card>
  );
}
