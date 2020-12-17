package com.gaad.rabbitmq.fgb.model.constants;

import java.io.Serializable;

/**
 * wtoip-framework
 *
 * @author Tokey
 * @version 1.0.0
 * @since 2017/09/25 16:27
 */
public enum BaseCodeContant implements Serializable {


    success(1, "操作成功"),
    fail(2, "操作失败"),
    systemBusy(3, "系统繁忙");

    int code;

    String codeExplain;

    BaseCodeContant(int code, String codeExplain) {
        this.code = code;
        this.codeExplain = codeExplain;
    }


    public int getCode() {
        return code;
    }

    public String getCodeExplainByCode(int code) {
        for (BaseCodeContant baseCode : values()) {
            if (baseCode.getCode() == code) {
                return baseCode.codeExplain;
            }
        }
        return null;
    }


    public String getCodeExplain() {
        return codeExplain;
    }

    @Override
    public String toString() {
        return "BaseCode{" +
                "code=" + code +
                ", codeExplain='" + codeExplain + '\'' +
                '}';
    }
}
