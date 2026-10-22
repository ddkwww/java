<template>
  <div class="admin">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="brand">
        <span class="seal">藏</span>
        <div>
          <div class="t1">藏经阁 · 后台管理系统</div>
          <div class="t2">功法秘籍数据管理（增删改查）</div>
        </div>
      </div>
      <div class="actions">
        <router-link to="/" class="lnk">返回首页</router-link>
        <span class="who">{{ auth.user || '管理员' }}</span>
        <button class="logout" @click="onLogout">退出登录</button>
      </div>
    </header>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="search">
        <span class="si">寻</span>
        <input v-model="keyword" type="text" placeholder="按名称 / 分类 / 出处 搜索…" @keyup.enter="load" />
      </div>
      <button class="btn-add" @click="openCreate">＋ 新增功法</button>
    </div>

    <!-- 表格 -->
    <div class="panel">
      <table class="grid">
        <thead>
          <tr>
            <th class="c-id">ID</th>
            <th>名称</th>
            <th>分类</th>
            <th>品阶</th>
            <th>境界</th>
            <th>出处</th>
            <th class="c-op">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in filtered" :key="it.id" :data-grade="it.grade">
            <td class="c-id">{{ it.id }}</td>
            <td class="name">{{ it.name }}</td>
            <td>{{ it.category }}</td>
            <td><span class="grade" :class="'g-' + it.grade">{{ it.grade }}阶</span></td>
            <td>{{ it.realm }}</td>
            <td class="dim">{{ it.origin || '—' }}</td>
            <td class="c-op">
              <button class="op edit" @click="openEdit(it)">编辑</button>
              <button class="op del" @click="onDelete(it)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && !filtered.length">
            <td colspan="7" class="empty">藏经阁中尚无此典。</td>
          </tr>
        </tbody>
      </table>

      <div v-if="loading" class="loading">读取藏经中…</div>
      <div class="count">共 {{ filtered.length }} 卷</div>
    </div>

    <!-- 表单弹窗 -->
    <ManualFormModal
      :visible="modalVisible"
      :editing="editing"
      :categories="categories"
      :saving="saving"
      @close="onModalClose"
      @submit="onSubmitted"
    />

    <Toast :message="toastMsg" :visible="toastVisible" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { fetchManuals, fetchFacets, createManual, updateManual, deleteManual } from '../api/sutra'
import ManualFormModal from '../components/ManualFormModal.vue'
import Toast from '../components/Toast.vue'

const auth = useAuthStore()
const router = useRouter()

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const categories = ref([])
const modalVisible = ref(false)
const editing = ref(null)
const saving = ref(false)

const toastMsg = ref('')
const toastVisible = ref(false)
let toastTimer = 0
function toast(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toastVisible.value = false), 2200)
}

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return list.value
  return list.value.filter((m) =>
    [m.name, m.category, m.realm, m.origin, m.desc]
      .some((f) => (f || '').toLowerCase().includes(k))
  )
})

async function load() {
  loading.value = true
  try {
    const res = await fetchManuals({})
    list.value = res.items || []
    const f = await fetchFacets()
    categories.value = f.categories || []
  } catch (e) {
    toast(e.message || '读取失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  modalVisible.value = true
}
function openEdit(it) {
  editing.value = it
  modalVisible.value = true
}
function onModalClose() {
  modalVisible.value = false
}

async function onSubmitted(payload) {
  saving.value = true
  try {
    if (editing.value) {
      await updateManual(editing.value.id, payload)
      toast('已更新：' + payload.name)
    } else {
      await createManual(payload)
      toast('已新增：' + payload.name)
    }
    modalVisible.value = false
    await load()
  } catch (e) {
    toast(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(it) {
  if (!confirm(`确定删除「${it.name}」？此操作不可恢复。`)) return
  try {
    await deleteManual(it.id)
    toast('已删除：' + it.name)
    await load()
  } catch (e) {
    toast(e.message || '删除失败')
  }
}

function onLogout() {
  auth.logout()
  router.push('/admin/login')
}

onMounted(load)
onUnmounted(() => clearTimeout(toastTimer))
</script>

<style scoped>
.admin{
  position:relative;z-index:2;height:100%;overflow:auto;
  padding:0 0 40px;
}
/* 顶栏 */
.topbar{
  position:sticky;top:0;z-index:5;
  display:flex;align-items:center;justify-content:space-between;gap:16px;
  padding:16px 28px;
  background:rgba(7,10,20,.8);backdrop-filter:blur(12px);
  border-bottom:1px solid var(--line);
}
.brand{display:flex;align-items:center;gap:14px}
.seal{
  width:42px;height:42px;border-radius:11px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;font-size:20px;
  box-shadow:0 0 16px rgba(233,200,120,.4);
}
.t1{font-size:17px;letter-spacing:2px;color:var(--ink);font-weight:700}
.t2{font-size:12px;letter-spacing:2px;color:var(--ink-dim);margin-top:2px}
.actions{display:flex;align-items:center;gap:14px}
.lnk{color:var(--qi);text-decoration:none;font-size:14px;letter-spacing:1px;opacity:.85}
.lnk:hover{opacity:1;text-shadow:0 0 12px rgba(103,232,249,.5)}
.who{color:var(--ink-dim);font-size:14px;letter-spacing:1px}
.logout{
  padding:7px 16px;border-radius:10px;cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px;
  border:1px solid rgba(233,200,120,.35);background:transparent;color:var(--gold);transition:.2s;
}
.logout:hover{background:rgba(233,200,120,.12)}

/* 工具栏 */
.toolbar{
  display:flex;align-items:center;gap:14px;
  padding:22px 28px 14px;
}
.search{
  flex:1;max-width:420px;display:flex;align-items:center;gap:8px;
  padding:0 14px;border-radius:999px;
  border:1px solid var(--line);background:rgba(20,29,54,.5);transition:.2s;
}
.search:focus-within{border-color:rgba(103,232,249,.5)}
.si{color:var(--gold);font-size:14px}
.search input{
  flex:1;border:none;outline:none;background:transparent;color:var(--ink);
  font-family:inherit;font-size:14px;letter-spacing:1px;padding:11px 4px;
}
.btn-add{
  margin-left:auto;padding:11px 22px;border:none;border-radius:10px;cursor:pointer;
  font-family:inherit;font-size:15px;letter-spacing:3px;font-weight:700;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  box-shadow:0 8px 24px rgba(233,200,120,.3);transition:.2s;
}
.btn-add:hover{transform:translateY(-1px);box-shadow:0 12px 30px rgba(233,200,120,.45)}

/* 表格 */
.panel{
  margin:0 28px;padding:6px 6px 14px;border-radius:16px;
  border:1px solid var(--line);background:linear-gradient(180deg,rgba(20,29,54,.45),rgba(7,10,20,.55));
}
.grid{width:100%;border-collapse:collapse;font-size:14px}
.grid thead th{
  text-align:left;padding:14px 16px;color:var(--gold);font-weight:700;letter-spacing:2px;
  border-bottom:1px solid var(--line);font-size:13px;
}
.grid tbody td{padding:13px 16px;border-bottom:1px solid rgba(140,170,220,.08);color:var(--ink)}
.grid tbody tr:hover{background:rgba(103,232,249,.05)}
.c-id{width:64px;color:var(--ink-dim);font-variant-numeric:tabular-nums}
.name{font-weight:600;letter-spacing:1px}
.dim{color:var(--ink-dim)}
.grade{padding:3px 10px;border-radius:6px;font-weight:700;font-size:13px}
.g-天{color:#0c1322;background:linear-gradient(135deg,var(--gold-2),var(--gold))}
.g-地{color:#fff;background:rgba(139,109,255,.75)}
.g-玄{color:#062024;background:linear-gradient(135deg,#8be6f6,var(--qi))}
.g-黄{color:var(--ink-dim);background:rgba(139,155,189,.18);border:1px solid var(--line)}
.c-op{width:150px}
.op{
  padding:6px 14px;border-radius:8px;cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:1px;
  border:1px solid var(--line);background:transparent;transition:.2s;margin-right:8px;
}
.op.edit{color:var(--qi)}
.op.edit:hover{border-color:rgba(103,232,249,.5);background:rgba(103,232,249,.1)}
.op.del{color:#ff9a9a}
.op.del:hover{border-color:rgba(255,120,120,.5);background:rgba(255,120,120,.1)}
.empty{text-align:center;color:var(--ink-dim);letter-spacing:3px;padding:40px 0}
.loading{text-align:center;color:var(--ink-dim);letter-spacing:3px;padding:24px 0}
.count{padding:12px 16px 0;color:var(--ink-dim);font-size:13px;letter-spacing:2px}

@media(max-width:760px){
  .topbar,.toolbar{padding-left:16px;padding-right:16px}
  .panel{margin:0 16px}
  .t2{display:none}
  .grid thead th:nth-child(3),.grid tbody td:nth-child(3),
  .grid thead th:nth-child(6),.grid tbody td:nth-child(6){display:none}
}
</style>
