# 模型详情功能说明

## 功能概述
新增了模型详情查看功能，用户可以通过以下方式查看模型的详细信息：

### 使用方式
1. **点击模型卡片**：在模型管理页面，直接点击任意模型卡片即可打开详情对话框
2. **使用下拉菜单**：点击模型卡片右上角的三点菜单，选择"详情"选项

### 详情页面内容
详情对话框包含两个标签页：

#### 基本信息标签页
- **模型基本信息**：名称、组织、发布状态、描述
- **模型能力**：显示模型支持的能力标签（文本处理、视觉理解、推理分析等）
- **技术规格**：上下文窗口大小
- **时间信息**：创建时间和更新时间

#### 供应商模型标签页
- **关联模型列表**：显示该模型关联的所有VendorModel
- **模型详情**：每个VendorModel的名称、供应商、启用状态、描述
- **价格信息**：输入和输出token的价格（如果配置）
- **空状态提示**：当没有关联的供应商模型时显示友好提示

### 技术实现
- **组件**：`ModelDetailDialog` - 主要的详情对话框组件
- **API接口**：
  - `getModelById(id)` - 获取模型详情
  - `getVendorModelsByModelId(modelId)` - 获取关联的供应商模型
  - `getAllProviders()` - 获取供应商列表
- **状态管理**：使用React useState管理对话框状态和选中的模型ID
- **用户体验**：
  - 加载状态显示骨架屏
  - 错误状态显示友好错误信息
  - 防止下拉菜单触发卡片点击事件

### 文件修改
1. **新增文件**：
   - `/web/src/components/models/model-detail-dialog.tsx` - 详情对话框组件

2. **修改文件**：
   - `/web/src/components/models/model-list.tsx` - 添加点击事件处理
   - `/web/src/app/models/page.tsx` - 集成详情功能
   - `/web/src/lib/api.ts` - 添加api导出

### 使用示例
```typescript
// 在组件中使用
const [selectedModelId, setSelectedModelId] = useState<number | null>(null)

const handleViewDetails = (modelId: number) => {
  setSelectedModelId(modelId)
}

// 渲染详情对话框
<ModelDetailDialog
  open={!!selectedModelId}
  onOpenChange={(open) => !open && setSelectedModelId(null)}
  modelId={selectedModelId}
/>
```

这个功能提供了完整的模型详情查看体验，用户可以方便地了解模型的详细信息和关联的供应商模型。