package com.vega.service.api.ws;

import com.vega.service.api.RestfulStack;
import com.vega.service.api.common.Helper;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.object.PackageInfo;
import com.vega.service.api.response.Result;
import com.vega.service.api.response.WSPackageInfo;
import com.vega.service.api.response.WSResult;
import com.vega.service.api.response.WSListPackage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.apache.log4j.Logger;

@WebService(serviceName = "listener", targetNamespace = "http://ser")
public class WSService {

    static Logger logger = Logger.getLogger(WSService.class);
    private String serviceUrl;
    RestfulStack restfulStack;

    @WebMethod(exclude = true)
    public String getServiceUrl() {
        return serviceUrl;
    }

    @WebMethod(exclude = true)
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @WebMethod(exclude = true)
    public RestfulStack getRestfulStack() {
        return restfulStack;
    }

    @WebMethod(exclude = true)
    public void setRestfulStack(RestfulStack restfulStack) {
        this.restfulStack = restfulStack;
    }

    public WSService() throws Exception {
        logger.info("WSService IS INITIATED SUCCESSFULLY!");
    }

    @WebMethod(operationName = "Register")
    public String register(@WebParam(name = "Msisdn") String msisdn, @WebParam(name = "PackageCode") String packageCode, @WebParam(name = "Username") String username,
            @WebParam(name = "Password") String password) {

        logger.info("WSService.register START"
                + "msisdn: " + msisdn
                + ", packageCode: " + packageCode
                + ", username: " + username
                + ", password: " + password);

        String retval = "0|DANG_KY_KHONG_THANH_CONG_XIN_VUI_LONG_THU_LAI_SAU";

        // Neu UserName & Password khong hop le
        if (!username.equals(ConfigStack.getConfig("api_webservice", "username", "")) || !password.equals(ConfigStack.getConfig("api_webservice", "password", ""))) {
            retval = "0|Sai username hoac password";
            logger.info(retval);
            return retval;
        } else if (!(msisdn != null && Helper.isMobileNumber(msisdn))) {
            retval = "0|Msisdn khong dung";
            logger.info(retval);
            return retval;
        }
        // xu ly nghiep vu
        HashMap<String, String> properties = new HashMap<String, String>();
        msisdn = Helper.formatMobileNumber(msisdn);
        PackageInfo p = Helper.getPackageByName(
                packageCode, restfulStack.getListPackage(properties));
        properties.put("msisdn", msisdn);
        properties.put("source", "WebService");
        properties.put("packageId", p.getPackageId() + "");

        Result r = (Result) restfulStack.registerPackage(properties);
        if (r.getErrorCode().startsWith("0")) {
            String a1 = "";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
            try {
                Date b1 = sdf.parse(r.getExpireAt());
                a1 = format.format(b1);
            } catch (Exception ex) {
                a1 = format.format(new Date());
            }
            retval = "1|" + p.getPackageName() + "|" + r.getPackagePrice() + "|" + a1;
        } else if (r.getErrorCode().startsWith("401")) {
            retval = "3|THUE_BAO_KHONG_DU_TIEN";
        } else if (r.getErrorCode().startsWith("1")) {
            retval = "2|MA_GOI_DA_TON_TAI";
        }
        logger.info("WSService.register END, msisdn: " + msisdn + ", ret: " + retval);
        return retval;
    }

    @WebMethod(operationName = "Unregister")
    public String unRegister(@WebParam(name = "Msisdn") String msisdn, @WebParam(name = "PackageCode") String packageCode, @WebParam(name = "Username") String username,
            @WebParam(name = "Password") String password) {

        logger.info("WSService.unRegister START"
                + "msisdn: " + msisdn
                + ", packageCode: " + packageCode
                + ", username: " + username
                + ", password: " + password);

        String retval = "0|HUY_KHONG_THANH_CONG_XIN_VUI_LONG_THU_LAI_SAU";

        // Neu UserName & Password khong hop le
        if (!username.equals(ConfigStack.getConfig("api_webservice", "username", "")) || !password.equals(ConfigStack.getConfig("api_webservice", "password", ""))) {
            retval = "0|Sai username hoac password";
            logger.info(retval);
            return retval;
        } else if (!(msisdn != null && Helper.isMobileNumber(msisdn))) {
            retval = "0|Msisdn khong dung";
            logger.info(retval);
            return retval;
        }
        // xu ly nghiep vu
        HashMap<String, String> properties = new HashMap<String, String>();
        msisdn = Helper.formatMobileNumber(msisdn);
        PackageInfo p = Helper.getPackageByName(
                packageCode, restfulStack.getListPackage(properties));
        properties.put("msisdn", msisdn);
        properties.put("source", "WebService");
        properties.put("packageId", p.getPackageId() + "");

        Result r = (Result) restfulStack.cancelSubPackage(properties);
        if (r.getErrorCode().startsWith("0")) {
            retval = "1|HUY_THANH_CONG";
        } else if (r.getErrorCode().startsWith("1")) {
            retval = "2|HUY_KHONG_THANH CONG_DO_THUE_BAO_CHUA_DANG_KY";
        }
        logger.info("WSService.unRegister END, msisdn: " + msisdn + ", ret: " + retval);
        return retval;
    }

    @WebMethod(operationName = "CheckStatus")
    public WSResult checkstatus(@WebParam(name = "Msisdn") String msisdn, @WebParam(name = "Username") String username,
            @WebParam(name = "Password") String password) {
        WSResult r = new WSResult();
        r.setErrorid(0);
        logger.info("WSService.checkstatus START"
                + "msisdn: " + msisdn
                + ", username: " + username
                + ", password: " + password);

        // Neu UserName & Password khong hop le
        if (!username.equals(ConfigStack.getConfig("api_webservice", "username", "")) || !password.equals(ConfigStack.getConfig("api_webservice", "password", ""))) {
            r.setMessage("Sai username hoac password");
            return r;
        } else if (!(msisdn != null && Helper.isMobileNumber(msisdn))) {
            r.setMessage("Msisdn khong dung");
            return r;
        }
        // xu ly nghiep vu
        msisdn = Helper.formatMobileNumber(msisdn);
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("msisdn", msisdn);
        properties.put("source", "WebService");

        Result resp = (Result) restfulStack.getSubPackage(properties);
        if (resp.getErrorCode().startsWith("0")) {
            r.setErrorid(1);
            r.setMessage("THUE_BAO_DANG_SU DUNG DICH_VU");

            PackageInfo a = Helper.getPackageById(Integer.parseInt(resp.getPackageId()), restfulStack.getListPackage(properties));
            WSPackageInfo packageInfo = new WSPackageInfo();
            packageInfo.setPackagecode(a.getPackageName());
            packageInfo.setCycle(a.getTimeLife() + "");
            packageInfo.setPrice(a.getSubFee() + "");
            packageInfo.setDesc("Goi cuoc " + a.getPackageName());

            r.setInfo(packageInfo);
        }
        logger.info("WSService.checkstatus END, msisdn: " + msisdn);
        return r;
    }

    @WebMethod(action = "GetListPackage")
    public WSListPackage getListPackage(@WebParam(name = "Username") String username,
            @WebParam(name = "Password") String password) {
        WSListPackage result = new WSListPackage();
        logger.info("WSService.getListPackage START"
                + "username: " + username
                + ", password: " + password);

        // Neu UserName & Password khong hop le
        if (!username.equals(ConfigStack.getConfig("api_webservice", "username", "")) || !password.equals(ConfigStack.getConfig("api_webservice", "password", ""))) {
            return result;
        }
        // xu ly nghiep vu
        HashMap<String, String> properties = new HashMap<String, String>();

        ArrayList<PackageInfo> lists = restfulStack.getListPackage(properties);
        WSPackageInfo[] packageList = new WSPackageInfo[lists.size()];
        for (int i = 0; i < lists.size(); i++) {
            PackageInfo a = lists.get(i);

            WSPackageInfo packageInfo = new WSPackageInfo();
            packageInfo.setPackagecode(a.getPackageName());
            packageInfo.setCycle(a.getTimeLife() + "");
            packageInfo.setPrice(a.getSubFee() + "");
            packageInfo.setDesc("Goi cuoc " + a.getPackageName());
            
            packageList[i] = packageInfo;
        }
        
        logger.info("WSService.getListPackage END");
        result.setInfo(packageList);
        return result;
    }
}
