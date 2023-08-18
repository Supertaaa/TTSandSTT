package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import com.vega.service.api.common.GiftAccountInfo;
import com.vega.service.api.common.GiftContentInfo;
import com.vega.service.api.common.Song;

import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.SQLException;

public class GiftDAO extends DBConnections {

    public void init() throws Exception {
        super.start();
    }

    public GiftAccountInfo getGiftAccount(String msisdn, boolean createNewIfNotExist, int maxFreeCount, int subPackageId, String subExpire) {
        GiftAccountInfo account = new GiftAccountInfo();

        PreparedStatement stmt = null;
        PreparedStatement stmt_ins = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from gift_account where msisdn = ?";
        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            if (rs.next()) {
                account.setMsisdn(msisdn);
                account.setSubPackageId(rs.getInt("sub_package_id"));
                account.setStatus(rs.getInt("status"));
                account.setFreeCount(rs.getInt("free_count"));
                account.setBlackList(rs.getString("black_list"));
                account.setWhiteList(rs.getString("white_list"));
                account.setSubExpireAt(rs.getString("sub_expire_at"));
            } else if (createNewIfNotExist) {
                i = 0;
                int status = GiftAccountInfo.STATUS_RECEIVED;
                sql = "insert into gift_account(msisdn,sub_package_id,sub_expire_at, free_count, status) values(?,?,?, ?, ?) ";
                stmt_ins = cnn.getConnection().prepareStatement(sql);
                stmt_ins.setString(++i, msisdn);
                stmt_ins.setInt(++i, subPackageId);
                stmt_ins.setString(++i, subExpire);
                stmt_ins.setInt(++i, maxFreeCount);
                stmt_ins.setInt(++i, status);
                stmt_ins.executeUpdate();
                cnn.getConnection().commit();

                account.setMsisdn(msisdn);
                account.setSubPackageId(subPackageId);
                account.setSubExpireAt(subExpire);
                account.setStatus(status);
                account.setFreeCount(maxFreeCount);
            }
        } catch (Exception e) {
            logger.error("error in getGiftAccount in database", e);
            account = null;
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_ins);
            freeConnection(cnn);
        }

        return account;
    }
    /*
     * ham update số quà tặng
     */

    public GiftAccountInfo updateFreeCount(String msisdn, int subPackageId, String subExpire, int maxFreeCount) {
        GiftAccountInfo account = new GiftAccountInfo();

        PreparedStatement stmt = null;
        PreparedStatement stmt_update = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from gift_account where msisdn = ?";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            if (rs.next()) {
                i = 0;
                int status = GiftAccountInfo.STATUS_RECEIVED;
                sql = "update gift_account set free_count = ?,sub_package_id= ?,sub_expire_at = ? where msisdn =?";
                stmt_update = cnn.getConnection().prepareStatement(sql);

                stmt_update.setInt(++i, maxFreeCount);
                stmt_update.setInt(++i, subPackageId);
                stmt_update.setString(++i, subExpire);
                stmt_update.setString(++i, msisdn);
                stmt_update.executeUpdate();
                cnn.getConnection().commit();

                account.setMsisdn(msisdn);
                account.setSubPackageId(subPackageId);
                account.setSubExpireAt(subExpire);
                account.setStatus(status);
                account.setFreeCount(maxFreeCount);
            }
        } catch (Exception e) {
            logger.error("error in updateFreeCount in database", e);
            account = null;
            rollbackTransaction(cnn);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_update);
            freeConnection(cnn);
        }

        return account;
    }
    /*
     * Hàm check xem có đc update quà tặng theo chu ký cước không
     */

    public boolean checkUpdateGiftAccount(String msisdn, int packageId, int subPackageId) {
        logger.info(">>>>>> GiftDAO checkUpdateGiftAccount");
        boolean result = false;
        PreparedStatement stmt = null;
        PreparedStatement stmt_ins = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select a.msisdn,a.billing_at from ( "
                + " select msisdn , billing_at from billing_activity  "
                + " where msisdn = ? and package_id = ? and sub_package_id = ? "
                + " and result = '0' and amount > 0 "
                + " and (billing_type = 1 or billing_type = 3) "
                + " ) a inner join  ("
                + " select msisdn, update_gift from gift_account where msisdn = ?) b"
                + " on a.msisdn = b.msisdn "
                + " where a.billing_at > b.update_gift";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, packageId);
            stmt.setInt(++i, subPackageId);
            stmt.setString(++i, msisdn);

            rs = stmt.executeQuery();
            if (rs.next()) {
                result = true;
            }
        } catch (Exception e) {
            logger.error("error in checkUpdateGiftAccount in database", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeStatement(stmt_ins);
            freeConnection(cnn);
        }

        return result;
    }

    public String insertGiftContent(GiftContentInfo g) {
        logger.error(" VAO HAM INSERT GIFT");
        String resp = Constants.SYSTEM_ERROR;

        PreparedStatement stmt = null;
        PreparedStatement stmt_acc = null;
        DbConnection cnn = null;
        String sql = "insert into gift_content("
                + "gift_content_name, "
                + "sender, "
                + "receiver, "
                + "content_id, "
                + "content_code, "
                + "topic_type, "
                + "message_path, "
                + "send_mt_date, "
                + "call_date, "
                + "source, "
                + "fee,"
                + "telco,"
                + "to_telco,"
                + "audio_path) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, g.getGiftContentName());
            stmt.setString(++i, g.getSender());
            stmt.setString(++i, g.getReceiver());
            stmt.setInt(++i, g.getContentId());
            stmt.setString(++i, g.getContentCode());
            stmt.setInt(++i, g.getTopicType());
            stmt.setString(++i, g.getMessagePath());
            stmt.setTimestamp(++i, new Timestamp(g.getSendMTDate().getTimeInMillis()));
            stmt.setTimestamp(++i, new Timestamp(g.getCallDate().getTimeInMillis()));
            stmt.setInt(++i, 0);
            stmt.setInt(++i, g.getFee());
            stmt.setInt(++i, g.getTelco());
            stmt.setInt(++i, g.getToTelco());
            stmt.setString(++i, g.getAudioPath());
            stmt.executeUpdate();

            if (g.getFee() <= 0) {
                i = 0;
                sql = "update gift_account set free_count = free_count - 1, updated_date = now() where msisdn = ? and free_count > 0";
                stmt_acc = cnn.getConnection().prepareStatement(sql);
                stmt_acc.setString(++i, g.getSender());
                stmt_acc.executeUpdate();
            }

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in insertGiftContent into database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            closeStatement(stmt_acc);
            freeConnection(cnn);
        }

        return resp;
    }

    //lay danh sach nhac theo phan
    public ArrayList<GiftContentInfo> getListGiftMusicPart(String msisdn, boolean onlyNotReceive, int limit) {
        ArrayList<GiftContentInfo> list = new ArrayList<>();
        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;

        String sql = "select t3.* from("
                + "    select t2.path content_path, t1.*  from ("
                + "      select * "
                + "      from gift_content "
                + "      where receiver = ? "
                + "      and call_date <= now() "
                + "      and user_deleted = 0 "
                + "      and topic_type = ? ";
        if (onlyNotReceive) {
            sql += "and (status = ? or status = ?) ";
        } else {
            sql += "and (status = ? or status = ? or status = ?) ";
        }
        sql += ") t1 inner join ("
                + "      select music_content_id, path, part_number "
                + "      from music_content_part"
                + "      where status = ? "
                + "      and created_date <= now() "
                + "    ) t2 on t1.content_id = t2.music_content_id and t1.content_code = t2.part_number ";
        sql += ") t3 order by call_date desc, gift_content_id desc "
                + " limit ? ";

        try {
            int i = 0;
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sql);
            pstm.setString(++i, msisdn);
            pstm.setInt(++i, GiftContentInfo.TOPIC_MUSIC_PART);
            pstm.setInt(++i, GiftContentInfo.STATUS_WAITING);
            pstm.setInt(++i, GiftContentInfo.STATUS_RETRY);
            if (!onlyNotReceive) {
                pstm.setInt(++i, GiftContentInfo.STATUS_OK);
            }
            pstm.setInt(++i, Constants.STATUS_PUBLIC);
            pstm.setInt(++i, limit);
            rs = pstm.executeQuery();
            while (rs.next()) {
                GiftContentInfo item = new GiftContentInfo();
                item.setGiftContentId(rs.getInt("gift_content_id"));
                item.setGiftContentName(rs.getString("gift_content_name"));
                item.setSender(rs.getString("sender"));
                item.setReceiver(rs.getString("receiver"));
                item.setContentId(rs.getInt("content_id"));
                item.setContentCode(rs.getString("content_code"));
                item.setTopicType(rs.getInt("topic_type"));
                item.setMessagePath(rs.getString("message_path"));
                item.setStatus(rs.getInt("status"));
                item.setAudioPath(rs.getString("audio_path"));
                item.setContentPath(rs.getString("content_path"));
                item.setTelco(rs.getInt("telco"));
                item.setToTelco(rs.getInt("to_telco"));

                list.add(item);
            }
        } catch (SQLException sqlE) {
            String msg = "Sql Exception in function getListGiftMusicPart, error code : " + sqlE.getErrorCode();
            sqlE.printStackTrace();
            logger.error(msg, sqlE);
        } catch (Exception ex) {
            logger.error("Exception in fuction getListGiftMusicPart", ex);
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return list;
    }

    public ArrayList<GiftContentInfo> getListGiftContent(String msisdn, boolean onlyNotReceive, int limit) {
        ArrayList<GiftContentInfo> list = new ArrayList<GiftContentInfo>();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;

        String sql = "select t3.* from("
                + "    select t2.content_path content_path, t1.*  from ("
                + "      select * "
                + "      from gift_content "
                + "      where receiver = ? "
                + "      and call_date <= now() "
                + "      and user_deleted = 0 "
                + "      and topic_type = ? ";
        if (onlyNotReceive) {
            sql += "and (status = ? or status = ?) ";
        } else {
            sql += "and (status = ? or status = ? or status = ?) ";
        }
        sql += ") t1 inner join ("
                + "      select music_content_id, content_path "
                + "      from music_content "
                + "      where status = ? "
                + "      and publish_date <= now() "
                + "      and total_part = 1 "
                + "    ) t2 on t1.content_id = t2.music_content_id "
                + "  union all "
                + "    select audio_path content_path, gift_content.* "
                + "    from gift_content "
                + "    where receiver = ? "
                + "    and call_date <= now() "
                + "    and user_deleted = 0 "
                + "    and (topic_type = ? or topic_type = ?) ";
        if (onlyNotReceive) {
            sql += "and (status = ? or status = ?) ";
        } else {
            sql += "and (status = ? or status = ? or status = ?) ";
        }
        sql += ") t3 order by call_date desc, gift_content_id desc "
                + " limit ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setString(++i, msisdn);
            stmt.setInt(++i, GiftContentInfo.TOPIC_MUSIC);
            stmt.setInt(++i, GiftContentInfo.STATUS_WAITING);
            stmt.setInt(++i, GiftContentInfo.STATUS_RETRY);
            if (!onlyNotReceive) {
                stmt.setInt(++i, GiftContentInfo.STATUS_OK);
            }
            stmt.setInt(++i, Constants.STATUS_PUBLIC);

            stmt.setString(++i, msisdn);
            stmt.setInt(++i, GiftContentInfo.TOPIC_IDOL);
            stmt.setInt(++i, GiftContentInfo.TOPIC_STUDIO);
            stmt.setInt(++i, GiftContentInfo.STATUS_WAITING);
            stmt.setInt(++i, GiftContentInfo.STATUS_RETRY);
            if (!onlyNotReceive) {
                stmt.setInt(++i, GiftContentInfo.STATUS_OK);
            }
            stmt.setInt(++i, limit);

            rs = stmt.executeQuery();
            while (rs.next()) {
                GiftContentInfo item = new GiftContentInfo();
                item.setGiftContentId(rs.getInt("gift_content_id"));
                item.setGiftContentName(rs.getString("gift_content_name"));
                item.setSender(rs.getString("sender"));
                item.setReceiver(rs.getString("receiver"));
                item.setContentId(rs.getInt("content_id"));
                item.setContentCode(rs.getString("content_code"));
                item.setTopicType(rs.getInt("topic_type"));
                item.setMessagePath(rs.getString("message_path"));
                item.setStatus(rs.getInt("status"));
                item.setAudioPath(rs.getString("audio_path"));
                item.setContentPath(rs.getString("content_path"));
                item.setTelco(rs.getInt("telco"));
                item.setToTelco(rs.getInt("to_telco"));

                list.add(item);
            }
            ArrayList<GiftContentInfo> listGiftMusicPart = getListGiftMusicPart(msisdn, onlyNotReceive, limit);
            if (listGiftMusicPart.size() > 0) {
                     list.addAll(listGiftMusicPart);
            }
        } catch (Exception e) {
            logger.error("error in getListGiftContent from database", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return list;
    }

    public String updateListenedGiftContent(int giftContentId, int listenStatus, int duration, int channel, int telco) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update gift_content "
                + "set status = ?, "
                + "updated_date = NOW(), "
                + "channel_received = ?, "
                + "listen_gift_status = ?, "
                + "listen_gift_duration = ? "
                + "where gift_content_id = ? and telco = ? "
                + "and status != ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, GiftContentInfo.STATUS_OK);
            stmt.setInt(++i, channel);
            stmt.setInt(++i, listenStatus);
            stmt.setInt(++i, duration);
            stmt.setInt(++i, giftContentId);
            stmt.setInt(++i, telco);
            stmt.setInt(++i, GiftContentInfo.STATUS_OK);
            stmt.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in updateListenedGiftContent into database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    public String removeGiftContent(int giftContentId, String msisdn, int telco) {
        String resp = Constants.SYSTEM_ERROR;
        PreparedStatement stmt = null;
        DbConnection cnn = null;
        String sql = "update gift_content "
                + "set user_deleted = 1 "
                + "where gift_content_id = ? and telco = ? "
                + "and receiver = ? ";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, giftContentId);
            stmt.setInt(++i, telco);
            stmt.setString(++i, msisdn);
            stmt.executeUpdate();

            cnn.getConnection().commit();
            resp = Constants.SUCCESS;
        } catch (Exception e) {
            logger.error("error in removeGiftContent from database", e);
            rollbackTransaction(cnn);
        } finally {
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return resp;
    }

    //Lay thong tin bai hat nam trong mot danh muc
    public Song getSongInfoMultilPart(int contentId, int songPart) {
        logger.error("TEST getSongInfoMultilPart :" + contentId + " and :" + songPart);
        Song s = new Song();
        PreparedStatement pstm = null;
        DbConnection conn = null;
        ResultSet rs = null;
        String sqlCommand = "select mp.* from music_content_part mp "
                + "where mp.status = ? and mp.part_number = ?  and mp.music_content_id = ?";

        int i = 0;
        try {
            conn = getConnection();
            pstm = conn.getConnection().prepareStatement(sqlCommand);
            pstm.setInt(++i, Constants.STATUS_PUBLIC);
            pstm.setInt(++i, songPart);
            pstm.setInt(++i, contentId);
            rs = pstm.executeQuery();
            while (rs.next()) {
                s.setSongId(rs.getString("music_content_id"));
                s.setBeat(rs.getString("part_number"));
                String namePath = rs.getString("path").substring(rs.getString("path").lastIndexOf("/") + 1);
                s.setName(namePath.replace("-", " "));
            }
        } catch (SQLException sle) {
            String msg = " SQL Exception in funticon getSongInfoMultilPart , errorcode :" + sle.getErrorCode();
            sle.printStackTrace();
            logger.error(msg, sle);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in function getSongInfoMultilPart");
        } finally {
            closeResultSet(rs);
            closeStatement(pstm);
            freeConnection(conn);
        }

        return s;
    }

    public Song getSongInfoByIdOrCode(int contentId, int contentCode) {
        Song s = new Song();

        PreparedStatement stmt = null;
        DbConnection cnn = null;
        ResultSet rs = null;
        String sql = "select * from music_content where status = ? and publish_date <= now() and total_part = 1 and (music_content_id = ? or code = ?)";

        int i = 0;
        try {
            cnn = getConnection();
            stmt = cnn.getConnection().prepareStatement(sql);
            stmt.setInt(++i, Constants.STATUS_PUBLIC);
            stmt.setInt(++i, contentId);
            stmt.setInt(++i, contentCode);

            rs = stmt.executeQuery();
            if (rs.next()) {
                s.setSongId(rs.getString("music_content_id"));
                s.setBeat(rs.getString("code"));
                s.setName(rs.getString("content_name"));
            }
        } catch (Exception e) {
            logger.error("error in getSongInfoByIdOrCode from database", e);
            s = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            freeConnection(cnn);
        }

        return s;
    }
}
