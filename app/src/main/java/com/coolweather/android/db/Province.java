package com.coolweather.android.db;

import org.litepal.crud.DataSupport;

/**
 * Created by Administrator on 2018/2/10.
 * provinceName是返回的省级数据里的省的名称
 * provinceCode是返回的省级数据里的省的id
 * 注意跟这里的id是不一致的，这里的id是为数据库服务的，而code是为在服务器上查询市级数据服务的！
 * 后面两个实体类同理，并且county多了个weatherId，用来在服务器上查询县级的天气
 */

public class Province extends DataSupport {
    private int id;
    private String provinceName;
    private int provinceCode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}
