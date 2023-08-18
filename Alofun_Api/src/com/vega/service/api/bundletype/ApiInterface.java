/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.bundletype;

/**
 *
 * @author PhongTom
 */
public interface ApiInterface {
    public String SendSMS(String msisdn, String mt, String Action, String Source, String subPackageId);
    public String onSMS(String keyWord, String msisdn, String serviceId);
}
