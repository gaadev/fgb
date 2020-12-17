
package com.gaad.rabbitmq.fgb.model.enums;

/**
 * Fgb调用类型
 *
 * @author loken
 * @Date 2020/12/3
 */
public enum FgbType {


    /**
     * 同步
     */
    SYNC(0, "SYNC"),
    /**
     * 异步
     */
    ASYNC(1, "ASYNC");

    private int type;
    private String name;

    FgbType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static FgbType getFgbType(Integer type) {
        if (type == null) {
            return SYNC;
        }
        for (FgbType e : FgbType.values()) {
            if (e.type == type) {
                return e;
            }
        }
        return SYNC;
    }
}
