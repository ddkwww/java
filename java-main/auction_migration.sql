-- 拍卖行迁移：为功法(t_manual)与法宝(t_treasure)增加拍卖字段
-- auction: 0=未拍卖（可进入拍卖行）/ 1=已拍卖（已拍出，移出拍卖池），默认 0
-- 注：后端已开启 spring.jpa.hibernate.ddl-auto=update，应用启动会自动建列；
--     此脚本用于手动审计/在别的库上复现。

USE fanren_login;

ALTER TABLE `t_manual`
    ADD COLUMN `auction` INT NOT NULL DEFAULT 0
    AFTER `price`;

ALTER TABLE `t_treasure`
    ADD COLUMN `auction` INT NOT NULL DEFAULT 0
    AFTER `price`;

-- 校验
-- SELECT id, name, auction FROM t_manual WHERE auction = 1;
-- SELECT id, name, auction FROM t_treasure WHERE auction = 1;
