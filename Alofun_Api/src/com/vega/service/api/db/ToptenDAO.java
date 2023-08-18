package com.vega.service.api.db;

import com.vega.service.api.object.TopTenHistoryInfo;
import com.vega.service.api.object.TopTenInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import com.vega.vcs.service.database.pool.DbConnection;
import javax.naming.NamingException;

/**
 * @author SP1/ManND
 *
 */
public class ToptenDAO extends DBConnections {

   public boolean start() throws NamingException {
        return super.start();
    }

    public List<TopTenHistoryInfo> getListenHistory(String msisdn, int noDay) {
        List<TopTenHistoryInfo> returnVal = new LinkedList<TopTenHistoryInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select * from topten_listen_history where msisdn = ? and created_date >= (NOW() - ?)";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, noDay);

            rs = stmt.executeQuery();
            while (rs.next()) {
                TopTenHistoryInfo his = new TopTenHistoryInfo();
                his.setId(rs.getInt("id"));
                his.setToptenRecordId(rs.getInt("topten_record_id"));
                his.setMsisdn(rs.getString("msisdn"));
                his.setOrderInUserList(rs.getInt("order_in_user_list"));
                his.setSubPackageId(rs.getInt("sub_package_id"));
                his.setPackageId(rs.getInt("package_id"));
                his.setDuration(rs.getInt("duration"));
                his.setCreated_time(rs.getTimestamp("created_date"));
                returnVal.add(his);
            }
        } catch (Exception e) {
            logger.error("error in getListenHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return returnVal;
    }

    /**
     * Lay danh sach cac bản ghi topten chưa nghe trong vòng {@code noDay} ngày
     * Theo thu tu. Ban ghi moi (theo publish_date) xep truoc, ban ghi nghe gan
     * dau xep duoi
     *
     * @param msisdn
     * @param noDay
     * @param limit
     * @return Danh sach theo thứ tự của {@code msisdn}
     */
    public List<TopTenInfo> getToptenListOfUser(String msisdn, int noDay, int limit) {
        List<TopTenInfo> returnVal = new LinkedList<TopTenInfo>();
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        /*

         SELECT * FROM (
         SELECT 	r.*, 
         (CASE WHEN h.TOPTEN_RECORD_ID IS NULL THEN 1 ELSE 0 END ) AS NEWRECORD,
         h.START_DATE
         FROM TOPTEN_RECORDS r LEFT JOIN (
         SELECT TOPTEN_RECORD_ID, MAX(START_DATE) as START_DATE
         FROM TOPTEN_LISTEN_HISTORY
         WHERE START_DATE >= SYSDATE - 30 AND MSISDN = '84988000555'
         GROUP BY TOPTEN_RECORD_ID
         ) h on r.ID = h.TOPTEN_RECORD_ID
         WHERE r.PUBLISH_DATE <= SYSDATE AND r.STATUS = 1 and r.DELETED = 0
			
         )
         WHERE ROWNUM <= 10
         ORDER BY NEWRECORD desc, START_DATE asc, publish_date desc;
         */
        String sql = "select * from \n" +
                "( select r.*,  \n" +
                "   (case when h.topten_record_id is null then 1 else 0 end ) as newrecord, \n" +
                "    h.start_date from topten_records r left join \n" +
                "    (select topten_record_id, max(start_date) as start_date from topten_listen_history \n" +
                "      where now() >= now() - ? and msisdn = ? group by topten_record_id ) \n" +
                "      h on r.id = h.topten_record_id \n" +
                "      where r.publish_date <= now() and r.status = 1 and r.deleted = 0 ) a \n" +
                " order by newrecord desc, start_date asc, publish_date desc limit 0,?";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(1, noDay);
            stmt.setString(2, msisdn);
            stmt.setInt(3, limit);
            rs = stmt.executeQuery();
            while (rs.next()) {
                TopTenInfo his = new TopTenInfo();
                his.setId(rs.getInt("id"));
                his.setContentPath(rs.getString("content_path"));
                his.setContentName(rs.getString("content_name"));
                his.setStatus(rs.getInt("status"));
                his.setDeleted(rs.getInt("deleted"));
                his.setPublishedDate(rs.getTimestamp("publish_date"));
                his.setCreatedDate(rs.getTime("created_date"));
                his.setCreatedId(rs.getInt("created_id"));
                his.setUpdatedDate(rs.getTimestamp("updated_date"));
                his.setUpdatedId(rs.getInt("updated_id"));
                his.setNewRecord(rs.getInt("newrecord") == 1);
                returnVal.add(his);
            }
        } catch (Exception e) {
            logger.error("error in getToptenListOfUser in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return returnVal;
    }

    /**
     * @param msisdn
     * @param nDays
     */
    public int getCountListendInNDays(String msisdn, int nDays) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "select count(id) as countlisteninndays from topten_listen_history where msisdn = ? and start_date >= DATE_ADD(NOW(), INTERVAL ? DAY)";
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(1, msisdn);
            stmt.setInt(2, nDays);
            logger.info(">> Dao getCountListendInNDays :"+stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                int countListen = rs.getInt("countListenInNDays");
                return countListen;
            }
        } catch (Exception e) {
            logger.error("error in getCountListendInNDays in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return 0;
    }

    /**
     * Update lich su nghe topten
     *
     * @param item
     * @return
     */
    public int updateListenHistory(TopTenHistoryInfo item) {
        DbConnection cnn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String sql = "insert into topten_listen_history(topten_record_id, msisdn, sub_package_id, package_id, duration, order_in_user_list, start_date) values (?, ?, ?, ?, ?, ?, ?)";

        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            int i = 0;
            stmt.setInt(++i, item.getToptenRecordId());
            stmt.setString(++i, item.getMsisdn());
            stmt.setInt(++i, item.getSubPackageId());
            stmt.setInt(++i, item.getPackageId());
            stmt.setInt(++i, item.getDuration());
            stmt.setInt(++i, item.getOrderInUserList());
            stmt.setTimestamp(++i, new Timestamp(item.getStart_date().getTime()));
            int executeUpdate = stmt.executeUpdate();
            cnn.getConnection().commit();
            return executeUpdate;
        } catch (Exception e) {
            logger.error("error in updateListenHistory in database", e);
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }
        return -1;
    }
}
