/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api;

import java.util.HashMap;
import com.vega.service.api.response.Result;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.Constants;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.ConnectDao;
import com.vega.service.api.object.ConnectInfo;
import com.vega.service.api.object.ListenHistory;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;

/**
 *
 * @author TRAN NGOC
 */
public class ConnectStack {

    static Logger logger = Logger.getLogger(ConnectStack.class);
    ConnectDao db;
    private RestfulStack resfulStack;

    public ConnectStack() throws NamingException {
        this.db = new ConnectDao();
        db.start();
    }

    /**
     * Lay danh sach phat
     *
     * @param params
     * @return
     */
    public Object getListContentConnect(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        if (msisdn == null || "".equals(msisdn)) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        msisdn = Helper.formatMobileNumber(msisdn);
        logger.debug(" >> getListContentConnect msisdn: " + msisdn);
        // lay danh sach ban tin 
        ArrayList<ConnectInfo> arrayList = db.getListContentConnect();
        // lay question id moi nhat dang dc active va con han trl 
        ConnectInfo questionIdActiveNewest = db.getQuestionActive();
        // lay last question id ma nguoi dung trl cuoi cung
        ConnectInfo lastQuestionId = db.getLastQuestionAnswerByMsisdn(msisdn);
        boolean checkReject = db.checkSubRejectByMsisdn(msisdn);
        if (arrayList != null) {
            // lay lich su nghe
            ListenHistory listenHis = db.getListenHistoryConnect(msisdn);
            ArrayList<ConnectInfo> array_new = new ArrayList<>();
            if (arrayList.size() > 0) {
                if (listenHis != null) {
                    String contentListened = listenHis.getContentListened() == null
                            || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                    logger.info("arrayList ContentConnect :" + arrayList.size());
                    int lastContentId = listenHis.getContentId();
                    int duration = listenHis.getDuration();
                    // Neu dang nghe do thi cho file nghe cuoi cung len trc
                    if (lastContentId > 0) {
                        logger.info("lastContentId > 0 ContentConnect  \n");
                        for (int i = 0, n = arrayList.size(); i < n; i++) {
                            ConnectInfo item = arrayList.get(i);
                            if (item.getConnect_content_id() == lastContentId) {
                                array_new.add(item);
                                arrayList.remove(i);
                                break;
                            }
                            r.setDuration(String.valueOf(duration));
                            r.setHaveHis(1);
                        }

                        logger.info("arrayList ContentConnect After soft Last conntentUd  :" + arrayList.size());
                    }
                    r.setHaveHis(0);
                    ArrayList<ConnectInfo> array_listened = new ArrayList<>();
                    for (int i = 0, n = arrayList.size(); i < n; i++) {
                        ConnectInfo item = arrayList.get(i);
                        String a = ListenHistory.SEPERATOR + item.getConnect_content_id() + ListenHistory.SEPERATOR;
                        if (contentListened.contains(a)) {
                            array_listened.add(item);
                        } else {
                            array_new.add(item);
                        }
                    }
                    array_new.addAll(array_listened);
                } else {
                    r.setHaveHis(0);
                    array_new.addAll(arrayList);
                }
            } else {
                logger.info(" >>>> khong thay noi dung nao \n");
                r.setErrorCode(Constants.NO_DATA_FOUND);
                return r;
            }
            if (checkReject) {
                logger.info("checkReject == true  \n");
                ConnectInfo cInfo = new ConnectInfo();
                for (int i = 0; i < array_new.size(); i++) {
                    cInfo = array_new.get(i);
                    cInfo.setAnswerQuestion(3);
                }
            } else {
                logger.info("checkReject == false  \n");
                // check cac  truong hop chua trl cau hoi ,da trl cau hoi , set cac cau da trl het han roi
                int questionIdNewest = questionIdActiveNewest.getConnect_content_id();
                if (lastQuestionId != null && lastQuestionId.getConnect_content_id() > 0) {
                    logger.info("checkReject == false lastQuestionId > 0 " + lastQuestionId.getConnect_content_id() + "  \n");
                    int questionIdLast = lastQuestionId.getConnect_content_id();
                    ConnectInfo cInfo = new ConnectInfo();
                    for (int i = 0; i < array_new.size(); i++) {
                        cInfo = array_new.get(i);
                        if (cInfo.getConnect_content_id() == questionIdNewest && cInfo.getConnect_content_id() > questionIdLast) {
                            logger.info("AConntectStack setAnswerQuestion(0) \n");
                            cInfo.setAnswerQuestion(0);
                        } else if (cInfo.getConnect_content_id() == questionIdNewest && cInfo.getConnect_content_id() == questionIdLast) {
                            logger.info("AConntectStack setAnswerQuestion(1) \n");
                            cInfo.setAnswerQuestion(1);
                        } else {
                            logger.info("AConntectStack setAnswerQuestion(2) \n");
                            cInfo.setAnswerQuestion(2);
                        }
                    }
                } else {
                    logger.info("checkReject == false lastQuestionId < 0  \n");
                    ConnectInfo cInfo = new ConnectInfo();
                    for (int i = 0; i < array_new.size(); i++) {
                        cInfo = array_new.get(i);
                        if (cInfo.getConnect_content_id() == questionIdNewest) {
                            logger.info("BConntectStack setAnswerQuestion(0) \n");
                            cInfo.setAnswerQuestion(0);
                        } else {
                            logger.info("BConntectStack setAnswerQuestion(2) \n");
                            cInfo.setAnswerQuestion(2);
                        }
                    }
                }
            }
            ConnectInfo[] data = new ConnectInfo[array_new.size()];
            for (int i = 0; i < data.length; i++) {
                logger.info("ConntectStack getStart_date_question:" + array_new.get(i).getStart_date_question() + "\n");
                data[i] = array_new.get(i);
            }
            r.setConnectData(data);
            r.setTotal(String.valueOf(array_new.size()));
            r.setErrorCode(Constants.SUCCESS);
        } else {
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    /**
     * ham tra loi cau hoi
     *
     * @param params
     * @return
     */
    public Object answerConnectQuestion(HashMap<String, String> params) {
        Result rs = new Result();
        String msisdn = params.get("msisdn");
        int connect_content_id = Integer.valueOf(params.get("connect_content_id"));
        int answerKey = Integer.valueOf(params.get("answerKey"));
        int numberSameAnswer = Integer.valueOf(params.get("numberSameAnswer"));
        if ((msisdn == null || "".equals(msisdn)) && connect_content_id <= 0 && answerKey <= 0 && numberSameAnswer <= 0) {
            rs.setErrorCode(Constants.WRONG_PARAM);
            return rs;
        }
        msisdn = Helper.formatMobileNumber(msisdn);
        rs = db.answerConnectQuestion(msisdn, connect_content_id, answerKey, numberSameAnswer);
        return rs;
    }

    /**
     * update lich su nghe connect_content
     *
     * @param params
     * @return
     */
    public Object updateHistoryConnect(HashMap<String, String> params) {
        logger.info(" updateHistoryConnect : " + (String) params.get("last_content_id") + "\n");
        Result r = new Result();
        int checkHistoryExist = 0;
        try {
            String msisdn = (String) params.get("msisdn");
            String connect_content_id = (String) params.get("connect_content_id");
            String last_content_id = (String) params.get("last_content_id");
            String duration = (String) params.get("duration");
            if ((msisdn == null) || (msisdn.equals(""))) {
                r.setErrorCode("-2");
                return r;
            }
            ListenHistory listenHis = db.getListenHistoryConnect(msisdn);
            if (listenHis == null) {
                logger.debug("nulllllllllllllllllllllllll");
                checkHistoryExist = 1;
                listenHis = new ListenHistory();
            }
            String contentListened = (listenHis.getContentListened() == null)
                    || (listenHis.getContentListened().equalsIgnoreCase("")) ? "" : listenHis.getContentListened();

            boolean hasHistory = !contentListened.equals("");
            if ((connect_content_id != null) && (!"".equals(connect_content_id))) {
                if (hasHistory) {
                    String key = "-" + connect_content_id + "-";
                    if (!contentListened.contains(key)) {
                        contentListened = contentListened + connect_content_id + "-";
                    } else {
                        key = connect_content_id + "-";
                        contentListened = contentListened.replaceAll(key, "");
                        contentListened = contentListened + key;
                    }
                } else {
                    contentListened = "-" + connect_content_id + "-";
                }
            }
            logger.info(" last_content_id : " + last_content_id);
            if ((last_content_id != null) && (!"".equals(last_content_id))) {
                listenHis.setContentId(Integer.valueOf(last_content_id).intValue());
                listenHis.setDuration(Integer.valueOf(duration).intValue());
            } else {
                listenHis.setContentId(0);
                listenHis.setDuration(0);
            }
            listenHis.setContentListened(contentListened);
            listenHis.setMsisdn(msisdn);
            int ConnectResponse = -1;
            if ((!hasHistory) && (checkHistoryExist == 1)) {
                ConnectResponse = db.insertListenHistory(listenHis);
            } else {
                ConnectResponse = db.updateListenHistory(listenHis);
            }
            r.setErrorCode(String.valueOf(ConnectResponse));
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode("-1");
        }
        return r;
    }

    /**
     * tu choi choi game tren IVR
     *
     * @param params
     * @return
     */
    public Object rejectGameConnect(HashMap<String, String> params) {
        Result r = new Result();
        Result result = new Result();
        try {
            String msisdn = params.get("msisdn");
            int status = Integer.valueOf(params.get("status"));
            if (msisdn == null || msisdn.equals("")) {
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }
            result = db.checkCountReject(msisdn);
            String erroCodeCheck = result.getErrorCode();
            int countReject = 0;
            if ("0".equals(erroCodeCheck)) {
                // update
                countReject = result.getCount_reject();
                countReject = countReject + 1;
                int resultUpdate = db.updateRejectConnect(msisdn, countReject, status);
                if (resultUpdate == 0 && countReject % 3 == 0) {
                    // send mt thong bao cho khach hang
                    SMS sms = new SMS();
                    int mtType = SMSType.Nofity.getValue();
                    String mt = ConfigStack.getConfig("mt", "mt_notify_alome_connect", "");
                    sms.setMtContent(mt);
                    sms.setMsisdn(params.get("msisdn"));
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("NOTIFY REJECT ALOME CONNECT");
                    sms.setSource("IVR");
                    sms.setPackageId(0);
                    sms.setBrandName(true);
                    resfulStack.sendMT(sms);
                }
            } else {
                // insert
                countReject = countReject + 1;
                int resultInsert = db.insertRejectConnect(msisdn, countReject);
            }
            r.setErrorCode(String.valueOf(result));
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    /**
     * @return the resfulStack
     */
    public RestfulStack getResfulStack() {
        return resfulStack;
    }

    /**
     * @param resfulStack the resfulStack to set
     */
    public void setResfulStack(RestfulStack resfulStack) {
        this.resfulStack = resfulStack;
    }
}
