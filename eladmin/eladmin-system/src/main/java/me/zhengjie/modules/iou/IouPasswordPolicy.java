package me.zhengjie.modules.iou;

import me.zhengjie.exception.BadRequestException;

/** 电子借条密码保留字符串形式，恰好六位 ASCII 数字。 */
public final class IouPasswordPolicy {
    private IouPasswordPolicy() {}

    public static void validate(String password) {
        if (password == null || !password.matches("[0-9]{6}")) {
            throw new BadRequestException("密码必须为 6 位数字");
        }
    }
}
