package com.vega.service.api;

import com.vega.service.api.billing.BillingStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.Helper;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.db.ToptenDAO;
import com.vega.service.api.object.TopTenHistoryInfo;
import com.vega.service.api.object.TopTenInfo;
import com.vega.service.api.response.Result;
import com.vega.service.api.response.ResultAbstract;
import com.vega.service.api.response.ResultTopten;
import com.vega.service.api.sms.SMSStack;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class ToptenStack {

    private static Logger logger = Logger.getLogger(ToptenStack.class);

    private RestfulStack restfulStack;
    private DBStack dbStack;
    private SMSStack smsStack;
    private BillingStack billing;
    private ToptenDAO toptenDao;

    public RestfulStack getRestfulStack() {
        return restfulStack;
    }

    public void setRestfulStack(RestfulStack restfulStack) {
        this.restfulStack = restfulStack;
    }

    public DBStack getDbStack() {
        return dbStack;
    }

    public void setDbStack(DBStack dbStack) {
        this.dbStack = dbStack;
    }

    public SMSStack getSmsStack() {
        return smsStack;
    }

    public void setSmsStack(SMSStack smsStack) {
        this.smsStack = smsStack;
    }

    public BillingStack getBilling() {
        return billing;
    }

    public void setBilling(BillingStack billing) {
        this.billing = billing;
    }
    
    public ToptenStack() throws Exception {
        if(toptenDao == null){
            toptenDao = new ToptenDAO();
            toptenDao.start();
        }
    }

    public ResultAbstract updateListenHistory(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Topten updateListenHistory");
        ResultTopten result = new ResultTopten();
        result.setErrorCode(Constants.SYSTEM_ERROR);
        try {
            TopTenHistoryInfo his = new TopTenHistoryInfo();
            his.setDuration(Helper.getInt(params.get("duration"), 0));
            his.setMsisdn(params.get("msisdn"));
            his.setOrderInUserList(Helper.getInt(params.get("order_in_user_list"), -1));
            his.setPackageId(Helper.getInt(params.get("package_id"), -1));
            his.setSubPackageId(Helper.getInt(params.get("sub_package_id"), -1));
            his.setToptenRecordId(Helper.getInt(params.get("topten_record_id"), -1));
            his.setStart_date(new Date(1000 * Helper.getLong(params.get("start_date"), 0)));
            int rs = toptenDao.updateListenHistory(his);
            logger.info("Update listen history result:" + rs);
            result.setErrorCode(Constants.SUCCESS);
        } catch (Exception e) {
            result.setErrorCode(Constants.WRONG_PARAM);
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public ResultAbstract getCountListendInNDays(HashMap<String, String> params) {
          logger.info(" >>>>>>>>>>>>>>> ToptenStack getCountListendInNDays");
        ResultTopten result = new ResultTopten();
        result.setErrorCode(Constants.SYSTEM_ERROR);
        String msisdn = params.get("msisdn");
        if (!Helper.isNull(msisdn)) {
            // p55_hd
            int nDays = Helper.getInt(ConfigStack.getConfig("topten", "number_of_days_to_show_guide", "-3"));
            int countListendInNDays = toptenDao.getCountListendInNDays(msisdn, nDays);
            result.setCountListendInNDays(countListendInNDays);
            result.setErrorCode(Constants.SUCCESS);
        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }
        logger.debug("getCountListendInNDays: " + result.toString());
        return result;
    }

    public ResultAbstract getListTopten(HashMap<String, String> params) {
       logger.info(" >>>>>>>>>>>>>>> Rs getListTopten");
        ResultTopten result = new ResultTopten();
        result.setErrorCode(Constants.SYSTEM_ERROR);
        String msisdn = params.get("msisdn");
        if (!Helper.isNull(msisdn)) {
            int noDay = Helper.getInt(ConfigStack.getConfig("topten", "number_of_days_history_to_re_listen", "30"));
            int limit = Helper.getInt(ConfigStack.getConfig("topten", "limit_number_of_records_return", "200"));
            List<TopTenInfo> toptenListOfUser = toptenDao.getToptenListOfUser(msisdn, noDay, limit);
            if (toptenListOfUser.size() <= 0) {
                result.setErrorCode(Constants.NO_DATA_FOUND);
            } else {
                result.setErrorCode(Constants.SUCCESS);
                result.setUserListTopten(toptenListOfUser);
                result.setTotal(toptenListOfUser.size());
            }
        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }
        logger.debug("getListTopten: " + result.toString());
        return result;
    }
}
