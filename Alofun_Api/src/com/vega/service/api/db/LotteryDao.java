/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.db;

import com.vega.service.api.object.LotteryHisDTO;
import com.vega.service.api.object.LotteryInfo;
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
public class LotteryDao extends DBConnections {

    static transient Logger logger = Logger.getLogger(LotteryDao.class);
    // DAO xo so

    public boolean start() throws NamingException {
        return super.start();
    }

    public ArrayList<LotteryInfo> getLotteryDataByDate(int region, int provinceId, String publishDate) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * "
                + "from lottery "
                + "where province_id = ? "
                + "and region = ? "
                + "and publish_date =  str_to_date(?, '%d%m%Y') "
                + "order by lot_no, ord ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setString(++i, publishDate);
            logger.debug(stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setId(rs.getInt("id"));
                item.setVal(rs.getString("val"));
                item.setPrefixVal(rs.getString("prefix_val"));
                item.setPostfixVal(rs.getString("postfix_val"));
                item.setOrd(rs.getInt("ord"));
                item.setLotNo(rs.getInt("lot_no"));

                list.add(item);
            }
        } catch (Exception e) {
            logger.error("error in getLotteryDataByDate from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public int getLotteryRegionOfSub(String msisdn) {
        DbConnection cnn = null;
        int region = 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select * from lottery_sub_region where msisdn = ?";

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            rs = stmt.executeQuery();
            if (rs.next()) {
                region = rs.getInt("region");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            region = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return region;
    }

    public int updateLotteryRegionOfSub(String msisdn, int region) {
        DbConnection cnn = null;
        int result = -1;
        PreparedStatement stmt = null;
        PreparedStatement stmt_up = null;
        ResultSet rs = null;
        String sql = "select count(*) total from lottery_sub_region where msisdn = ?";
        int count = 0;

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }

            if (count == 0) {
                sql = "insert into lottery_sub_region(region, msisdn) values(?,?)";
            } else {
                sql = "update lottery_sub_region set region = ?, updated_date = now() where msisdn = ?";
            }
            stmt_up = cnn.getConnection().prepareStatement(sql);
            stmt_up.setInt(1, region);
            stmt_up.setString(2, msisdn);
            stmt_up.executeUpdate();

            cnn.getConnection().commit();
            result = 0;
        } catch (Exception e) {
            logger.error(e.getMessage());
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    public int updateLotteryCalloutStatusOfSub(String msisdn, int region, int provinceId, int status, String calloutDate, String source) {
        DbConnection cnn = null;
        int result = -1;
        PreparedStatement stmt = null;
        PreparedStatement stmt_up = null;
        ResultSet rs = null;
        String sql = "select count(*) total from lottery_sub_callout where msisdn = ? and province_id = ?";
        int count = 0;
        int i = 0;

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, provinceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }

            if (count == 0) {
                sql = "insert into lottery_sub_callout(msisdn, region, province_id, callout_status, callout_date, source) values(?,?,?,?,str_to_date(?, '%d%m%Y'),?)";
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setString(++i, msisdn);
                stmt_up.setInt(++i, region);
                stmt_up.setInt(++i, provinceId);
                stmt_up.setInt(++i, status);
                stmt_up.setString(++i, calloutDate);
                stmt_up.setString(++i, source);
            } else {
                sql = "update lottery_sub_callout set callout_status = ?, callout_date = str_to_date(?, '%d%m%Y'), source = ?, updated_date = now() where msisdn = ? and province_id = ?";
                stmt_up = cnn.getConnection().prepareStatement(sql);
                stmt_up.setInt(++i, status);
                stmt_up.setString(++i, calloutDate);
                stmt_up.setString(++i, source);
                stmt_up.setString(++i, msisdn);
                stmt_up.setInt(++i, provinceId);
            }

            stmt_up.executeUpdate();
            cnn.getConnection().commit();
            result = 0;
        } catch (Exception e) {
            logger.error(e.getMessage());
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_up);
            freeConnection(cnn);
        }

        return result;
    }

    public int getStatusCalloutOfSub(String msisdn, int provinceId) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select callout_status from lottery_sub_callout where msisdn = ? and province_id = ?";
        int status = 0;

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, provinceId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                status = rs.getInt("callout_status");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            rollbackTransaction(cnn);
            status = -1;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return status;
    }

    public ArrayList<LotteryInfo> getMinAndMaxRatingPostfixOfLotteryData(int region, int provinceId, int dateRange) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "("
                + "  select count(*) total, postfix_val val "
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and publish_date >= DATE_FORMAT(SUBDATE(now(), INTERVAL ? DAY),'%Y-%m-%d') "
                + "  group by postfix_val "
                + "  order by total desc "
                + "  limit 1) "
                + " union all "
                + "  ( "
                + "  select count(*) total, postfix_val val"
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and publish_date >= DATE_FORMAT(SUBDATE(now(), INTERVAL ? DAY),'%Y-%m-%d') "
                + "  group by postfix_val "
                + "  order by total "
                + " limit 1) ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setInt(++i, dateRange);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setInt(++i, dateRange);
            logger.debug(stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setPostfixVal(rs.getString("val"));

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getMinAndMaxRatingPostfixOfLotteryData", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<LotteryInfo> getMinAndMaxRatingPrefixOfLotteryData(int region, int provinceId, int dateRange) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "( "
                + "  select count(*) total, prefix_val val"
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and publish_date >= DATE_FORMAT(SUBDATE(now(), INTERVAL ? DAY),'%Y-%m-%d') "
                + "  group by prefix_val "
                + "  order by total desc "
                + "  limit 1) "
                + "  union all "
                + "  ( "
                + "  select count(*) total, prefix_val  val"
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and publish_date >= DATE_FORMAT(SUBDATE(now(), INTERVAL ? DAY),'%Y-%m-%d') "
                + "  group by prefix_val "
                + "  order by total "
                + "  limit 1) ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setInt(++i, dateRange);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setInt(++i, dateRange);
            logger.debug(stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setPostfixVal(rs.getString("val"));

                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getMinAndMaxRatingPrefixOfLotteryData", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<LotteryInfo> getQuantityPostfixOfLotteryData(int region, int provinceId, String postfix, int dateRange) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select lot_no, count(*) total "
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and postfix_val = ? "
                + "  and publish_date >= DATE_FORMAT(SUBDATE(now(), INTERVAL ? DAY),'%Y-%m-%d') "
                + "  group by lot_no "
                + "  order by lot_no ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setString(++i, postfix);
            stmt.setInt(++i, dateRange);
            logger.debug(stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setLotNo(rs.getInt("lot_no"));
                item.setTotal(rs.getInt("total"));
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getQuantityPostfixOfLotteryData", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<LotteryInfo> getQuantityPrefixOfLotteryData(int region, int provinceId, String prefix, int dateRange) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select lot_no, count(*) total "
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and prefix_val = ? "
                + "  and publish_date >= DATE_FORMAT(SUBDATE(now(), INTERVAL ? DAY),'%Y-%m-%d') "
                + "  group by lot_no "
                + "  order by lot_no ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setString(++i, prefix);
            stmt.setInt(++i, dateRange);
            logger.debug(stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setLotNo(rs.getInt("lot_no"));
                item.setTotal(rs.getInt("total"));
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getQuantityPrefixOfLotteryData", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public int insertLotteryHis(LotteryHisDTO item) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        int ret = 0;
        String sql = "insert into lottery_his(msisdn, action, province_id, channel, region, result) values(?, ?, ?, ?, ?, ?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, item.getMsisdn());
            stmt.setInt(++i, item.getAction());
            stmt.setInt(++i, item.getProvinceId());
            stmt.setString(++i, item.getChannel());
            stmt.setInt(++i, item.getRegion());
            stmt.setInt(++i, item.getResult());
            stmt.executeUpdate();

            cnn.getConnection().commit();
            ret = 1;
        } catch (Exception e) {
            logger.error("Exception in insertLotteryHis", e);
            logger.trace(e);
            ret = -1;
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return ret;
    }

    public ArrayList<LotteryInfo> getQuantityPostfixOfLotteryDataByPublishDate(int region, int provinceId, String postfix, String publishDate) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select lot_no, count(*) total "
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and postfix_val = ? "
                + "  and publish_date = str_to_date(?, '%d%m%Y') "
                + "  group by lot_no "
                + "  order by lot_no ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setString(++i, postfix);
            stmt.setString(++i, publishDate);

            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setLotNo(rs.getInt("lot_no"));
                item.setTotal(rs.getInt("total"));
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getQuantityPostfixOfLotteryDataByPublishDate", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public ArrayList<LotteryInfo> getQuantityPrefixOfLotteryDataByPublishDate(int region, int provinceId, String prefix, String publishDate) {
        ArrayList<LotteryInfo> list = new ArrayList<LotteryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select lot_no, count(*) total "
                + "  from lottery "
                + "  where province_id = ? "
                + "  and region = ? "
                + "  and prefix_val = ? "
                + "  and publish_date = str_to_date(?, '%d%m%Y') "
                + "  group by lot_no "
                + "  order by lot_no ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, provinceId);
            stmt.setInt(++i, region);
            stmt.setString(++i, prefix);
            stmt.setString(++i, publishDate);

            rs = stmt.executeQuery();
            while (rs.next()) {
                LotteryInfo item = new LotteryInfo();
                item.setLotNo(rs.getInt("lot_no"));
                item.setTotal(rs.getInt("total"));
                list.add(item);
            }

        } catch (Exception e) {
            logger.error("error in getQuantityPrefixOfLotteryDataByPublishDate", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }
}
