package com.fenglin.springboottest;

import com.fenglin.springboottest.entity.Sect;
import com.fenglin.springboottest.repository.SectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门派数据灌入：应用启动时若 t_sect 为空，则录入《凡人修仙传》修仙门派/势力。
 * 仅灌一次（以表内是否有数据判定），便于后续在库中增删而不被覆盖。
 * @Order(30)：与功法/法宝种子互不依赖，独立灌入。
 */
@Component
@Order(30)
public class SectDataSeeder implements CommandLineRunner {

    private final SectRepository sectRepository;

    public SectDataSeeder(SectRepository sectRepository) {
        this.sectRepository = sectRepository;
    }

    @Override
    public void run(String... args) {
        if (sectRepository.count() > 0) {
            return; // 已录入，跳过
        }
        List<Sect> seed = List.of(
                mk("七玄门", "正道", "越国", "越国七大修仙门派之一，门规森严，以剑修为长，韩立早年的宗门。", "七绝剑气", "黄枫谷诸修"),
                mk("黄枫谷", "正道", "越国", "越国知名正道宗门，谷中多灵植灵药，善炼丹制符，门风平和。", "长春功", "韩立"),
                mk("掩月宗", "魔道", "越国", "越国魔道巨擘，行事诡秘，善驱鬼御魔，门徒众多。", "掩月魔功", "掩月老祖"),
                mk("巨剑门", "正道", "越国", "越国以巨剑立派的正道宗门，力大势沉，门下多炼体剑修。", "巨剑诀", "巨剑门主"),
                mk("天阙堡", "正道", "越国", "越国正道堡垒，据守一方，堡中修士擅防御布阵。", "天阙阵", "天阙堡主"),
                mk("落云宗", "正道", "天南", "天南正道大宗，云霞缭绕，宗内剑修与法修并重。", "落云剑经", "南宫婉"),
                mk("合欢宗", "魔道", "天南", "天南魔道双修宗门，男女双修采补，行事放浪不羁。", "合欢心法", "合欢宗主"),
                mk("极阴岛", "魔道", "天南", "天南魔道势力，盘踞海外仙岛，阴气森森，善驭阴煞。", "极阴煞功", "极阴祖师"),
                mk("星宫", "中立", "海外", "海外散修汇聚之地，与魔道分庭抗礼，宫主神通广大。", "星宿大法", "星宫宫主"),
                mk("魔道联盟", "魔道", "大晋", "大晋魔道诸派的联合，势力庞大，行事无忌。", "真魔功", "古魔"),
                mk("灵界宗门", "中立", "灵界", "飞升灵界后诸仙门统称，传承古老，境界深不可测。", "诸般神通", "灵界大能"),
                mk("散修", "散修", "各地", "无门无派的修仙者，凭机缘与苦修自成一脉，如韩立早年。", "各凭机缘", "韩立")
        );
        sectRepository.saveAll(seed);
        System.out.println("[Sect] 门派谱已录入 " + seed.size() + " 个修仙门派/势力。");
    }

    private Sect mk(String name, String camp, String region, String desc, String signature, String representative) {
        Sect s = new Sect();
        s.setName(name);
        s.setCamp(camp);
        s.setRegion(region);
        s.setDesc(desc);
        s.setSignature(signature);
        s.setRepresentative(representative);
        return s;
    }
}
