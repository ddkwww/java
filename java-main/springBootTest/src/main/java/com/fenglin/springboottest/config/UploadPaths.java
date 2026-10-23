package com.fenglin.springboottest.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 功法图片上传的共享路径常量。
 * 保存到项目源码树的 static/images/manuals 目录（与种子图同目录），
 * 通过 UploadConfig 将该目录映射为静态资源 URL（/images/manuals/**），
 * 保证 jar 运行模式下也能正常写入与访问。
 */
public final class UploadPaths {

    /** 对外暴露的 URL 前缀（存入 t_manual.image 字段的值） */
    public static final String URL_PREFIX = "/images/manuals";

    /** 物理目录：<工作目录>/src/main/resources/static/images/manuals */
    public static final Path DIR = Paths.get(
            System.getProperty("user.dir"),
            "src", "main", "resources", "static", "images", "manuals");

    private UploadPaths() {}
}
