/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.response;

import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author minhnd
 */
@XmlType(propOrder = {
    "packagecode",
    "cycle",
    "price",
    "desc"
})
public class WSPackageInfo {

    private String packagecode;
    private String cycle;
    private String price;
    private String desc;

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPackagecode() {
        return packagecode;
    }

    public void setPackagecode(String packagecode) {
        this.packagecode = packagecode;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
