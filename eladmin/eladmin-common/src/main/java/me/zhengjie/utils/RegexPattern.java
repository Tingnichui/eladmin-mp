package me.zhengjie.utils;

public interface RegexPattern {
    /**
     * 匹配 html 的 <a></a> 及其内容
     */
    String A_TAG_REG = "<a[^>]*?>(.*?)</a>";

    /**
     * 匹配 html 的 <a></a> 标签内的链接
     */
    String HREF_REG = "href=\\\"([^\\\"]*)\\\"";

    /**
     * 匹配所有的空格、回车、换行符、制表符
     */
    String ALL_BLANK_REG = "\\s*|\\t|\\r|\\n";

    /**
     * 匹配所有的回车、换行符、制表符
     */
    String BLANK_EXCEPT_SPACE_REG = "\\t|\\r|\\n";

    /**
     * 匹配 html 中的注释
     * 例如：<!-- 一些注释 -->
     */
    String HTML_NOTES_REG = "<!--.*?-->";

    /**
     * 匹配 html 两个标签之间的
     * 例如：<div> <a> </a> </div>
     */
    String WHITE_SPACE_IN_HTML_TAG = ">\\s+<";
}
