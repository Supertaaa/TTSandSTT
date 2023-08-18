package com.vega.service.api.object;

public class CCUInfo {

    public static final int CCU_BY_CALL = 0;
    public static final int CCU_BY_PACKAGE = 1;
    public static final int CCU_BY_KEY = 2;
    private String ccu;
    private int error_code;
    private int ccu_total = 0;
    private int ccu_type = 0;
    private int package_id = 0;
    private int key = 0;

    public int getCcu_type() {
        return ccu_type;
    }

    public void setCcu_type(int ccu_type) {
        this.ccu_type = ccu_type;
    }

    public int getPackage_id() {
        return package_id;
    }

    public void setPackage_id(int package_id) {
        this.package_id = package_id;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int game_id) {
        this.key = game_id;
    }

    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public String getCcu() {
        return ccu;
    }

    public void setCcu(String ccu) {
        this.ccu = ccu;
    }

    public int getCcu_total() {
        return ccu_total;
    }

    public void setCcu_total(int ccu_total) {
        this.ccu_total = ccu_total;
    }
}
