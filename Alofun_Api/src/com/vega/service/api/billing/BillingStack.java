/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.billing;


import com.vega.alome.sbb.billing.bundletype.BillingErrorCode;
import com.vega.alome.sbb.billing.bundletype.BillingSBBInterface;
import com.vega.alome.sbb.billing.bundletype.SubPackageInfo;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.Helper;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.object.BillingActivityInfo;
import com.vega.service.api.object.PackageInfo;
import com.vega.service.api.response.Result;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author PhongTom
 */
public class BillingStack {

    static transient Logger logger = Logger.getLogger(BillingStack.class);
    private DBStack db;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //Client client;
    private  BillingSBBInterface billingSBB;

    public BillingStack(){
              
    }
    
    public SubPackageInfo getSubPackage(HashMap<String, String> params) {
        SubPackageInfo sub = new  SubPackageInfo();
        logger.info("vao ham getSubPackage :");
        sub = getBillingSBB().getSubPackage(params);
        return sub;
    }

    public SubPackageInfo registerPackage(HashMap<String, String> params) {
        SubPackageInfo sub = new SubPackageInfo();
        logger.info("vao ham registerPackage :");
        sub =  getBillingSBB().registerPackage(params);
        return sub;
    }
  
    public SubPackageInfo cancelSubPackage(HashMap<String, String> params) {
           logger.info("vao ham cancelSubPackage :");
        SubPackageInfo cancelSubPackage = new SubPackageInfo();
        cancelSubPackage = getBillingSBB().cancelSubPackage(params);
        return cancelSubPackage;
    }

    public ArrayList<PackageInfo> getAllPackages() {
        return getDb().getListPackage();
    }

    public SubPackageInfo renewPackageNow(HashMap<String, String> params, int package_id) {
         logger.info("vao ham renewPackageNow :");
        SubPackageInfo sub = new SubPackageInfo();
        sub = getBillingSBB().renewPackage(params);
        return sub;
    } 
    public Result chargeGift(HashMap<String, String> params) {
        Result r = new Result();
        r.setErrorCode(Constants.WRONG_PARAM);
        logger.info("vao ham chargeGift :");
        // Loi tham gia dau vao
        if (params == null || Helper.isNull(params.get("msisdn"))
                || Helper.getInt(params.get("packageId")) < 0
                || Helper.getInt(params.get("subPackageId")) < 0
                || Helper.getInt(params.get("amount")) < 0) {
            return r;
        }

        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        int packageId = Helper.getInt(params.get("packageId"));
        int subPackageId = Helper.getInt(params.get("subPackageId"));
        int amount = Helper.getInt(params.get("amount"));
        String source = Helper.isNull(params.get("source")) ? "IVR"
                : params.get("source").toUpperCase();
        String keyword = Helper.isNull(params.get("keyword")) ? "Charge Gift"
                : params.get("keyword");
        logger.info("input chargeGift msisdn: " + msisdn + "; packageId: "
                + packageId + "; source: " + source + "; keyword: "
                + keyword + "; subPackageId: " + subPackageId);
        String currentTime = "";
        // Thoi gian dang ky goi cuoc
        Calendar c = Calendar.getInstance();
        currentTime = dateFormat.format(c.getTime());

        // Yeu cau charging
        BillingActivityInfo actInfo = new BillingActivityInfo();
        actInfo.setAction("gift");
        actInfo.setBillingRequest(currentTime);
        HashMap<String, String> paramsGift= new HashMap<>();
        paramsGift.put("msisdn", msisdn);
        paramsGift.put("packageId", packageId+"");
        paramsGift.put("subPackageId", subPackageId+"");
        paramsGift.put("source", source);
        paramsGift.put("keyword", "ChargeGift");
        BillingErrorCode resultCharging = getBillingSBB().chargeGift(paramsGift);
        logger.debug("resultCharging: " + resultCharging);
        actInfo.setBillingAt(dateFormat.format(new Date()));
        if (resultCharging.equals("0")) {
            // Charge cuoc thanh cong
            r.setErrorCode(String.valueOf(BillingErrorCode.Success.getValue()));
        } else if (resultCharging.equals("1")) {
            // Dang ky khong du tien
            r.setErrorCode(String.valueOf(BillingErrorCode.NotEnoughBalance.getValue()));
        } else {
            // Dang ky bi loi
            r.setErrorCode(String.valueOf(BillingErrorCode.SystemError.getValue()));
        }

        // Luu log giao dich dang ky
        String requestId = "";
        actInfo.setRequestId(requestId);
        actInfo.setMsisdn(msisdn);
        actInfo.setPackageId(packageId);
        actInfo.setSubPackageId(subPackageId);
        actInfo.setBillingType(Integer.parseInt(ConfigStack.getConfig("api_billing", "gift_type", "4")));
        actInfo.setAmount(amount);
        actInfo.setResult(String.valueOf(resultCharging.getValue()));
        actInfo.setSource(source);
        actInfo.setPromotion(0);
        actInfo.setDescription(keyword);
        getDb().insertBillingActivity(actInfo, null, null);

        logger.info("output chargeGift msisdn: " + msisdn
                + " => ErrorCode: " + r.getErrorCode());
        return r;
    }

    /**
     * @return the db
     */
    public DBStack getDb() {
        return db;
    }

    /**
     * @param db the db to set
     */
    public void setDb(DBStack db) {
        this.db = db;
    }

    /**
     * @return the billingSBB
     */
    public BillingSBBInterface getBillingSBB() {
        return billingSBB;
    }

    /**
     * @param billingSBB the billingSBB to set
     */
    public void setBillingSBB(BillingSBBInterface billingSBB) {
        this.billingSBB = billingSBB;
    }
}
