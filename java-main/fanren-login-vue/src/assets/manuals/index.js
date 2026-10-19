// 藏经阁 · 功法秘籍插画清单
// 每部功法一张 AI 生成国风插画（480px JPG），按名称映射，
// 与后端 /api/manuals 数据（SutraDataSeeder）及前端本地 mock 均按「功法名称」对应。
import m01 from './m01.jpg'
import m02 from './m02.jpg'
import m03 from './m03.jpg'
import m04 from './m04.jpg'
import m05 from './m05.jpg'
import m06 from './m06.jpg'
import m07 from './m07.jpg'
import m08 from './m08.jpg'
import m09 from './m09.jpg'
import m10 from './m10.jpg'
import m11 from './m11.jpg'
import m12 from './m12.jpg'
import m13 from './m13.jpg'
import m14 from './m14.jpg'
import m15 from './m15.jpg'
import m16 from './m16.jpg'
import m17 from './m17.jpg'
import m18 from './m18.jpg'
import m19 from './m19.jpg'
import m20 from './m20.jpg'
import m21 from './m21.jpg'
import m22 from './m22.jpg'
import m23 from './m23.jpg'
import m24 from './m24.jpg'
import m25 from './m25.jpg'
import m26 from './m26.jpg'
import m27 from './m27.jpg'
import m28 from './m28.jpg'
import m29 from './m29.jpg'
import m30 from './m30.jpg'
import m31 from './m31.jpg'
import m32 from './m32.jpg'
import m33 from './m33.jpg'
import m34 from './m34.jpg'
import m35 from './m35.jpg'
import m36 from './m36.jpg'
import m37 from './m37.jpg'
import m38 from './m38.jpg'
import m39 from './m39.jpg'
import m40 from './m40.jpg'
import m41 from './m41.jpg'
import m42 from './m42.jpg'
import m43 from './m43.jpg'
import m44 from './m44.jpg'
import m45 from './m45.jpg'
import m46 from './m46.jpg'

// 功法名称 -> 插画 URL（顺序与 SutraDataSeeder 一致）
export const MANUAL_IMAGES = {
  '长春功': m01,
  '青元剑诀': m02,
  '大衍诀': m03,
  '梵圣真魔功': m04,
  '五雷正法': m05,
  '三阳决': m06,
  '明王诀': m07,
  '素女轮回功': m08,
  '托天魔功': m09,
  '驱物术': m10,
  '火弹术': m11,
  '雷闪术': m12,
  '冰针术': m13,
  '土甲术': m14,
  '火蟒术': m15,
  '清风诀': m16,
  '匿风遁': m17,
  '血影遁': m18,
  '惊神刺': m19,
  '血炼神光': m20,
  '煞丹术': m21,
  '融灵之术': m22,
  '春黎剑阵': m23,
  '青蟠剑阵': m24,
  '疾风九变': m25,
  '百脉炼宝诀': m26,
  '缩地成寸': m27,
  '三头六臂': m28,
  '筑基丹丹方': m29,
  '凝魂丹丹方': m30,
  '大衍丹丹方': m31,
  '天火液炼方': m32,
  '长春丹方': m33,
  '炼器总纲': m34,
  '青竹蜂云剑炼制术': m35,
  '傀儡术总诀': m36,
  '千竹教傀儡术': m37,
  '符箓基础': m38,
  '雷符术': m39,
  '五行灵符': m40,
  '阵法初解': m41,
  '幻阵术': m42,
  '困龙阵': m43,
  '玄天斩灵术': m44,
  '虚空炼器法': m45,
  '渡劫秘要': m46
}

/** 按功法名称取插画；未收录时返回 null（卡片显示分类兜底底纹） */
export function manualImage(name) {
  return MANUAL_IMAGES[name] || null
}
