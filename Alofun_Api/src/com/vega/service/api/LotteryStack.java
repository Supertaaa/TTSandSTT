/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api;

import java.util.HashMap;
import com.vega.service.api.response.Result;
import com.vega.service.api.common.Helper;
import com.vega.service.api.object.LotteryHisDTO;
import com.vega.service.api.object.LotteryInfo;
import com.vega.service.api.common.Constants;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import com.vega.service.api.config.ConfigStack;
import java.util.Arrays;
import java.util.List;
import com.vega.service.api.common.ProvinceInfo;
import com.vega.service.api.db.LotteryDao;
import java.util.Calendar;

/**
 *
 * @author N.Tuyen
 */
public class LotteryStack {

    static Logger logger = Logger.getLogger(LotteryStack.class);
    LotteryDao db;
    // cac api xo so

    public LotteryStack() throws NamingException {
        this.db = new LotteryDao();
        db.start();
    }

    public Object getLotteryRegionOfSub(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");

        if (!Helper.isNull(msisdn)) {
            int region = db.getLotteryRegionOfSub(msisdn);
            if (region >= 0) {
                r.setRegion(String.valueOf(region));
                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object registerLotteryRegion(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int region = Helper.getInt(params.get("region"), 0);
        String channel = Helper.isNull(params.get("channel")) ? "IVR" : params.get("channel").toUpperCase();
        int switchRegion = Helper.getInt(params.get("switchRegion"), 0);

        if (!Helper.isNull(msisdn) && region > 0) {
            int ret = db.updateLotteryRegionOfSub(msisdn, region);
            if (ret == 0) {
                r.setErrorCode(Constants.SUCCESS);

                LotteryHisDTO his = new LotteryHisDTO();
                his.setMsisdn(msisdn);
                his.setChannel(channel);
                his.setAction(switchRegion == 1 ? LotteryHisDTO.ACT_SWITCH_REGION : LotteryHisDTO.ACT_REG_REGION);
                his.setProvinceId(0);
                his.setRegion(region);
                his.setResult(LotteryHisDTO.RES_SUCCESS);
                db.insertLotteryHis(his);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object registerLotteryCallout(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int region = Helper.getInt(params.get("region"), 0);
        int status = Helper.getInt(params.get("status"), 0);
        int provinceId = Helper.getInt(params.get("provinceId"), 0);
        String source = Helper.isNull(params.get("source")) ? "IVR" : params.get("source");
        String calloutDate = params.get("calloutDate");

        if (status == Constants.LOT_STATUS_CALLOUT_REG_ONE_DAY) {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
            sdf.setLenient(false);
            try {
                sdf.parse(calloutDate);
            } catch (Exception e) {
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }
        }

        if (!Helper.isNull(msisdn) && provinceId > 0 && region > 0) {
            int ret = db.updateLotteryCalloutStatusOfSub(msisdn, region, provinceId, status, calloutDate, source);
            if (ret == 0) {
                r.setErrorCode(Constants.SUCCESS);

                int action = LotteryHisDTO.ACT_SETUP_RECV_EVERY_DAY;
                if (status == Constants.LOT_STATUS_CALLOUT_REG_ONE_DAY) {
                    action = LotteryHisDTO.ACT_SETUP_RECV_BY_DAY;
                } else if (status == Constants.LOT_STATUS_CALLOUT_UNREG) {
                    action = LotteryHisDTO.ACT_CANCEL_RECV_RESULT;
                }

                LotteryHisDTO his = new LotteryHisDTO();
                his.setMsisdn(msisdn);
                his.setChannel(source);
                his.setAction(action);
                his.setProvinceId(provinceId);
                his.setRegion(region);
                his.setResult(LotteryHisDTO.RES_SUCCESS);

                db.insertLotteryHis(his);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object getStatusLotteryCallout(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int provinceId = Helper.getInt(params.get("provinceId"), 0);

        if (!Helper.isNull(msisdn) && provinceId > 0) {
            int status = db.getStatusCalloutOfSub(msisdn, provinceId);

            if (status >= 0) {
                r.setDesc(String.valueOf(status));
                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object getLotteryDataByProvince(HashMap<String, String> params) {
        Result r = new Result();

        int region = Helper.getInt(params.get("region"), 0);
        int provinceId = Helper.getInt(params.get("provinceId"), 0);
        String publishDate = params.get("publishDate");
        logger.debug("publishDate: " + publishDate);

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        sdf.setLenient(false);
        try {
            logger.debug("publishDate: " + publishDate);
            //sdf.parse(publishDate);
            logger.debug("publishDate length: " + publishDate.length());
            if (publishDate.length() != 8) {
                logger.debug("publishDate: " + publishDate);
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }
        } catch (Exception e) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }

        if (region > 0 && provinceId > 0) {
            logger.debug("publishDate: " + publishDate + "; region: " + region + "; provinceId: " + provinceId);
            ArrayList<LotteryInfo> list = db.getLotteryDataByDate(region, provinceId, publishDate);
            if (list != null) {
                if (list.size() > 0) {
                    LotteryInfo[] data = new LotteryInfo[list.size()];
                    Helper.copyListToArray(list, data);

                    r.setLotteryData(data);
                }

                r.setTotal(String.valueOf(list.size()));
                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object getRatingOfLotteryInDateRange(HashMap<String, String> params) {
        Result r = new Result();

        int region = Helper.getInt(params.get("region"), 0);
        int provinceId = Helper.getInt(params.get("provinceId"), 0);
        int level = Helper.getInt(params.get("level"), 1);
        int dateRange = Helper.getInt(ConfigStack.getConfig("lottery", "date_range_level_" + String.valueOf(level), "0"), 0);

        if (region > 0 && provinceId > 0 && dateRange > 0) {
            ArrayList<LotteryInfo> postfixList = db.getMinAndMaxRatingPostfixOfLotteryData(region, provinceId, dateRange);
            ArrayList<LotteryInfo> prefixList = db.getMinAndMaxRatingPrefixOfLotteryData(region, provinceId, dateRange);
            if (postfixList != null
                    && prefixList != null
                    && postfixList.size() > 0
                    && prefixList.size() > 0) {

                r.setLotNumber1(postfixList.get(0).getPostfixVal());
                r.setLotNumber2(postfixList.size() > 1 ? postfixList.get(1).getPostfixVal() : "0");
                r.setLotNumber3(prefixList.get(0).getPostfixVal());
                r.setLotNumber4(prefixList.size() > 1 ? prefixList.get(1).getPostfixVal() : "0");

                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object getRatingOfLotteryInDateRangeByNumber(HashMap<String, String> params) {
        Result r = new Result();

        int region = Helper.getInt(params.get("region"), 0);
        int provinceId = Helper.getInt(params.get("provinceId"), 0);
        int level = Helper.getInt(params.get("level"), 1);
        logger.debug("date_range_level_ : " + ConfigStack.getConfig("lottery", "date_range_level_" + String.valueOf(level), "1"));
        int dateRange = Helper.getInt(ConfigStack.getConfig("lottery", "date_range_level_" + String.valueOf(level), "1"), -1);
        String pairOfNumber = params.get("number");
        if (Helper.isNull(pairOfNumber)
                || pairOfNumber.length() != 2
                || Helper.getInt(pairOfNumber, -1) == -1) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }

        if (region > 0 && provinceId > 0 && dateRange >= 0) {
            ArrayList<LotteryInfo> postfixList = db.getQuantityPostfixOfLotteryData(region, provinceId, pairOfNumber, dateRange);
            ArrayList<LotteryInfo> prefixList = db.getQuantityPrefixOfLotteryData(region, provinceId, pairOfNumber, dateRange);
            if (postfixList != null
                    && prefixList != null) {
                int quantityPostfixSpecial = 0;
                int quantityPrefixSpecial = 0;
                int quantityPostfixOther = 0;
                int quantityPrefixOther = 0;

                if (postfixList.size() > 0 && postfixList.get(0).getLotNo() == 0) {
                    quantityPostfixSpecial = postfixList.get(0).getTotal();
                }

                if (prefixList.size() > 0 && prefixList.get(0).getLotNo() == 0) {
                    quantityPrefixSpecial = prefixList.get(0).getTotal();
                }

                for (LotteryInfo item : postfixList) {
                    if (item.getLotNo() > 0) {
                        quantityPostfixOther += item.getTotal();
                    }
                }

                for (LotteryInfo item : prefixList) {
                    if (item.getLotNo() > 0) {
                        quantityPrefixOther += item.getTotal();
                    }
                }

                r.setLotNumber1(String.valueOf(quantityPostfixSpecial));
                r.setLotNumber2(String.valueOf(quantityPrefixSpecial));
                r.setLotNumber3(String.valueOf(quantityPostfixOther));
                r.setLotNumber4(String.valueOf(quantityPrefixOther));

                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object checkProvinceLottery(HashMap<String, String> params) {
        Result result = new Result();

        String sign = params.get("sign");
        int region = Helper.getInt(params.get("region"), 0);

        if (!Helper.isNull(sign) && region > 0) {
            List<String> listProvinceId = Arrays.asList(ConfigStack.getConfig("lottery", "province_ids_region_" + String.valueOf(region), "").split("-"));

            ProvinceInfo province = Helper.getProvinceFromSign(sign, db.getListProvince());
            if (province != null && listProvinceId.contains(String.valueOf(province.getProvinceId()))) {
                result.setId(String.valueOf(province.getProvinceId()));
                result.setDesc(province.getFileName());
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.NO_DATA_FOUND);
            }

        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }

        return result;
    }

    public Object getProvinceNameOfLottery(HashMap<String, String> params) {
        Result result = new Result();

        int provinceId = Helper.getInt(params.get("provinceId"), 0);

        if (provinceId > 0) {
            ProvinceInfo province = Helper.getProvinceById(provinceId, db.getListProvince());
            if (province != null) {
                result.setId(String.valueOf(province.getProvinceId()));
                result.setDesc(province.getFileName());
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.NO_DATA_FOUND);
            }

        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }

        return result;
    }

    public Object getDayOfWeekByDate(HashMap<String, String> params) {
        Result r = new Result();

        String inputDate = params.get("inputDate");
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        sdf.setLenient(false);

        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(inputDate));
            if (inputDate.length() != 8) {
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }
        } catch (Exception e) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }

        r.setIndex(String.valueOf(cal.get(Calendar.DAY_OF_WEEK)));
        r.setErrorCode(Constants.SUCCESS);

        return r;
    }

    public Object insertLotteryHis(HashMap<String, String> params) {
        Result r = new Result();

        String msisdn = params.get("msisdn");
        int provinceId = Helper.getInt(params.get("provinceId"), 0);
        int region = Helper.getInt(params.get("region"), 0);
        String channel = params.get("channel");
        int result = Helper.getInt(params.get("result"), LotteryHisDTO.RES_FAILED);
        int action = Helper.getInt(params.get("action"), 0);

        if (!Helper.isNull(msisdn) && provinceId > 0 && region > 0 && action > 0) {
            LotteryHisDTO his = new LotteryHisDTO();
            his.setMsisdn(msisdn);
            his.setChannel(channel);
            his.setAction(action);
            his.setProvinceId(provinceId);
            his.setRegion(region);
            his.setResult(result);

            int ret = db.insertLotteryHis(his);
            if (ret == 1) {
                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object getRatingOfLotteryInPublishDateByNumber(HashMap<String, String> params) {
        Result r = new Result();

        int region = Helper.getInt(params.get("region"), 0);
        int provinceId = Helper.getInt(params.get("provinceId"), 0);
        String publishDate = params.get("publishDate");
        String pairOfNumber = params.get("number");
        if (Helper.isNull(pairOfNumber)
                || pairOfNumber.length() != 2
                || Helper.getInt(pairOfNumber, -1) == -1) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }

        if (region > 0 && provinceId > 0 && !Helper.isNull(publishDate)) {
            ArrayList<LotteryInfo> postfixList = db.getQuantityPostfixOfLotteryDataByPublishDate(region, provinceId, pairOfNumber, publishDate);
            ArrayList<LotteryInfo> prefixList = db.getQuantityPrefixOfLotteryDataByPublishDate(region, provinceId, pairOfNumber, publishDate);
            if (postfixList != null
                    && prefixList != null) {
                int quantityPostfixSpecial = 0;
                int quantityPrefixSpecial = 0;
                int quantityPostfixOther = 0;
                int quantityPrefixOther = 0;

                if (postfixList.size() > 0 && postfixList.get(0).getLotNo() == 0) {
                    quantityPostfixSpecial = postfixList.get(0).getTotal();
                }

                if (prefixList.size() > 0 && prefixList.get(0).getLotNo() == 0) {
                    quantityPrefixSpecial = prefixList.get(0).getTotal();
                }

                for (LotteryInfo item : postfixList) {
                    if (item.getLotNo() > 0) {
                        quantityPostfixOther += item.getTotal();
                    }
                }

                for (LotteryInfo item : prefixList) {
                    if (item.getLotNo() > 0) {
                        quantityPrefixOther += item.getTotal();
                    }
                }

                r.setLotNumber1(String.valueOf(quantityPostfixSpecial));
                r.setLotNumber2(String.valueOf(quantityPrefixSpecial));
                r.setLotNumber3(String.valueOf(quantityPostfixOther));
                r.setLotNumber4(String.valueOf(quantityPrefixOther));

                r.setErrorCode(Constants.SUCCESS);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }
}
