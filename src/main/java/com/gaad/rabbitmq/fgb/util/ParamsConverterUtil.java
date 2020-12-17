package com.gaad.rabbitmq.fgb.util;

import java.lang.reflect.Method;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author loken
 * @date 2020/12/3
 */
@SuppressWarnings("all")
public class ParamsConverterUtil {

    public static Object convertReturnType(Method method, JSONObject result) {
        if (result != null) {
            return JSON.parseObject(result.toString(), method.getGenericReturnType());

            // if (result != null) {
            // if (method.getreturntype() == resultbean.class) {
            // resultbean resultbean = json.parseobject(result.tojsonstring(),
            // resultbean.class);
            // if (method.getgenericreturntype() instanceof parameterizedtype) {
            // //获取泛型类型
            // type[] resultgenerictype = ((parameterizedtype)
            // method.getgenericreturntype()).getactualtypearguments();
            // object resultdata = resultbean.getdata();
            // if (null != resultdata) {
            // if (resultgenerictype[0] instanceof collection) {
            // try {
            // resultbean.setdata(jsonarray.parsearray(resultdata.tostring(),
            // resultgenerictype));
            // } catch (jsonexception e) {
            // resultbean.setdata(json.parsearray(json.tojsonstring(resultdata.tostring()),
            // resultgenerictype));
            // }
            // } else {
            // try {
            // resultbean.setdata(json.parseobject(resultdata.tostring(),
            // resultgenerictype[0]));
            // } catch (jsonexception e) {
            // resultbean.setdata(json.parseobject(json.tojsonstring(resultdata.tostring()),
            // resultgenerictype[0]));
            // }
            // }
            // }

            // }
            // return resultbean;
            // }
            // }
            // return resultbean.success();
        }
        return null;
    }
}
