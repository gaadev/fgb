package com.gaad.rabbitmq.fgb.model.enums;

/**
 * FGB模式定义
 *
 * @author loken
 * @Date 2020/12/3
 */
public enum FgbMode {

    /**
     * 客户端
     */
    FGB_CLIENT(0, "客户端"),
    /**
     * 服务端
     */
    FGB_SERVER(1, "服务端");

    private int mode;

    private String name;

    FgbMode(int mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    public int getMode() {
        return mode;
    }

    public String getName() {
        return name;
    }

    public static FgbMode getFgbMode(Integer mode) {
        if (mode == null) {
            return FGB_SERVER;
        }
        for (FgbMode e : FgbMode.values()) {
            if (e.mode == mode) {
                return e;
            }
        }
        return FGB_SERVER;
    }
}
