package com.vega.service.api;

import com.vega.service.api.common.AwardExchangeResult;
import com.vega.service.api.common.AwardInfo;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.DateUtil;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.SubPointAction;
import com.vega.service.api.common.SubPointCheckingInfo;
import com.vega.service.api.common.SubProfileInfo;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.db.GamificationDAO;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;
import com.vega.service.api.object.SubPackageInfo;
import com.vega.service.api.response.Result;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

public class GamificationStack {

    private static Logger logger = Logger.getLogger(GamificationStack.class);
    private RestfulStack restfulStack;
    private DBStack dbStack;
    private GamificationDAO gamificationDAO;
    private Boolean bonusBirthDayRunning = false;
    private Boolean checkingPointRunning = false;
    private ThreadPoolExecutor executorCheckPoint;
    private int checkPointThread = 10;

    public GamificationStack() throws Exception {
        if (gamificationDAO == null) {
            gamificationDAO = new GamificationDAO();
            executorCheckPoint = ((ThreadPoolExecutor) Executors.newFixedThreadPool(10));
        }
    }

    /*
     * Gamification API
     */
    /**
     * Doi diem theo giai thuong
     * @param params
     * @return
     */
    public Object exchangePointToAward(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> GamificationStack exchangePointToAward");
        AwardExchangeResult result = new AwardExchangeResult();
        try {
            String msisdn = params.get("msisdn");
            String awardKey = params.get("awardKey");
            int quantity = Helper.getInt(params.get("quantity"), 0);
            int subPackageId = Helper.getInt(params.get("subPackageId"), 0);
            int packageId = Helper.getInt(params.get("packageId"), 0);
            String programName = "GAMIFICATION";

            if (!Helper.isNull(msisdn) && !Helper.isNull(awardKey) && quantity > 0 && subPackageId > 0) {

                result.setErrorCode(BillingErrorCode.SystemError.getValue());

                AwardInfo a = gamificationDAO.getAwardInfo(awardKey);
                logger.info(">>>>> Game Dao  exchangePointToAward getAwardInfo");
                if (!Helper.isNull(msisdn) && a != null) {
                    logger.info(">>>>> Game Dao  exchangePointToAward 2");
                    result = gamificationDAO.exchangePointToAward(msisdn, subPackageId, packageId, a, quantity, SubPointAction.ExchangeAward, programName);
                    if (result.getErrorCode() == BillingErrorCode.Success.getValue()) {
                        /*
                         * Push MT
                         */
                        String mt = "";
                        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
                        unusualSymbols.setDecimalSeparator(',');
                        unusualSymbols.setGroupingSeparator('.');

                        String strange = "###,###.###";
                        DecimalFormat weirdFormatter = new DecimalFormat(strange, unusualSymbols);
                        weirdFormatter.setGroupingSize(3);
                        String awardName = a.getAwardName();
                        int point = a.getPointExchange();
                        if (a.getFreeMinutes() > 0 || a.getDayUsing() > 0) {
                            mt = ConfigStack.getConfig("mt", "gameExchangePointToSub", "");
                            int dayUsing = a.getDayUsing() * quantity;
                            int freeMinutes = a.getFreeMinutes() * quantity;
                            point = point * quantity;
                            logger.info(">>> Diem doi thuong : " + point);
                            if (mt != null) {
                                mt = mt.replaceAll("\\{ngay\\}", String.valueOf(dayUsing));
                                mt = mt.replaceAll("\\{phut\\}", String.valueOf(freeMinutes));
                                mt = mt.replaceAll("\\{diem\\}", weirdFormatter.format(point));
                            }

                        } else {
                            mt = ConfigStack.getConfig("mt", "gameExchangePointToAward", "");
                            if (mt != null) {
                                mt = mt.replaceAll("\\{so_lan\\}", String.valueOf(quantity));
                                mt = mt.replaceAll("\\{diem\\}", weirdFormatter.format(point));
                                mt = mt.replaceAll("\\{ten_qua\\}", Helper.getUnsignedString(awardName));
                            }
                        }

                        if (mt != null) {
                            //Gui MT
                            int mtType = SMSType.Nofity.getValue();
                            SMS sms = new SMS();
                            sms.setMtContent(mt);
                            sms.setMsisdn(msisdn);
                            sms.setType(mtType);
                            sms.setHaveMO(false);
                            sms.setAction("sendGamification");
                            sms.setSource("IVR");
                            sms.setPackageId(packageId);
                            sms.setBrandName(true);
                            restfulStack.sendMT(sms);
                        }
                    }
                }
            } else {
                result.setErrorCode(BillingErrorCode.WrongParams.getValue());
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(BillingErrorCode.SystemError.getValue());
        }

        return result;
    }

    public Object getAwardInfoByKey(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> GamificationStack getAwardInfoByKey");
        Result result = new Result();
        try {
            String awardKey = params.get("awardKey");

            if (!Helper.isNull(awardKey)) {
                AwardInfo a = gamificationDAO.getAwardInfo(awardKey);
                if (a != null) {
                    result.setFreeMinutesOfAward(a.getFreeMinutes());
                    result.setDayUsingOfAward(a.getDayUsing());
                    result.setMinimumPointToAward(a.getPointExchange());
                    result.setName(a.getAwardNameSlug());
                    result.setErrorCode(Constants.SUCCESS);
                } else {
                    result.setErrorCode(Constants.NO_DATA_FOUND);
                }
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode("-1");
        }

        return result;
    }

    /**
     * Ham cong diem theo action 
     * @param params
     * @return
     */
    public Object addPointByAction(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> GamificationStack exchangePointToAward");
        Result result = new Result();
        SubPackageInfo sub = new SubPackageInfo();
        String msisdn = params.get("msisdn");
        int packageId = Helper.getInt(params.get("packageId"), 0);
        int subPackageId = Helper.getInt(params.get("subPackageId"), 0);
        sub.setMsisdn(msisdn);
        sub.setSubPackageId(subPackageId);
        sub.setPackageId(packageId);
        String desc = params.get("desc");
        int action = Helper.getInt(params.get("action"), 0);

        if (!Helper.isNull(msisdn) && action > 0) {
            BillingErrorCode rs = gamificationDAO.insertUpdateSubPoint(sub, action, action, desc);
            result.setErrorCode(String.valueOf(rs.getValue()));
        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }
        return result;
    }

    /*
     * Gamification Event
     */
    public void birthDayPointEvent() {
//        synchronized (bonusBirthDayRunning) {
//            if (bonusBirthDayRunning) {
//                logger.info("Previous birth day scheduler is running, exiting...");
//                return;
//            }
//            /*
//             * Locking processs
//             */
//            bonusBirthDayRunning = true;
//        }
//
//        /*
//         * Locking DB
//         */
//        int minuteTimeout = Helper.getInt(ConfigStack.getConfig("general", "birthday_point_lock_timeout", ""), 60);
//        String keyLocking = "birthday_point";
//        int result = dbStack.insertConfigKeyValueWithTimeout(keyLocking, ConfigStack.appServerIp, minuteTimeout);
//        if (result != 0) {
//            logger.info("Birthday scheduler was started from other server, exiting...");
//            bonusBirthDayRunning = false;
//            return;
//        }

        int delay = Helper.getInt(ConfigStack.getConfig("sub_message", "delay", "500"));
        int bonusBirthDayPoint = Helper.getInt(ConfigStack.getConfig("game", "birthDayPoint", "500"));
        ArrayList<SubProfileInfo> list = gamificationDAO.getSubProfileBirthDay();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (gamificationDAO.updateBonusBirthDay(list.get(i), bonusBirthDayPoint)) {
                    String mt = ConfigStack.getConfig("mt", "FRIEND_BIRTH_DAY_BONUS_POINT", "");
                    mt = mt.replaceAll("\\{diem\\}", String.valueOf(bonusBirthDayPoint));

                    //Gui MT
                    int mtType = SMSType.Nofity.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(list.get(i).getMsisdn());
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("birthDayPointEvent");
                    sms.setSource("IVR");
                    sms.setPackageId(0);
                    sms.setBrandName(true);
                    restfulStack.sendMT(sms);

                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e1) {
                            continue;
                        }
                    }
                }
            }
        }

        //Unlocking DB
        //dbStack.deleteConfigKeyValue(keyLocking);
        bonusBirthDayRunning = false;
        logger.info("onBirthDayPointEvent finished");
    }

    public void checkingPointEvent() {
        logger.info("checkingPointEvent called");
        /*
         * Get config
         */
        int dayUsingServiceToPoint = Helper.getInt(ConfigStack.getConfig("game", "dayUsingServiceToPoint", "30"));
        int pointDayUsingService = Helper.getInt(ConfigStack.getConfig("game", "pointDayUsingService", "200"));
        int pointCallDuration = Helper.getInt(ConfigStack.getConfig("game", "pointCallDuration", "10"));
        int callDurationToPoint = Helper.getInt(ConfigStack.getConfig("game", "callDurationToPoint", "10"));
        Calendar currentDate = Calendar.getInstance();

        try {
            //Get subs for checking point
            ArrayList<SubPointCheckingInfo> checklist = gamificationDAO.getListSubPointChecking();
            logger.info("Size check point :" + checklist.size() + "\n");
            for (int i = 0, n = checklist.size(); i < n; i++) {
            SubPointCheckingInfo c = checklist.get(0);
            logger.info("ActiveCoutTheard :" + executorCheckPoint.getActiveCount() + "\n");
            while (executorCheckPoint.getActiveCount() >= checkPointThread) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    logger.error("onCheckingPointEvent::InterruptedException: " + e);
                    e.printStackTrace();
                }
            }

            try {
                executorCheckPoint.submit(new CheckPointTask(c, dayUsingServiceToPoint, pointDayUsingService, pointCallDuration, callDurationToPoint, currentDate));
            } catch (Exception e1) {
                logger.error(c.getMsisdn()
                        + " submit checkpoint from scheduler error::Exception: "
                        + e1);
                e1.printStackTrace();
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
        logger.info("Checking point finished");

    }

    /**
     *
     */
    public class CheckPointTask implements Callable<Void> {

        private SubPointCheckingInfo c;
        private int dayUsingServiceToPoint;
        private int pointDayUsingService;
        private int pointCallDuration;
        private int callDurationToPoint;
        private Calendar currentDate;

        public CheckPointTask(final SubPointCheckingInfo subPoint, int dayUsingServiceToPoint, int pointDayUsingService, int pointCallDuration, int callDurationToPoint, final Calendar currentDate) {
            this.c = subPoint;
            this.dayUsingServiceToPoint = dayUsingServiceToPoint;
            this.pointDayUsingService = pointDayUsingService;
            this.pointCallDuration = pointCallDuration;
            this.callDurationToPoint = callDurationToPoint;
            this.currentDate = currentDate;
        }

        @Override
        public Void call() throws Exception {
            processCheckPointTask(c, dayUsingServiceToPoint, pointDayUsingService, pointCallDuration, callDurationToPoint, currentDate);
            return null;
        }
    }

    protected void processCheckPointTask(final SubPointCheckingInfo c, int dayUsingServiceToPoint, int pointDayUsingService, int pointCallDuration, int callDurationToPoint, final Calendar currentDate) {
        logger.info(" >>>>>>>>>>>>>>> GamificationStack processCheckPointTask");
        HashMap<SubPointAction, Integer> pointWithActions = new HashMap<SubPointAction, Integer>();
        /*
         * 1. Check using days
         */
        Timestamp checkingUsingDate = c.getCheckedUsingDate() != null ? c.getCheckedUsingDate() : c.getRegAt();
        if (c.getOldSubPackageId() == 0) {
            c.setOldSubPackageId(c.getSubPackageid());
        }

        if (c.getSubPackageid() != c.getOldSubPackageId()) {
            /*
             * Doi goi cuoc => tinh tu ngay dang ky 
             */
            checkingUsingDate = c.getRegAt();
            c.setOldSubPackageId(c.getSubPackageid());
        }
        c.setCheckedUsingDate(checkingUsingDate);
        Timestamp currentTimeStamp = new Timestamp(currentDate.getTimeInMillis());
        int days = DateUtil.daysBetween(checkingUsingDate, currentTimeStamp);
        logger.info("NGay su dung :"+days);

        /*
         * Cong so ngay su dung du truoc do
         */
        days = days + c.getRemainUsingDays();

        /*
         * Quy doi ra co chu ki duoc tinh diem
         */
        int periodUsingToPoint = days / dayUsingServiceToPoint;
        logger.debug("days: " + days + "; periodUsingToPoint: " + periodUsingToPoint + "; dayUsingServiceToPoint: " + dayUsingServiceToPoint);

        if (periodUsingToPoint > 0) {
            int pointForUsing = periodUsingToPoint * pointDayUsingService;
            logger.debug("pointForUsing: " + pointForUsing);
            pointWithActions.put(SubPointAction.DayUsing, pointForUsing);

            /*
             * Tinh lai so ngay su dung con lai 
             */
            c.setRemainUsingDays(days % dayUsingServiceToPoint);

            /*
             * Chot ngay su dung da quy doi diem
             */
            c.setCheckedUsingDate(currentTimeStamp);
        }

        /*
         * 2. Check call duration
         */
        boolean subHasCall = true;
        logger.info(">>>> Qui doi phut nghe Man :" + c.getMsisdn() + "  ;" + c.getLastCallDate() + ";" + c.getCheckedCallDate());
        if (c.getLastCallDate() != null && c.getCheckedCallDate() != null) {
            logger.info("......Man ...... ");
            Calendar lastCallDate = Calendar.getInstance();
            lastCallDate.setTimeInMillis(c.getLastCallDate().getTime());
            Calendar checkedCallDate = Calendar.getInstance();
            checkedCallDate.setTimeInMillis(c.getCheckedCallDate().getTime());

            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            logger.info("msisdn :" + c.getMsisdn() + ";" + "lastCallDate: " + f.format(lastCallDate.getTime()) + "; checkedCallDate: " + f.format(checkedCallDate.getTime()));
            if (checkedCallDate.after(lastCallDate)) {
                logger.info("checkedCallDate > lastCallDate");
                /*
                 * Thue bao chua phat sinh cuoc goi moi tu thoi diem chot truoc do
                 */
                subHasCall = false;
            }
        }
        logger.info(">>>> Qui doi phut nghe A:   \n");
        if (subHasCall) {
            logger.info(">>>> Qui doi phut nghe B:   \n");
            Calendar checkCallFromDate = null;
            if (c.getCheckedCallDate() != null) {
                checkCallFromDate = Calendar.getInstance();
                checkCallFromDate.setTimeInMillis(c.getCheckedCallDate().getTime());
                checkCallFromDate.set(Calendar.SECOND, checkCallFromDate.get(Calendar.SECOND) + 1);
            } else {
                checkCallFromDate = Calendar.getInstance();
                checkCallFromDate.set(Calendar.MILLISECOND, 0);
                checkCallFromDate.set(Calendar.SECOND, 0);
                checkCallFromDate.set(Calendar.MINUTE, 0);
                checkCallFromDate.set(Calendar.HOUR_OF_DAY, 0);
            }

            Calendar checkCallToDate = Calendar.getInstance();
            logger.info("getTotalDurationCallToMinutes for " + c.getMsisdn() + "; checkCallToDate: " + checkCallToDate.getTimeInMillis());
            int callDurationToMinutes = gamificationDAO.getTotalDurationCallToMinutes(c.getMsisdn(), new Timestamp(checkCallFromDate.getTimeInMillis()), new Timestamp(checkCallToDate.getTimeInMillis()));
            logger.info("callDurationToMinutes for " + c.getMsisdn() + ": " + callDurationToMinutes + "; callDurationToPoint: " + callDurationToPoint);
            if (callDurationToMinutes >= 0) {
                /*
                 * Cong them so du phut goi cua lan check point truoc do
                 */
                callDurationToMinutes = callDurationToMinutes + c.getRemainCallMinutes();
                int callPeriodToPoint = callDurationToMinutes / callDurationToPoint;
                logger.info("Diem cong callPeriodToPoint for " + c.getMsisdn() + ": " + callPeriodToPoint);
                if (callPeriodToPoint > 0) {
                    pointWithActions.put(SubPointAction.CallDuration, callPeriodToPoint * pointCallDuration);

                    /*
                     * Luu lai so du
                     */
                    c.setRemainCallMinutes(callDurationToMinutes % callDurationToPoint);
                } else {
                    logger.info(">>>> Qui doi phut nghe C:   \n");
                    /*
                     * Luu lai so du, lan sau tinh
                     */
                    c.setRemainCallMinutes(callDurationToMinutes);
                }

            } else {
                logger.info(">>>> Qui doi phut nghe D:   \n");
                /*
                 * Co loi khi thong ke
                 */
                return;
            }

            /*
             * Chot ngay goi da check point
             */
            logger.info(">>>> Qui doi phut nghe E:   \n");
            c.setCheckedCallDate(new Timestamp(checkCallToDate.getTimeInMillis()));
        }
        /*
         * 3. Cap nhat DB
         */
        logger.info(">>> Luu Diem cho thue bao :" + pointWithActions.size() + "\n");
        if (pointWithActions.size() > 0) {
            gamificationDAO.addPointByCheckingPoint(c, pointWithActions);
        }
        /*
         * 4. Ban tin
         */
    }

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

    public GamificationDAO getGamificationDAO() {
        return gamificationDAO;
    }

    public void setGamificationDAO(GamificationDAO gamificationDAO) {
        this.gamificationDAO = gamificationDAO;
    }
}
