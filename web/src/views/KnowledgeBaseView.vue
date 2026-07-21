<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { createKnowledgeArticle, createKnowledgeCategory, archiveKnowledgeArticle, fetchKnowledgeCategories, fetchKnowledgePage, publishKnowledgeArticle, updateKnowledgeArticle } from '@/services/knowledge'
import type { KnowledgeArticle, KnowledgeVisibility, KnowledgeCategory } from '@/types/api'

const auth = useAuthStore()
const categories = ref<KnowledgeCategory[]>([])
const rows = ref<KnowledgeArticle[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const categoryId = ref<number | undefined>()
const loading = ref(false)
const articleVisible = ref(false)
const categoryVisible = ref(false)
const editing = ref<KnowledgeArticle | null>(null)
const articleForm = reactive({ categoryId: undefined as number | undefined, title: '', summary: '', content: '', visibility: 'TENANT' as KnowledgeVisibility, departmentId: undefined as number | undefined })
const categoryForm = reactive({ categoryCode: '', categoryName: '', description: '', sortOrder: 10 })
const canWrite = () => auth.hasAuthority('knowledge:write') || auth.hasAuthority('knowledge:manage')
const canManage = () => auth.hasAuthority('knowledge:manage')

async function loadCategories() { categories.value = await fetchKnowledgeCategories() }
async function loadArticles() { loading.value = true; try { const page = await fetchKnowledgePage({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined, categoryId: categoryId.value }); rows.value = page.records; total.value = page.total } finally { loading.value = false } }
function openCreate() { editing.value = null; Object.assign(articleForm, { categoryId: categories.value[0]?.id, title: '', summary: '', content: '', visibility: 'TENANT', departmentId: undefined }); articleVisible.value = true }
function openEdit(value: unknown) { const article = value as KnowledgeArticle; editing.value = article; Object.assign(articleForm, { categoryId: article.categoryId, title: article.title, summary: article.summary ?? '', content: article.content, visibility: article.visibility, departmentId: article.departmentId ?? undefined }); articleVisible.value = true }
async function saveArticle() { if (!articleForm.categoryId || !articleForm.title || !articleForm.content) return ElMessage.warning('请填写分类、标题和正文'); const payload = { categoryId: articleForm.categoryId, title: articleForm.title, summary: articleForm.summary || undefined, content: articleForm.content, visibility: articleForm.visibility, departmentId: articleForm.visibility === 'DEPARTMENT' ? articleForm.departmentId : undefined }; if (editing.value) await updateKnowledgeArticle({ ...payload, id: editing.value.id, version: editing.value.version }); else await createKnowledgeArticle(payload); articleVisible.value = false; ElMessage.success('知识文章已保存为草稿'); await loadArticles() }
async function publish(value: unknown) { const article = value as KnowledgeArticle; await publishKnowledgeArticle(article.id, article.version); ElMessage.success('文章已发布'); await loadArticles() }
async function archive(value: unknown) { const article = value as KnowledgeArticle; await archiveKnowledgeArticle(article.id, article.version); ElMessage.success('文章已归档'); await loadArticles() }
async function saveCategory() { if (!categoryForm.categoryCode || !categoryForm.categoryName) return ElMessage.warning('请填写分类编码和名称'); await createKnowledgeCategory(categoryForm); categoryVisible.value = false; Object.assign(categoryForm, { categoryCode: '', categoryName: '', description: '', sortOrder: 10 }); await loadCategories(); ElMessage.success('分类已创建') }
onMounted(async () => { await Promise.all([loadCategories(), loadArticles()]) })
</script>

<template>
  <main class="workspace-page">
    <header class="page-heading"><div><span class="section-kicker">KNOWLEDGE BASE</span><h1>知识库</h1><p>沉淀可复用方案；检索结果会按当前租户和部门范围过滤。</p></div><div><el-button v-if="canManage()" @click="categoryVisible=true">管理分类</el-button><el-button v-if="canWrite()" class="primary-action" type="primary" :icon="Plus" @click="openCreate">新建知识</el-button></div></header>
    <section class="glass-card module-card"><form class="ticket-toolbar" @submit.prevent="loadArticles"><el-input v-model="keyword" class="ticket-search" placeholder="搜索标题、摘要或正文" :prefix-icon="Search" clearable /><el-select v-model="categoryId" placeholder="全部分类" clearable><el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select><el-button type="primary" native-type="submit" :icon="Search">检索</el-button></form>
      <el-table v-loading="loading" :data="rows"><el-table-column prop="title" label="标题" min-width="220" /><el-table-column label="分类" width="140"><template #default="scope">{{ categories.find(item => item.id === scope.row.categoryId)?.categoryName ?? '-' }}</template></el-table-column><el-table-column prop="status" label="状态" width="110" /><el-table-column prop="visibility" label="范围" width="120" /><el-table-column prop="updatedAt" label="更新时间" width="170" /><el-table-column label="操作" width="220"><template #default="scope"><el-button link @click="openEdit(scope.row)">查看/编辑</el-button><el-button v-if="canWrite() && scope.row.status === 'DRAFT'" link type="success" @click="publish(scope.row)">发布</el-button><el-button v-if="canWrite() && scope.row.status !== 'ARCHIVED'" link type="danger" @click="archive(scope.row)">归档</el-button></template></el-table-column></el-table>
      <div class="table-footer"><span>共 {{ total }} 条</span><el-pagination background layout="prev,pager,next" :total="total" :page-size="pageSize" v-model:current-page="pageNum" @current-change="loadArticles" /></div></section>
    <el-dialog v-model="articleVisible" :title="editing ? '编辑知识文章' : '新建知识文章'" width="680px"><el-form label-position="top"><el-form-item label="分类"><el-select v-model="articleForm.categoryId"><el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select></el-form-item><el-form-item label="标题"><el-input v-model="articleForm.title" /></el-form-item><el-form-item label="摘要"><el-input v-model="articleForm.summary" type="textarea" /></el-form-item><el-form-item label="正文"><el-input v-model="articleForm.content" type="textarea" :rows="10" /></el-form-item><el-form-item label="可见范围"><el-radio-group v-model="articleForm.visibility"><el-radio value="TENANT">全租户</el-radio><el-radio value="DEPARTMENT">指定部门</el-radio></el-radio-group></el-form-item><el-form-item v-if="articleForm.visibility === 'DEPARTMENT'" label="部门 ID"><el-input-number v-model="articleForm.departmentId" :min="1" /></el-form-item></el-form><template #footer><el-button @click="articleVisible=false">取消</el-button><el-button type="primary" @click="saveArticle">保存草稿</el-button></template></el-dialog>
    <el-dialog v-model="categoryVisible" title="新建分类" width="480px"><el-form label-position="top"><el-form-item label="分类编码"><el-input v-model="categoryForm.categoryCode" placeholder="例如 INCIDENT_RUNBOOK" /></el-form-item><el-form-item label="分类名称"><el-input v-model="categoryForm.categoryName" /></el-form-item><el-form-item label="说明"><el-input v-model="categoryForm.description" type="textarea" /></el-form-item><el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="0" /></el-form-item></el-form><template #footer><el-button @click="categoryVisible=false">取消</el-button><el-button type="primary" @click="saveCategory">创建</el-button></template></el-dialog>
  </main>
</template>
