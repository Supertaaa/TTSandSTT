/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.response;

import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author ishop
 */
@XmlType(propOrder = {
    "errorid",
    "message",
    "info"
})
public class WSResult {

    private int errorid;
    private String message;
    private WSPackageInfo info;

    public int getErrorid() {
        return errorid;
    }

    public void setErrorid(int errorid) {
        this.errorid = errorid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the info
     */
    public WSPackageInfo getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(WSPackageInfo info) {
        this.info = info;
    }
}
