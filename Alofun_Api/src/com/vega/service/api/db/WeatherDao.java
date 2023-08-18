/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import com.vega.service.api.common.ProvinceInfo;
import com.vega.service.api.object.WeatherObj;
import com.vega.service.api.response.Result;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 *
 * @author N.Tuyen
 */
public class WeatherDao extends DBConnections {

    static transient Logger logger = Logger.getLogger(WeatherDao.class);

    public boolean start() throws NamingException {
        return super.start();
    }

    public WeatherObj getDataWeatherByProvince(int region_weather, String publishDate) {
        logger.info(" >>>> getDataWeatherByProvince");
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        WeatherObj item = new WeatherObj();
        String sql = "select * "
                + " from weather_content "
                + " where region_weather = ? "
                + " and status = 1 "
                + " and str_to_date(publish_date, '%Y-%m-%d') <= str_to_date(?, '%Y-%m-%d')"
                + " and publish_date < now()"
                + " order by publish_date desc limit 1 ";
        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, region_weather);
            stmt.setString(++i, publishDate);
            logger.debug(stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                item.setErrorCode(Constants.SUCCESS);
                item.setWeather_content_id(rs.getInt("weather_content_id"));
                item.setContent_sms(rs.getString("content_sms"));
                item.setContent_name(rs.getString("content_name"));
                item.setContent_path(rs.getString("content_path"));
                item.setWeather_region(rs.getInt("region_weather"));
            } else {
                item.setErrorCode(Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("error in getDataWeatherByProvince from database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return item;
    }

    public int getWeatherRegion(int provinceId) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select region_weather from province where province_id = ?";
        int region_weather = 0;

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, provinceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                region_weather = rs.getInt("region_weather");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            rollbackTransaction(cnn);
            region_weather = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return region_weather;
    }

    public int insertWeatherHis(WeatherObj item) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        int ret = 0;
        String sql = "insert into weather_history(msisdn, weather_content_id, action, source, weather_region, province_id,duration) "
                + " values(?, ?, ?, ?, ?, ?,?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, item.getMsisdn());
            stmt.setInt(++i, item.getWeather_content_id());
            stmt.setInt(++i, item.getAction());
            stmt.setString(++i, item.getSource());
            stmt.setInt(++i, item.getWeather_region());
            stmt.setInt(++i, item.getProvince_id());
            stmt.setInt(++i, item.getDuration());

            stmt.executeUpdate();

            cnn.getConnection().commit();
            ret = 1;
        } catch (Exception e) {
            logger.error("Exception in insertWeatherHis", e);
            logger.trace(e);
            ret = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return ret;
    }

    public int insertRejectWeather(String msisdn, int status) {
        logger.info(">>>>> rejectWeatherDao " + msisdn);
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        int ret = -1;
        String sql_check_exit = "select * from weather_reject where msisdn =? and status = 1";
        String sql = "insert into weather_reject(msisdn, status) "
                + "values(?,?) ON DUPLICATE KEY UPDATE status = ?";
        int i = 0;
        try {
            cnn = getConnection();
            if (status == 1) {
                stmt1 = cnn.getConnection().prepareStatement(sql_check_exit);
                stmt1.setString(++i, msisdn);
                rs = stmt1.executeQuery();
                if (rs.next()) {
                    ret = 2;
                    return ret;
                }
            }
            i = 0;
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, status);
            stmt.setInt(++i, status);
            stmt.executeUpdate();
            cnn.getConnection().commit();
            ret = 1;
        } catch (Exception e) {
            logger.error("Exception in insertRejectWeather", e);
            ret = -1;
            logger.trace(e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return ret;
    }

    public int insertSubRegion(String msisdn, int provinceId, int regionWeather) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int ret = -1;
        logger.info("insertSubRegion >>> " + msisdn);
        String sql_insert_update = "insert into weather_sub_region(msisdn, province_id,region_weather) "
                + "values(?,?,?) ON DUPLICATE KEY UPDATE province_id =?,region_weather=?";
        int i = 0;
        try {
            i = 0;
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql_insert_update);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, regionWeather);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, regionWeather);
            stmt.executeUpdate();
            cnn.getConnection().commit();
            ret = 0;
        } catch (Exception e) {
            logger.error("Exception in insertSubRegion", e);
            logger.trace(e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);

            freeConnection(cnn);
        }

        return ret;
    }

    public Result checkRegionListened(String msisdn) {
        logger.info("checkRegionListened Dao:");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        Result result = new Result();
        String sql = "select province_id, region_weather from weather_sub_region where msisdn =? ";
        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            logger.info("checkRegionListened Dao:" + stmt1.toString());
            rs = stmt1.executeQuery();
            if (rs.next()) {
                result.setRegion(rs.getString("province_id"));
                result.setRegion_weather(rs.getString("region_weather"));
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.NO_DATA_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error in checkRegionListened Dao", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }
        logger.info("CLGT : " + result.getErrorCode());
        return result;
    }

    public int insertWeatherCallOut(String msisdn, String time_call, int province_id, int region_weather, int status) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        int ret = 0;
        String sql = "insert into weather_callout(msisdn,call_date, province_id, region_weather, status) "
                + " values(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE call_date =? ,status =?  ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);
            stmt.setString(++i, time_call);
            stmt.setInt(++i, province_id);
            stmt.setInt(++i, region_weather);
            stmt.setInt(++i, status);
            stmt.setString(++i, time_call);
            stmt.setInt(++i, status);
            stmt.executeUpdate();

            cnn.getConnection().commit();
            ret = 1;
        } catch (Exception e) {
            logger.error("Exception in insertWeatherCallOut", e);
            logger.trace(e);
            ret = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return ret;
    }

    public int updateStatusWeatherCallOut(String msisdn, int status) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        int ret = 0;
        String sql = "update weather_callout set status = ? where msisdn =?";
        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, status);
            stmt.setString(++i, msisdn);
            stmt.executeUpdate();
            cnn.getConnection().commit();
            ret = 1;
        } catch (Exception e) {
            logger.error("Exception in updateStatusWeatherCallOut", e);
            logger.trace(e);
            ret = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return ret;
    }

    public Result checkSetupCallOut(String msisdn) {
        logger.info("checkSetupCallOut Dao:");
        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        Result result = new Result();
        String sql = "select count(msisdn) as total  from weather_callout where msisdn =? and status =1";
        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setString(1, msisdn);
            logger.info("checkSetupCallOut Dao:" + stmt1.toString());
            rs = stmt1.executeQuery();
            if (rs.next()) {
                if (rs.getInt("total") > 0) {
                    result.setErrorCode(Constants.SUCCESS);
                } else {
                    result.setErrorCode(Constants.NO_DATA_FOUND);
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkRegionListened Dao", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return result;
    }

    public ArrayList<ProvinceInfo> getListProvince() {
        ArrayList<ProvinceInfo> list = new ArrayList<ProvinceInfo>();

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select province_id, name, identify_sign, file_name, region from province "
                + " where status = ? ";

        try {
            cnn = getConnection();
            stmt1 = cnn.getConnection().prepareStatement(sql);
            stmt1.setInt(1, Constants.PROFILE_STATUS_ACTIVE);
            logger.info("AAAAAA:" + stmt1.toString());
            rs = stmt1.executeQuery();
            while (rs.next()) {
                ProvinceInfo p = new ProvinceInfo();
                p.setName(rs.getString("name"));
                p.setProvinceId(rs.getInt("province_id"));
                p.setIdentifySign(rs.getString("identify_sign"));
                p.setFileName(rs.getString("file_name"));
                p.setRegion(rs.getInt("region"));

                list.add(p);
            }

        } catch (Exception e) {
            logger.error("Error in getListProvince weather", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }
}
