package com.vega.service.api.common;

import com.vega.service.api.object.PackageInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class BillingHelper {

    
    public static Object[] shuffleList(Object[] a) {
        if (a == null || a.length <= 1) {
            return a;
        }

        int n = a.length;
        Random random = new Random();
        random.nextInt();
        for (int i = 0; i < n; i++) {
            int change = i + random.nextInt(n - i);
            swap(a, i, change);
        }
        return a;
    }
    
    private static void swap(Object[] a, int i, int change) {
        Object helper = a[i];
        a[i] = a[change];
        a[change] = helper;
    }
    
    /**
     * Dinh dang so dien thoai di dong theo chuan 849xxxxxxxx
     * @param msisdn
     * @return String
     */
    public static String formatMobileNumber(String mobile_number) {
        String resp = mobile_number;
        if (mobile_number.startsWith("84")) {
            resp = mobile_number;
        } else if (mobile_number.startsWith("084")) {
            resp = mobile_number.substring(1);
        } else if (mobile_number.startsWith("0")) {
            resp = "84" + mobile_number.substring(1);
        } else {
            resp = "84" + mobile_number;
        }
        return resp;
    }

    /**
     * Dinh dang so dien thoai di dong khong co prefix 84, 0
     * @param msisdn
     * @return
     */
    public static String formatMobileNumberWithoutPrefix(String msisdn) {
        String msdn = "";
        if (msisdn.startsWith("84")) {
            msdn = msisdn.substring(2, msisdn.length());
        } else if (msisdn.startsWith("0")) {
            msdn = msisdn.substring(1, msisdn.length());
        } else if (msisdn.startsWith("084")) {
            msdn = msisdn.substring(3, msisdn.length());
        } else {
            msdn = msisdn;
        }
        return msdn;
    }

    /**
     * Xac dinh trang thai null cua 1 chuoi
     * @param str
     * @return
     */
    public static boolean isNull(String str) {
        boolean ret = false;
        if (str == null) {
            ret = true;
        } else {
            ret = str.trim().equals("");
        }

        return ret;
    }

    public static int getInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getInt(String value, int defaultvalue) {
        try {
            return Integer.valueOf(value);
        } catch (Exception ex) {
            return defaultvalue;
        }
    }

    /**
     * Lay thong tin goi cuoc theo id
     * @param packageId
     * @param packages
     * @return
     */
    public static PackageInfo getPackageById(int packageId, ArrayList<PackageInfo> packages) {
        PackageInfo p = null;
        if (packages != null) {
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.getPackageId() == packageId) {
                    p = packageInfo;
                    break;
                }
            }
        }

        return p;
    }

    /**
     * Thiet lap thoi diem cuoi ngay
     * @param c
     * @return
     */
    public static Calendar getEndOfDate(Calendar c) {
        if (c == null) {
            return c;
        }

        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);

        return c;
    }

    /**
     * Thiet lap thoi diem dau ngay
     * @param c
     * @return
     */
    public static Calendar getStartOfDate(Calendar c) {
        if (c == null) {
            return c;
        }

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        return c;
    }
}
