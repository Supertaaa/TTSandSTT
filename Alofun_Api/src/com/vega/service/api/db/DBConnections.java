/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.db;

import com.vega.service.api.common.Constants;
import com.vega.service.api.common.ProvinceInfo;
import com.vega.vcs.service.cache.CacheService;
import com.vega.vcs.service.database.DBPool;
import com.vega.vcs.service.database.pool.DbConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 *
 * @author N.Tuyen
 */
public class DBConnections {

    static transient Logger logger = Logger.getLogger(DBConnections.class);
    private DBPool dbPool;
    Ehcache cache;

    public boolean start() throws NamingException {
        Context ctx = new InitialContext();
        dbPool = (DBPool) ctx.lookup("service/dbpool");
        CacheService cacheService = (CacheService) ctx.lookup("service/cache");
        cache = cacheService.getCache("api");
        return true;
    }

    public boolean stop() {
        return true;
    }

    public boolean reload() {
        return true;
    }

    /**
     * Check-out 1 ket noi toi DB
     *
     * @return
     * @throws SQLException
     */
    public DbConnection getConnection() throws Exception {
        DbConnection conn = null;
        try {
            conn = dbPool.getConnectionPool().getConnection(30);
        } catch (Exception ex) {
            logger.error(ex);
        }
        if (conn == null) {
            logger.error("connection is not established");
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("get dbconnection ok");
            }
        }
        return conn;
    }

    /**
     * Giai phong connection toi DB
     *
     * @param cnn
     */
    public void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                logger.error("error in closeResultSet", e);
            }
        }
    }

    public void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                logger.error("error in closeStatement", e);
            }
        }
    }

    public void freeConnection(DbConnection cnn) {
        if (cnn != null) {
            try {
                dbPool.getConnectionPool().freeConnection(cnn);
            } catch (Exception e) {
                logger.error("error in freeConnection", e);
            }
        }
    }

    /**
     * Rollback lai giao dich
     *
     * @param cnn
     */
    public void rollbackTransaction(DbConnection cnn) {
        if (cnn != null) {
            try {
                cnn.getConnection().rollback();
            } catch (Exception ex) {
                logger.error("Error in rollbackTransaction", ex);
            }
        }
    }
      public ArrayList<ProvinceInfo> getListProvince() {
        ArrayList<ProvinceInfo> list = new ArrayList<ProvinceInfo>();
        String key = "getListProvince";
        Element data = cache.get(key);
        if (data != null) {
            logger.debug("getListProvince from cache");
            return (ArrayList<ProvinceInfo>) data.getObjectValue();
        }

        DbConnection cnn = null;
        PreparedStatement stmt1 = null;
        ResultSet rs = null;
        String sql = "select province_id, name, identify_sign, file_name, region from province "
                + " where status = ?";

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
            logger.error("Error in getListProvince", e);
            list = null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt1);
            freeConnection(cnn);
        }

        return list;
    }
}
