import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Input,
  List,
  Row,
  Space,
  Steps,
  Tag,
  Typography,
  Popconfirm,
  message,
} from 'antd';
import {
  BookOutlined,
  PlusOutlined,
  DeleteOutlined,
  ImportOutlined,
  LinkOutlined,
  EditOutlined,
  SearchOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import {
  fetchAdminSkills,
  upsertSkill,
  deleteSkill,
  importSkillsSource,
} from '../core/api/chat';

const { TextArea } = Input;
const { Title, Text } = Typography;

const IMPORT_STEPS = [
  { title: '连接', description: '请求远程源' },
  { title: '解析', description: '分析技能文件' },
  { title: '保存', description: '写入技能库' },
];

export default function Skills() {
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [searchText, setSearchText] = useState('');

  // Form state
  const [skillName, setSkillName] = useState('');
  const [skillVersion, setSkillVersion] = useState('1.0.0');
  const [skillContent, setSkillContent] = useState('');

  // Import state
  const [sourceInput, setSourceInput] = useState('');
  const [importStep, setImportStep] = useState(-1); // -1 = idle
  const [importedNames, setImportedNames] = useState([]);

  const [msgApi, contextHolder] = message.useMessage();

  const reload = async () => {
    try {
      const data = await fetchAdminSkills();
      setSkills(data);
      return data;
    } catch (e) {
      setError(e.message);
      return null;
    }
  };

  useEffect(() => {
    reload().finally(() => setLoading(false));
  }, []);

  const filteredSkills = skills.filter(
    (s) => !searchText || s.skillName.toLowerCase().includes(searchText.toLowerCase())
  );

  const onSelect = (skill) => {
    setSelected(skill);
    setSkillName(skill.skillName);
    setSkillVersion(skill.version);
    setSkillContent(skill.content || '');
  };

  const onNew = () => {
    setSelected(null);
    setSkillName('');
    setSkillVersion('1.0.0');
    setSkillContent('');
  };

  const onSave = async () => {
    if (!skillName.trim() || !skillContent.trim()) {
      msgApi.warning('技能名称和内容不能为空');
      return;
    }
    try {
      setBusy(true);
      await upsertSkill({
        skillName: skillName.trim(),
        version: (skillVersion || '1.0.0').trim(),
        content: skillContent,
      });
      await reload();
      msgApi.success('技能保存成功');
    } catch (e) {
      msgApi.error(`保存失败: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const onDelete = async (skill) => {
    if (!skill.editable) { msgApi.warning('内置技能不可删除'); return; }
    try {
      setBusy(true);
      await deleteSkill(skill.skillName, skill.version);
      await reload();
      if (selected?.skillName === skill.skillName) onNew();
      msgApi.success('技能已删除');
    } catch (e) {
      msgApi.error(`删除失败: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const onImportSource = async () => {
    if (!sourceInput.trim()) { msgApi.warning('请输入 owner/repo 或 URL'); return; }
    try {
      setBusy(true);
      setImportedNames([]);
      setImportStep(0); // connecting
      const result = await importSkillsSource(sourceInput.trim());
      setImportStep(2); // saving
      await reload();
      setImportedNames(result.skillNames || []);
      setImportStep(-1);
      if (result.imported === 0) {
        msgApi.warning('未发现可导入的技能');
      }
      setSourceInput('');
    } catch (e) {
      setImportStep(-1);
      msgApi.error(`导入失败: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      {contextHolder}
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>技能管理</Title>
        <Text type="secondary">创建、编辑和导入 AI 技能提示词</Text>
      </div>

      {error && (
        <Alert
          type="error"
          message={error}
          showIcon
          style={{ marginBottom: 16 }}
          closable
          onClose={() => setError('')}
        />
      )}

      <Row gutter={[16, 16]}>
        {/* Left: Skill List */}
        <Col xs={24} md={8}>
          <Card
            title={<Space><BookOutlined />技能列表 <Tag>{skills.length}</Tag></Space>}
            size="small"
            loading={loading}
            extra={
              <Button size="small" icon={<PlusOutlined />} onClick={onNew} type="primary">
                新建
              </Button>
            }
          >
            <Input
              placeholder="搜索技能..."
              prefix={<SearchOutlined style={{ color: '#bbb' }} />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              size="small"
              style={{ marginBottom: 8 }}
              allowClear
            />
            <List
              dataSource={filteredSkills}
              size="small"
              renderItem={(skill) => (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    background:
                      selected?.skillName === skill.skillName && selected?.version === skill.version
                        ? '#e6f4ff'
                        : 'transparent',
                    borderRadius: 4,
                    padding: '8px 12px',
                    marginBottom: 4,
                  }}
                  onClick={() => onSelect(skill)}
                  actions={[
                    skill.editable && (
                      <Popconfirm
                        key="del"
                        title="确认删除此技能?"
                        onConfirm={(e) => { e.stopPropagation(); onDelete(skill); }}
                        okText="删除"
                        cancelText="取消"
                        okButtonProps={{ danger: true }}
                      >
                        <DeleteOutlined
                          style={{ color: '#ff4d4f' }}
                          onClick={(e) => e.stopPropagation()}
                        />
                      </Popconfirm>
                    ),
                  ].filter(Boolean)}
                >
                  <List.Item.Meta
                    avatar={<BookOutlined style={{ color: '#722ed1', marginTop: 3 }} />}
                    title={
                      <Space size={4}>
                        <Text style={{ fontSize: 13 }} ellipsis>{skill.skillName}</Text>
                        {!skill.editable && (
                          <Tag color="default" style={{ fontSize: 10, padding: '0 4px' }}>内置</Tag>
                        )}
                      </Space>
                    }
                    description={
                      <Text type="secondary" style={{ fontSize: 11 }}>v{skill.version}</Text>
                    }
                  />
                </List.Item>
              )}
              locale={{ emptyText: searchText ? '无匹配技能' : '暂无技能，点击新建' }}
            />
          </Card>
        </Col>

        {/* Right: Editor + Import */}
        <Col xs={24} md={16}>
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            {/* Skill Editor */}
            <Card
              title={
                <Space>
                  <EditOutlined />
                  {selected ? `编辑: ${selected.skillName}` : '新建技能'}
                </Space>
              }
              size="small"
              extra={
                <Space>
                  <Button size="small" onClick={onNew}>清空</Button>
                  <Button size="small" type="primary" loading={busy} onClick={onSave}>保存</Button>
                </Space>
              }
            >
              <Space direction="vertical" style={{ width: '100%' }} size={8}>
                <Row gutter={8}>
                  <Col flex="1">
                    <Text style={{ fontSize: 12 }}>技能名称</Text>
                    <Input
                      value={skillName}
                      onChange={(e) => setSkillName(e.target.value)}
                      placeholder="team/custom/skill-name"
                      style={{ marginTop: 4 }}
                      disabled={selected && !selected.editable}
                    />
                  </Col>
                  <Col flex="120px">
                    <Text style={{ fontSize: 12 }}>版本</Text>
                    <Input
                      value={skillVersion}
                      onChange={(e) => setSkillVersion(e.target.value)}
                      placeholder="1.0.0"
                      style={{ marginTop: 4 }}
                    />
                  </Col>
                </Row>
                <div>
                  <Text style={{ fontSize: 12 }}>技能内容 (系统提示词)</Text>
                  <TextArea
                    value={skillContent}
                    onChange={(e) => setSkillContent(e.target.value)}
                    autoSize={{ minRows: 6, maxRows: 12 }}
                    placeholder="输入技能提示词内容，将作为系统提示词注入对话..."
                    style={{ marginTop: 4, fontFamily: 'monospace', fontSize: 13 }}
                    readOnly={selected && !selected.editable}
                  />
                </div>
                {selected && !selected.editable && (
                  <Alert
                    type="info"
                    showIcon
                    message="内置技能为只读，无法修改"
                    style={{ padding: '4px 10px' }}
                  />
                )}
              </Space>
            </Card>

            {/* Import from GitHub / URL */}
            <Card
              title={<Space><LinkOutlined />从 GitHub / URL 导入</Space>}
              size="small"
            >
              <Space direction="vertical" style={{ width: '100%' }} size={10}>
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    value={sourceInput}
                    onChange={(e) => setSourceInput(e.target.value)}
                    onPressEnter={onImportSource}
                    placeholder="owner/repo, owner/repo@skill, 或 https://github.com/..."
                    disabled={busy}
                  />
                  <Button
                    loading={busy}
                    onClick={onImportSource}
                    type="primary"
                    icon={<ImportOutlined />}
                  >
                    导入
                  </Button>
                </Space.Compact>

                {/* Progress steps during import */}
                {busy && importStep >= 0 && (
                  <div style={{ padding: '6px 4px 2px' }}>
                    <Steps
                      size="small"
                      current={importStep}
                      status="process"
                      items={IMPORT_STEPS}
                    />
                  </div>
                )}

                {/* Imported skill names result */}
                {!busy && importedNames.length > 0 && (
                  <Alert
                    type="success"
                    showIcon
                    message={`成功导入 ${importedNames.length} 个技能`}
                    description={
                      <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                        {importedNames.map((name) => (
                          <Tag
                            key={name}
                            color="purple"
                            icon={<CheckCircleOutlined />}
                            style={{ fontSize: 12 }}
                          >
                            {name}
                          </Tag>
                        ))}
                      </div>
                    }
                    closable
                    onClose={() => setImportedNames([])}
                  />
                )}

                <Text type="secondary" style={{ fontSize: 12 }}>
                  支持格式：<code>owner/repo</code>、<code>owner/repo@skill</code>、
                  <code>https://github.com/owner/repo</code>
                </Text>
              </Space>
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  );
}
