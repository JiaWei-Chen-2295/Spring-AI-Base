import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Divider,
  Input,
  List,
  Row,
  Space,
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
} from '@ant-design/icons';
import {
  fetchAdminSkills,
  upsertSkill,
  deleteSkill,
  importSkillsSh,
  importSkillsSource,
} from '../core/api/chat';

const { TextArea } = Input;
const { Title, Text } = Typography;

export default function Skills() {
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  // Form state
  const [skillName, setSkillName] = useState('');
  const [skillVersion, setSkillVersion] = useState('1.0.0');
  const [skillContent, setSkillContent] = useState('');

  // Import state
  const [scriptInput, setScriptInput] = useState('');
  const [sourceInput, setSourceInput] = useState('');

  const [msgApi, contextHolder] = message.useMessage();

  const reload = async () => {
    try {
      const data = await fetchAdminSkills();
      setSkills(data);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    reload().finally(() => setLoading(false));
  }, []);

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
      await upsertSkill({ skillName: skillName.trim(), version: (skillVersion || '1.0.0').trim(), content: skillContent });
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

  const onImportSh = async () => {
    if (!scriptInput.trim()) { msgApi.warning('请粘贴 skills.sh 内容'); return; }
    try {
      setBusy(true);
      const result = await importSkillsSh(scriptInput);
      await reload();
      msgApi.success(`导入成功: ${result.imported} 个技能`);
      setScriptInput('');
    } catch (e) {
      msgApi.error(`导入失败: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const onImportSource = async () => {
    if (!sourceInput.trim()) { msgApi.warning('请输入 owner/repo 或 URL'); return; }
    try {
      setBusy(true);
      const result = await importSkillsSource(sourceInput.trim());
      await reload();
      msgApi.success(`导入成功: ${result.imported} 个技能`);
      setSourceInput('');
    } catch (e) {
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
        <Text type="secondary">创建、编辑、导入和管理 AI 技能提示词</Text>
      </div>

      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} closable onClose={() => setError('')} />}

      <Row gutter={[16, 16]}>
        {/* Left: Skill List */}
        <Col xs={24} md={8}>
          <Card
            title={<Space><BookOutlined />技能列表 <Tag>{skills.length}</Tag></Space>}
            size="small"
            extra={
              <Button size="small" icon={<PlusOutlined />} onClick={onNew} type="primary">新建</Button>
            }
            loading={loading}
          >
            <List
              dataSource={skills}
              size="small"
              renderItem={(skill) => (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    background: selected?.skillName === skill.skillName && selected?.version === skill.version ? '#e6f4ff' : 'transparent',
                    borderRadius: 4,
                    padding: '8px 12px',
                    marginBottom: 4,
                  }}
                  onClick={() => onSelect(skill)}
                  actions={[
                    skill.editable && (
                      <Popconfirm
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
                        {!skill.editable && <Tag color="default" style={{ fontSize: 10, padding: '0 4px' }}>内置</Tag>}
                      </Space>
                    }
                    description={<Text type="secondary" style={{ fontSize: 11 }}>v{skill.version}</Text>}
                  />
                </List.Item>
              )}
              locale={{ emptyText: '暂无技能，点击新建' }}
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
                    placeholder="输入技能提示词内容..."
                    style={{ marginTop: 4, fontFamily: 'monospace', fontSize: 13 }}
                  />
                </div>
              </Space>
            </Card>

            {/* Import: skills.sh */}
            <Card
              title={<Space><ImportOutlined />从 skills.sh 导入</Space>}
              size="small"
              extra={<Button size="small" loading={busy} onClick={onImportSh} icon={<ImportOutlined />}>导入</Button>}
            >
              <TextArea
                value={scriptInput}
                onChange={(e) => setScriptInput(e.target.value)}
                autoSize={{ minRows: 3, maxRows: 6 }}
                placeholder={`粘贴 skills.sh 内容:\nadd_skill "team/custom/research" "1.0.0" <<'EOF'\nYou are a research assistant...\nEOF`}
                style={{ fontFamily: 'monospace', fontSize: 12 }}
              />
            </Card>

            {/* Import: URL/slug */}
            <Card
              title={<Space><LinkOutlined />从 URL / Slug 导入</Space>}
              size="small"
            >
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  value={sourceInput}
                  onChange={(e) => setSourceInput(e.target.value)}
                  placeholder="owner/repo, owner/repo@skill, 或 https://skills.sh/..."
                />
                <Button loading={busy} onClick={onImportSource} icon={<ImportOutlined />}>导入</Button>
              </Space.Compact>
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  );
}
