/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api;

import java.util.HashMap;
import com.vega.service.api.response.Result;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.ProvinceInfo;
import java.text.SimpleDateFormat;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.WeatherDao;
import com.vega.service.api.object.WeatherObj;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author N.Tuyen
 */
public class WeatherStack {

    static Logger logger = Logger.getLogger(WeatherStack.class);
    WeatherDao db;

    public WeatherStack() throws NamingException {
        this.db = new WeatherDao();
        db.start();
    }

    /**
     * lay noi dung thoi tiet theo khu vuc
     * @param params
     * @return
     */
    public Object getDataWeatherByProvince(HashMap<String, String> params) {
        logger.info(" >>>> getDataWeatherByProvince :");
        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        int region = Helper.getInt(params.get("region_id"), 0);
        int type = Helper.getInt(params.get("type"), 0);
        String publishDate = "";
        publishDate = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        logger.debug("publishDate: " + publishDate);
        WeatherObj wObj = new WeatherObj();
        int provinceId = 0;
        int regionWeather = 0;
        if (type > 0 && region > 0) {
            if (type == 1) {
                // lay id mien theo id tinh
                provinceId = region;
                regionWeather = 0;
                region = db.getWeatherRegion(region);
            } else {
                provinceId = 0;
                regionWeather = region;
            }
            wObj = db.getDataWeatherByProvince(region, publishDate);

            int rs = db.insertSubRegion(msisdn, provinceId, regionWeather);
            logger.info("rs insertSubRegion: " + rs);
        } else {
            wObj.setErrorCode(Constants.WRONG_PARAM);
        }
        return wObj;
    }

    /**
     * check khu vuc nghe gan nhat
     * @param params
     * @return
     */
    public Object checkRegionListened(HashMap<String, String> params) {
        logger.info("checkRegionListened :" + params.get("msisdn"));
        Result result = new Result();
        Result rs2 = new Result();
        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        if (!Helper.isNull(msisdn)) {
            result = db.checkRegionListened(msisdn);
            logger.info("checkRegionListened result:" + result.getErrorCode());
            if (result.getErrorCode().equals(Constants.SUCCESS)) {
                if (!result.getRegion().equals("0")) {
                    int provinceId = Integer.valueOf(result.getRegion());
                    rs2.setType("1");
                    rs2.setRegion_id(result.getRegion());
                    ArrayList<ProvinceInfo> arrProvince = new ArrayList<ProvinceInfo>();
                    arrProvince = db.getListProvince();
                    for (int i = 0; i < arrProvince.size(); i++) {
                        if (provinceId == arrProvince.get(i).getProvinceId()) {
                            rs2.setName(arrProvince.get(i).getFileName());
                        }
                    }
                } else if (!result.getRegion_weather().equals("0")) {
                    rs2.setType("2");
                    rs2.setRegion_id(result.getRegion_weather());
                }
                rs2.setErrorCode(Constants.SUCCESS);
            } else {
                rs2.setErrorCode(Constants.NO_DATA_FOUND);
            }
        } else {
            rs2.setErrorCode(Constants.WRONG_PARAM);
        }
        return rs2;
    }

    /**
     * check nguoi dung da hen gio call out chua 
     * @param params
     * @return
     */
    public Object checkSetupCallOut(HashMap<String, String> params) {
        logger.info("checkSetupCallOut :" + params.get("msisdn"));
        Result result = new Result();
        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        if (!Helper.isNull(msisdn)) {
            result = db.checkSetupCallOut(msisdn);
        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }
        return result;
    }

    /**
     * update lich su thao tac ngươi dung
     * @param params
     * @return
     */
    public Object updateHistoryContentWeather(HashMap<String, String> params) {
        logger.info(">>>>> updateHistoryContent ");
        Result r = new Result();

        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        int duration = Helper.getInt(params.get("duration"), 0);
        int weather_content_id = Helper.getInt(params.get("weather_content_id"), 0);
        int action = Helper.getInt(params.get("action"), 0);
        String source = params.get("source");
        if (Helper.isNull(source)) {
            source = "IVR";
        }
        int weatherRegion = Helper.getInt(params.get("weather_region"), 0);
        int provinceId = Helper.getInt(params.get("province_id"), 0);
        if (!Helper.isNull(msisdn)) {
            WeatherObj weatherObj = new WeatherObj();
            weatherObj.setMsisdn(msisdn);
            weatherObj.setWeather_content_id(weather_content_id);
            weatherObj.setAction(action);
            weatherObj.setSource(source);
            weatherObj.setWeather_region(weatherRegion);
            weatherObj.setProvince_id(provinceId);
            weatherObj.setDuration(duration);
            int ret = db.insertWeatherHis(weatherObj);
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

    /**
     * Dat thoi gian goi ra cho nguoi dung nghe thoi tiet
     * @param params
     * @return
     */
    public Object setTimeCallout(HashMap<String, String> params) {
        logger.info(">>>>> setTimeCallout ");
        Result r = new Result();

        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        int province_id = Helper.getInt(params.get("province_id"), 0);
        int region_weather = Helper.getInt(params.get("region_weather"), 0);
        int type = Helper.getInt(params.get("type"), 0);
        String time_call = params.get("time_call");
        int status = Integer.valueOf(params.get("status"));
        if (!Helper.isNull(msisdn) && !Helper.isNull(time_call) && type > 0) {
            int ret = db.insertWeatherCallOut(msisdn, time_call, province_id, region_weather, status);
            logger.info(">>>>> setTimeCallout ret :" + ret);
            if (ret == 1) {
                HashMap<String, String> paramsReject = new HashMap<String, String>();
                paramsReject.put("msisdn", msisdn);
                paramsReject.put("status", "0");
                r = (Result) rejectWeather(paramsReject);
            } else {
                r.setErrorCode(Constants.SYSTEM_ERROR);
            }
        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public Object updateStatusCallOutWeather(HashMap<String, String> params) {
        logger.info(">>>>> updateStatusCallOutWeather ");
        Result r = new Result();

        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        int status = Helper.getInt(params.get("status"), 0);
        if (!Helper.isNull(msisdn)) {
            int ret = db.updateStatusWeatherCallOut(msisdn, status);
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

    /**
     * Tu choi / bo tu choi nghe ban tin thoi tiet
     * @param params
     * @return
     */
    public Object rejectWeather(HashMap<String, String> params) {
        logger.info(">>>>> rejectWeather " + params.get("msisdn"));
        Result r = new Result();

        String msisdn = Helper.formatMobileNumber(params.get("msisdn"));
        int status = Integer.valueOf(params.get("status"));
        if (!Helper.isNull(msisdn)) {
            int ret = db.insertRejectWeather(msisdn, status);
            logger.info(">>>>> rejectWeather ret :" + ret);
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

    public Object checkProvinceWeather(HashMap<String, String> params) {
        Result result = new Result();
        String sign = params.get("sign");
        logger.info(" >>>>>>>>>>>>>>> checkProvinceWeather" + sign);
        if (!Helper.isNull(sign)) {
            ProvinceInfo province = Helper.getProvinceFromSign(sign, db.getListProvince());
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
}
