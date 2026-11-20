<template>
  <aside class="sidebar" :class="{ collapsed }">
    <!-- 侧栏抬头 + 收起/展开 -->
    <div class="sb-head">
      <div v-show="!collapsed" class="sb-title">
        <span class="seal">仙</span>
        <span class="sb-title-text">修 仙 导 航</span>
      </div>
      <button class="fold-btn" :title="collapsed ? '展开侧栏' : '收起侧栏'" @click="collapsed = !collapsed">
        {{ collapsed ? '»' : '«' }}
      </button>
    </div>

    <!-- 个人资料卡 -->
    <div class="profile-card">
      <div class="avatar">
        <img :src="avatarUrl" :alt="auth.user || '道友'" />
        <span class="avatar-ring"></span>
      </div>
      <div v-show="!collapsed" class="profile-meta">
        <div class="p-name">{{ auth.user || '无名道友' }}</div>
        <div class="p-realm"><span class="realm-dot"></span>{{ realm }}</div>
        <div class="p-stone"><i class="stone-mini">灵</i>{{ auth.spiritStone }}</div>
      </div>
    </div>

    <!-- 主导航 -->
    <nav class="sb-nav">
      <a
        v-for="it in navs"
        :key="it.name"
        class="nav-item"
        :class="{ active: it.active }"
        :title="it.label"
        @click="go(it.path)"
      >
        <span class="nav-icon">{{ it.icon }}</span>
        <span v-show="!collapsed" class="nav-label">{{ it.label }}</span>
        <span v-if="it.badge" class="nav-badge">{{ it.badge }}</span>
      </a>
    </nav>

    <!-- 底部操作 -->
    <div class="sb-foot">
      <a class="foot-item" title="个人资料" @click="go('/stats')">
        <span class="nav-icon">我</span>
        <span v-show="!collapsed" class="nav-label">个人资料</span>
      </a>
      <a class="foot-item" title="修改密码" @click="go('/change-password')">
        <span class="nav-icon">改</span>
        <span v-show="!collapsed" class="nav-label">修改密码</span>
      </a>
      <a v-if="auth.permission === 0" class="foot-item admin" title="后台管理" @click="go('/admin/login')">
        <span class="nav-icon">管</span>
        <span v-show="!collapsed" class="nav-label">后台管理</span>
      </a>
      <a class="foot-item danger" title="退归山林" @click="onLogout">
        <span class="nav-icon">退</span>
        <span v-show="!collapsed" class="nav-label">退归山林</span>
      </a>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { avatarImage, defaultAvatars, AVATAR_IMAGES } from '../assets/avatars/index'

const props = defineProps({
  unread: { type: Number, default: 0 }
})

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

// 窄屏默认收起，给内容区留空间
onMounted(() => { if (window.innerWidth <= 900) collapsed.value = true })

const realm = computed(() => auth.profile?.realm || '炼气')

const avatarUrl = computed(() => {
  const a = auth.profile?.avatar
  return (a && avatarImage(a)) || AVATAR_IMAGES[defaultAvatars(auth.profile?.gender)[0]]
})

const navs = computed(() => [
  { name: 'home',      label: '仙途首页', icon: '府', path: '/',            active: route.name === 'home' },
  { name: 'sutras',    label: '藏经阁',   icon: '经', path: '/sutras',      active: ['sutras', 'manual'].includes(route.name) },
  { name: 'treasures', label: '法宝阁',   icon: '宝', path: '/treasures',   active: ['treasures', 'treasure'].includes(route.name) },
  { name: 'stats',     label: '修为',     icon: '修', path: '/stats',       active: route.name === 'stats' },
  { name: 'sects',     label: '门派',     icon: '派', path: '/sects',       active: route.name === 'sects' },
  { name: 'tianjiao',  label: '天骄榜',   icon: '骄', path: '/tianjiao',    active: route.name === 'tianjiao' },
  { name: 'duel',      label: '论道对战', icon: '战', path: '/duel',        active: route.name === 'duel' },
  { name: 'auction',   label: '拍卖行',   icon: '拍', path: '/auction',     active: route.name === 'auction' },
  { name: 'games',     label: '小游戏',   icon: '戏', path: '/games',       active: ['games', 'monopoly'].includes(route.name) },
  {
    name: 'friend', label: '好友', icon: '友', path: '/friend', active: route.name === 'friend',
    badge: props.unread > 0 ? (props.unread > 99 ? '99+' : String(props.unread)) : ''
  }
])

function go(path) { router.push(path) }
function onLogout() { auth.logout(); router.push('/login') }
</script>

<style scoped>
.sidebar{
  flex:none;width:232px;
  display:flex;flex-direction:column;
  height:100%;overflow-y:auto;overflow-x:hidden;
  background:linear-gradient(180deg,rgba(10,16,32,.92),rgba(7,10,20,.86));
  border-right:1px solid var(--line);
  backdrop-filter:blur(12px);
  transition:width .25s ease;
  scrollbar-width:thin;scrollbar-color:rgba(233,200,120,.25) transparent;
}
.sidebar::-webkit-scrollbar{width:5px}
.sidebar::-webkit-scrollbar-thumb{background:rgba(233,200,120,.25);border-radius:4px}
.sidebar.collapsed{width:76px}

/* 抬头 */
.sb-head{
  flex:none;display:flex;align-items:center;justify-content:space-between;gap:8px;
  padding:16px 14px;border-bottom:1px solid var(--line);
}
.sb-title{display:flex;align-items:center;gap:9px;min-width:0}
.sb-title .seal{
  width:28px;height:28px;flex:none;border-radius:8px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;font-size:14px;
  box-shadow:0 0 14px rgba(233,200,120,.40);
}
.sb-title-text{
  font-size:14px;letter-spacing:3px;font-weight:700;white-space:nowrap;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
}
.fold-btn{
  flex:none;width:26px;height:26px;border-radius:8px;cursor:pointer;
  border:1px solid var(--line);background:rgba(20,29,54,.5);color:var(--ink-dim);
  font-size:14px;line-height:1;transition:.2s;
}
.fold-btn:hover{color:var(--gold-2);border-color:rgba(233,200,120,.45);background:rgba(233,200,120,.10)}
.collapsed .sb-head{justify-content:center;padding:14px 0}

/* 个人资料卡 */
.profile-card{
  flex:none;display:flex;align-items:center;gap:12px;
  margin:14px 12px;padding:12px;border-radius:14px;
  border:1px solid rgba(233,200,120,.22);
  background:linear-gradient(135deg,rgba(233,200,120,.10),rgba(20,29,54,.5));
}
.avatar{position:relative;width:46px;height:46px;flex:none}
.avatar img{width:100%;height:100%;border-radius:50%;object-fit:cover;display:block}
.avatar-ring{
  position:absolute;inset:-3px;border-radius:50%;pointer-events:none;
  border:1px solid rgba(233,200,120,.55);
  box-shadow:0 0 12px rgba(233,200,120,.28);
}
.profile-meta{min-width:0}
.p-name{font-size:15px;font-weight:700;letter-spacing:1px;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.p-realm{display:flex;align-items:center;gap:5px;font-size:12px;color:var(--gold-2);margin-top:3px}
.realm-dot{width:6px;height:6px;border-radius:50%;background:var(--qi);box-shadow:0 0 8px var(--qi)}
.p-stone{display:flex;align-items:center;gap:5px;font-size:12px;color:var(--gold-2);margin-top:5px;font-weight:700}
.stone-mini{width:15px;height:15px;border-radius:50%;display:grid;place-items:center;font-style:normal;font-size:9px;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#0c1322}
.collapsed .profile-card{justify-content:center;margin:14px 8px;padding:10px 0}

/* 导航 */
.sb-nav{flex:none;display:flex;flex-direction:column;gap:3px;padding:6px 10px}
.nav-item{
  position:relative;display:flex;align-items:center;gap:11px;
  height:42px;padding:0 10px;border-radius:11px;cursor:pointer;
  color:var(--ink-dim);font-size:14px;letter-spacing:2px;
  border:1px solid transparent;transition:.18s;white-space:nowrap;
}
.nav-item:hover{color:var(--gold-2);background:rgba(233,200,120,.08);border-color:rgba(233,200,120,.20)}
.nav-item.active{
  color:var(--gold-2);
  background:linear-gradient(90deg,rgba(233,200,120,.16),rgba(233,200,120,.02));
  border-color:rgba(233,200,120,.28);
}
.nav-item.active::before{
  content:'';position:absolute;left:-10px;top:9px;bottom:9px;width:3px;border-radius:0 3px 3px 0;
  background:linear-gradient(180deg,var(--gold-2),var(--gold));box-shadow:0 0 10px rgba(233,200,120,.6);
}
.nav-icon{
  width:28px;height:28px;flex:none;border-radius:8px;display:grid;place-items:center;
  font-size:13px;font-weight:700;letter-spacing:0;
  border:1px solid rgba(140,170,220,.22);background:rgba(20,29,54,.55);
}
.nav-item.active .nav-icon{
  border-color:rgba(233,200,120,.45);background:rgba(233,200,120,.14);color:var(--gold-2);
  box-shadow:0 0 12px rgba(233,200,120,.20);
}
.nav-label{overflow:hidden;text-overflow:ellipsis}
.nav-badge{
  position:absolute;top:4px;right:6px;min-width:16px;height:16px;padding:0 4px;
  border-radius:999px;display:grid;place-items:center;font-size:10px;font-weight:700;color:#fff;
  background:linear-gradient(135deg,#ff6b6b,#e04040);box-shadow:0 0 8px rgba(255,80,80,.6);
}
.collapsed .sb-nav{padding:6px 8px}
.collapsed .nav-item{justify-content:center;padding:0}

/* 底部操作 */
.sb-foot{margin-top:auto;display:flex;flex-direction:column;gap:2px;padding:8px 10px 14px;border-top:1px solid var(--line)}
.foot-item{
  display:flex;align-items:center;gap:11px;height:38px;padding:0 10px;border-radius:10px;
  cursor:pointer;color:var(--ink-dim);font-size:13px;letter-spacing:1px;white-space:nowrap;
  border:1px solid transparent;transition:.18s;
}
.foot-item:hover{color:var(--gold-2);background:rgba(233,200,120,.08)}
.foot-item.admin{color:var(--gold-2)}
.foot-item.danger{color:#ff8a8a}
.foot-item.danger:hover{background:rgba(255,138,138,.12)}
.collapsed .sb-foot{padding:8px 8px 14px}
.collapsed .foot-item{justify-content:center;padding:0}
</style>
