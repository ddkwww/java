// 注册页 · 默认头像清单（AI 生成国风头像，512px JPG）
// avatar 值形如 "avatar:male-1"；上传头像则以 dataURL 形式存库。
import male1 from './avatar-male-1.jpg'
import male2 from './avatar-male-2.jpg'
import male3 from './avatar-male-3.jpg'
import female1 from './avatar-female-1.jpg'
import female2 from './avatar-female-2.jpg'
import female3 from './avatar-female-3.jpg'

// 头像专属海报（每个默认头像对应一张大师级国风海报）
import posterMale1 from '../posters/poster-male-1.jpg'
import posterMale2 from '../posters/poster-male-2.jpg'
import posterMale3 from '../posters/poster-male-3.jpg'
import posterFemale1 from '../posters/poster-female-1.jpg'
import posterFemale2 from '../posters/poster-female-2.jpg'
import posterFemale3 from '../posters/poster-female-3.jpg'

/** 头像键名 -> 图片 URL */
export const AVATAR_IMAGES = {
  'avatar:male-1': male1,     // 青袍剑修
  'avatar:male-2': male2,     // 灰白道袍道人
  'avatar:male-3': male3,     // 墨蓝儒生
  'avatar:female-1': female1, // 红银剑修
  'avatar:female-2': female2, // 月白丹修
  'avatar:female-3': female3  // 紫衣仙子
}

/** 按性别取默认头像键名列表 */
export function defaultAvatars(gender) {
  const prefix = gender === '女' ? 'avatar:female-' : 'avatar:male-'
  return Object.keys(AVATAR_IMAGES).filter((k) => k.startsWith(prefix))
}

/** 头像键名 -> 图片 URL；上传的 dataURL 原样返回 */
export function avatarImage(avatar) {
  if (!avatar) return null
  if (avatar.startsWith('data:')) return avatar
  return AVATAR_IMAGES[avatar] || null
}

/** 头像键名 -> 专属海报 URL */
export const AVATAR_POSTERS = {
  'avatar:male-1': posterMale1,
  'avatar:male-2': posterMale2,
  'avatar:male-3': posterMale3,
  'avatar:female-1': posterFemale1,
  'avatar:female-2': posterFemale2,
  'avatar:female-3': posterFemale3,
}

/** 取头像对应的专属海报；上传头像或未知键返回 null（由调用方回退性别海报） */
export function avatarPoster(avatar) {
  if (!avatar || avatar.startsWith('data:')) return null
  return AVATAR_POSTERS[avatar] || null
}
