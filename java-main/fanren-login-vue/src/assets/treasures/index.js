// 法宝阁 · 法宝插画清单（本地兜底）
//
// 与藏经阁（src/assets/manuals）一致：把每件法宝的本地国风插画放在这个目录，
// 并按下表映射到法宝名称，即可在「未连后端 / 后端上传目录被重置」时仍稳定显示图片，
// 不再出现“法宝图片没有了”的情况（reboot 免疫）。
//
// 用法：将图片（如 t01.jpg ...）放入本目录，并在 TREASURE_IMAGES 中按法宝名称登记即可。
// 本地生成的 3D 立绘已放在 public/treasures/（构建后由 /treasures/xxx.png 直接访问），
// 此处按下表按法宝名称映射到公开路径，未连后端时也能稳定显示。
const TREASURE_IMAGES = {
  '青竹蜂云剑': '/treasures/treasure-1.png',
  '风雷翅':     '/treasures/treasure-2.png',
  '玄天斩灵剑': '/treasures/treasure-3.png',
  '聚宝盆':     '/treasures/treasure-4.png',
  '虚天鼎':     '/treasures/treasure-5.png',
  '乾蓝冰焰':   '/treasures/treasure-6.png',
  '风雷旗':     '/treasures/treasure-7.png',
  '圭灵盾':     '/treasures/treasure-8.png',
  '七焰扇':     '/treasures/treasure-9.png',
  '墨蛟剑':     '/treasures/treasure-10.png',
  '五行环':     '/treasures/treasure-11.png',
  '天罗真甲':   '/treasures/treasure-12.png',
  '雷珠':       '/treasures/treasure-13.png',
  '黑风旗':     '/treasures/treasure-14.png',
  '纳物袋':     '/treasures/treasure-15.png',
  '化龙玺':     '/treasures/treasure-16.png'
}

/** 按法宝名称取本地插画；未收录时返回 null（卡片显示分类兜底底纹） */
export function treasureImage(name) {
  return TREASURE_IMAGES[name] || null
}
