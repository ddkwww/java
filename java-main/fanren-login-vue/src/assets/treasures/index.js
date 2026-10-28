// 法宝阁 · 法宝插画清单（本地兜底）
//
// 与藏经阁（src/assets/manuals）一致：把每件法宝的本地国风插画放在这个目录，
// 并按下表映射到法宝名称，即可在「未连后端 / 后端上传目录被重置」时仍稳定显示图片，
// 不再出现“法宝图片没有了”的情况（reboot 免疫）。
//
// 用法：将图片（如 t01.jpg ...）放入本目录，并在 TREASURE_IMAGES 中按法宝名称登记即可。
// 当前未放本地图，treasureImage() 返回 null，卡片自动回退到「分类文字」兜底底纹。
const TREASURE_IMAGES = {}

/** 按法宝名称取本地插画；未收录时返回 null（卡片显示分类兜底底纹） */
export function treasureImage(name) {
  return TREASURE_IMAGES[name] || null
}
