package com.vega.service.api.common;

import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.object.PackageInfo;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import com.vega.service.api.object.LotteryInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

    public static boolean isMobileNumber(String msisdn) {
        // return msisdn.matches(ConfigStack.getConfig("api", "mobile_pattern", "^(0|84)((9[03])|(12[01268])|(89))\\d{7}$"));
        String test = ConfigStack.getConfig("api", "mobile_pattern", "^(0|84)((((9[01234678])|(12[34579]))\\d{7}$)|(|(888)\\d{6}$))");
        System.out.println("mobiPattenr : " + test);
        return msisdn.matches(ConfigStack.getConfig("api", "mobile_pattern", "^(0|84)((((9[01234678])|(12[34579]))\\d{7}$)|(|(888)\\d{6}$))"));
    }
     public static boolean isCheckMobi(String msisdn,String pattern) {
        System.out.println("mobiPattenr : " + pattern);
        return msisdn.matches(pattern);
    }

    public static String formatMobileNumber(String mobile_number) {
        String resp = mobile_number;
        String prefix_number = ConfigStack.getConfig("api_sms", "prefix_number", "84");
        if (mobile_number.startsWith(prefix_number)) {
            resp = mobile_number;
        } else if (mobile_number.startsWith("0" + prefix_number)) {
            resp = mobile_number.substring(1);
        } else if (mobile_number.startsWith("0")) {
            resp = prefix_number + mobile_number.substring(1);
        } else {
            resp = prefix_number + mobile_number;
        }
        return resp;
    }
    public static boolean isMobileNumber(String mobileNumber, String patternMobile) {
        boolean rs = false;
        if (!isNull(mobileNumber)) {
            Pattern pattern = Pattern.compile(patternMobile);
            Matcher matcher;
            matcher = pattern.matcher(mobileNumber.trim());
            if (matcher.matches()) {
                rs = true;
            }
        }

        return rs;
    }

    /**
     * Dinh dang so dien thoai di dong khong co prefix 84, 0
     *
     * @param msisdn
     * @return
     */
    public static String formatMobileNumberWithoutPrefix(String msisdn) {
        String msdn = "";
        String prefix_number = ConfigStack.getConfig("api_sms", "prefix_number", "84");
        if (msisdn.startsWith(prefix_number)) {
            msdn = msisdn.substring(2, msisdn.length());
        } else if (msisdn.startsWith("0")) {
            msdn = msisdn.substring(1, msisdn.length());
        } else if (msisdn.startsWith("0" + prefix_number)) {
            msdn = msisdn.substring(3, msisdn.length());
        } else {
            msdn = msisdn;
        }
        return msdn;
    }

    public static boolean isNull(String str) {
        boolean ret = false;
        if (str == null) {
            ret = true;
        } else {
            ret = str.trim().equals("");
        }

        return ret;
    }

    public static String convertToFriendly(String a) {
        a = compositeToPrecomposed(a);
        String str = getUnsignedString(a);
//        return str.replaceAll("[^A-Za-z0-9 -]", "");
        return str;
    }

    public static String compositeToPrecomposed(String str) {
        // Perform Unicode NFC on NFD string
        return Normalizer.normalize(str, Normalizer.Form.NFC);
    }

    public static String getUnsignedString(String s) {
        StringBuilder unsignedString = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            unsignedString.append(getUnsignedChar(s.charAt(i)));
        }
        return unsignedString.toString();
    }

    public static char getUnsignedChar(char c) {
        char result;
        switch (c) {
            case '\u00E1':
            case '\u00E0':
            case '\u1EA3':
            case '\u00E3':
            case '\u1EA1':
            case '\u0103':
            case '\u1EAF':
            case '\u1EB1':
            case '\u1EB3':
            case '\u1EB5':
            case '\u1EB7':
            case '\u00E2':
            case '\u1EA5':
            case '\u1EA7':
            case '\u1EA9':
            case '\u1EAB':
            case '\u1EAD':
            case '\u0203':
            case '\u01CE': {
                result = 'a';
                break;
            }
            case '\u00E9':
            case '\u00E8':
            case '\u1EBB':
            case '\u1EBD':
            case '\u1EB9':
            case '\u00EA':
            case '\u1EBF':
            case '\u1EC1':
            case '\u1EC3':
            case '\u1EC5':
            case '\u1EC7':
            case '\u0207': {
                result = 'e';
                break;
            }
            case '\u00ED':
            case '\u00EC':
            case '\u1EC9':
            case '\u0129':
            case '\u1ECB': {
                result = 'i';
                break;
            }
            case '\u00F3':
            case '\u00F2':
            case '\u1ECF':
            case '\u00F5':
            case '\u1ECD':
            case '\u00F4':
            case '\u1ED1':
            case '\u1ED3':
            case '\u1ED5':
            case '\u1ED7':
            case '\u1ED9':
            case '\u01A1':
            case '\u1EDB':
            case '\u1EDD':
            case '\u1EDF':
            case '\u1EE1':
            case '\u1EE3':
            case '\u020F': {
                result = 'o';
                break;
            }
            case '\u00FA':
            case '\u00F9':
            case '\u1EE7':
            case '\u0169':
            case '\u1EE5':
            case '\u01B0':
            case '\u1EE9':
            case '\u1EEB':
            case '\u1EED':
            case '\u1EEF':
            case '\u1EF1': {
                result = 'u';
                break;
            }
            case '\u00FD':
            case '\u1EF3':
            case '\u1EF7':
            case '\u1EF9':
            case '\u1EF5': {
                result = 'y';
                break;
            }
            case '\u0111': {
                result = 'd';
                break;
            }
            case '\u00C1':
            case '\u00C0':
            case '\u1EA2':
            case '\u00C3':
            case '\u1EA0':
            case '\u0102':
            case '\u1EAE':
            case '\u1EB0':
            case '\u1EB2':
            case '\u1EB4':
            case '\u1EB6':
            case '\u00C2':
            case '\u1EA4':
            case '\u1EA6':
            case '\u1EA8':
            case '\u1EAA':
            case '\u1EAC':
            case '\u0202':
            case '\u01CD': {
                result = 'A';
                break;
            }
            case '\u00C9':
            case '\u00C8':
            case '\u1EBA':
            case '\u1EBC':
            case '\u1EB8':
            case '\u00CA':
            case '\u1EBE':
            case '\u1EC0':
            case '\u1EC2':
            case '\u1EC4':
            case '\u1EC6':
            case '\u0206': {
                result = 'E';
                break;
            }
            case '\u00CD':
            case '\u00CC':
            case '\u1EC8':
            case '\u0128':
            case '\u1ECA': {
                result = 'I';
                break;
            }
            case '\u00D3':
            case '\u00D2':
            case '\u1ECE':
            case '\u00D5':
            case '\u1ECC':
            case '\u00D4':
            case '\u1ED0':
            case '\u1ED2':
            case '\u1ED4':
            case '\u1ED6':
            case '\u1ED8':
            case '\u01A0':
            case '\u1EDA':
            case '\u1EDC':
            case '\u1EDE':
            case '\u1EE0':
            case '\u1EE2':
            case '\u020E': {
                result = 'O';
                break;
            }
            case '\u00DA':
            case '\u00D9':
            case '\u1EE6':
            case '\u0168':
            case '\u1EE4':
            case '\u01AF':
            case '\u1EE8':
            case '\u1EEA':
            case '\u1EEC':
            case '\u1EEE':
            case '\u1EF0': {
                result = 'U';
                break;
            }

            case '\u00DD':
            case '\u1EF2':
            case '\u1EF6':
            case '\u1EF8':
            case '\u1EF4': {
                result = 'Y';
                break;
            }
            case '\u0110':
            case '\u00D0':
            case '\u0089': {
                result = 'D';
                break;
            }
            default:
                result = c;
        }
        return result;
    }

    public static String removeDoubleSpace(String content) {
        if (isNull(content)) {
            return content;
        }

        while (content.contains("  ")) {
            content = content.replaceAll("  ", " ");
        }

        return content;
    }

    public static String md5(String in) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(in.getBytes());

            byte byteData[] = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    public static String getRandomString(int length) {
        Random random = new Random();

        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int baseCharNumber = Math.abs(random.nextInt()) % 62;
            int charNumber = 0;
            if (baseCharNumber < 26) {
                charNumber = 65 + baseCharNumber;
            } else if (baseCharNumber < 52) {
                charNumber = 97 + (baseCharNumber - 26);
            } else {
                charNumber = 48 + (baseCharNumber - 52);
            }
            stringBuffer.append((char) charNumber);
        }

        return stringBuffer.toString();
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
     *
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

    public static PackageInfo getPackageByName(String packageName, ArrayList<PackageInfo> packages) {
        PackageInfo p = null;
        if (packages != null) {
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.getPackageName().toLowerCase().equals(packageName.toLowerCase())) {
                    p = packageInfo;
                    break;
                }
            }
        }

        return p;
    }

    /**
     * Thiet lap thoi diem cuoi ngay
     *
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
     *
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

    public static String prepaidContent(String content, String ngay, String phut, String tengoicuoc, String ten, String ma, String tengoicuocmoi, String cuocthoai, String ngaysudung, String casi, String giagoicuoc, String password, String sender, String receiver) {
        content = content.replaceAll("\\{ngay\\}", ngay);
        content = content.replaceAll("\\{phut\\}", phut);
        content = content.replaceAll("\\{tengoicuoc\\}", tengoicuoc);
        content = content.replaceAll("\\{giagoicuoc\\}", giagoicuoc);
        content = content.replaceAll("\\{tengoicuocmoi\\}", tengoicuocmoi);
        content = content.replaceAll("\\{ten\\}", ten);
        content = content.replaceAll("\\{ma\\}", ma);
        content = content.replaceAll("\\{cuocthoai\\}", cuocthoai);
        content = content.replaceAll("\\{ngaysudung\\}", ngaysudung);
        content = content.replaceAll("\\{casi\\}", casi);
        content = content.replaceAll("\\{password\\}", password);
        content = content.replaceAll("\\{receiver\\}", receiver);
        content = content.replaceAll("\\{sender\\}", sender);
        return content;
    }

    public static String convert(String org) {
        char arrChar[] = org.toCharArray();
        char result[] = new char[arrChar.length];
        for (int i = 0; i < arrChar.length; i++) {
            switch (arrChar[i]) {
                case '\u00E1':
                case '\u00E0':
                case '\u1EA3':
                case '\u00E3':
                case '\u1EA1':
                case '\u0103':
                case '\u1EAF':
                case '\u1EB1':
                case '\u1EB3':
                case '\u1EB5':
                case '\u1EB7':
                case '\u00E2':
                case '\u1EA5':
                case '\u1EA7':
                case '\u1EA9':
                case '\u1EAB':
                case '\u1EAD':
                case '\u0203':
                case '\u01CE': {
                    result[i] = 'a';
                    break;
                }
                case '\u00E9':
                case '\u00E8':
                case '\u1EBB':
                case '\u1EBD':
                case '\u1EB9':
                case '\u00EA':
                case '\u1EBF':
                case '\u1EC1':
                case '\u1EC3':
                case '\u1EC5':
                case '\u1EC7':
                case '\u0207': {
                    result[i] = 'e';
                    break;
                }
                case '\u00ED':
                case '\u00EC':
                case '\u1EC9':
                case '\u0129':
                case '\u1ECB': {
                    result[i] = 'i';
                    break;
                }
                case '\u00F3':
                case '\u00F2':
                case '\u1ECF':
                case '\u00F5':
                case '\u1ECD':
                case '\u00F4':
                case '\u1ED1':
                case '\u1ED3':
                case '\u1ED5':
                case '\u1ED7':
                case '\u1ED9':
                case '\u01A1':
                case '\u1EDB':
                case '\u1EDD':
                case '\u1EDF':
                case '\u1EE1':
                case '\u1EE3':
                case '\u020F': {
                    result[i] = 'o';
                    break;
                }
                case '\u00FA':
                case '\u00F9':
                case '\u1EE7':
                case '\u0169':
                case '\u1EE5':
                case '\u01B0':
                case '\u1EE9':
                case '\u1EEB':
                case '\u1EED':
                case '\u1EEF':
                case '\u1EF1': {
                    result[i] = 'u';
                    break;
                }
                case '\u00FD':
                case '\u1EF3':
                case '\u1EF7':
                case '\u1EF9':
                case '\u1EF5': {
                    result[i] = 'y';
                    break;
                }
                case '\u0111': {
                    result[i] = 'd';
                    break;
                }
                case '\u00C1':
                case '\u00C0':
                case '\u1EA2':
                case '\u00C3':
                case '\u1EA0':
                case '\u0102':
                case '\u1EAE':
                case '\u1EB0':
                case '\u1EB2':
                case '\u1EB4':
                case '\u1EB6':
                case '\u00C2':
                case '\u1EA4':
                case '\u1EA6':
                case '\u1EA8':
                case '\u1EAA':
                case '\u1EAC':
                case '\u0202':
                case '\u01CD': {
                    result[i] = 'A';
                    break;
                }
                case '\u00C9':
                case '\u00C8':
                case '\u1EBA':
                case '\u1EBC':
                case '\u1EB8':
                case '\u00CA':
                case '\u1EBE':
                case '\u1EC0':
                case '\u1EC2':
                case '\u1EC4':
                case '\u1EC6':
                case '\u0206': {
                    result[i] = 'E';
                    break;
                }
                case '\u00CD':
                case '\u00CC':
                case '\u1EC8':
                case '\u0128':
                case '\u1ECA': {
                    result[i] = 'I';
                    break;
                }
                case '\u00D3':
                case '\u00D2':
                case '\u1ECE':
                case '\u00D5':
                case '\u1ECC':
                case '\u00D4':
                case '\u1ED0':
                case '\u1ED2':
                case '\u1ED4':
                case '\u1ED6':
                case '\u1ED8':
                case '\u01A0':
                case '\u1EDA':
                case '\u1EDC':
                case '\u1EDE':
                case '\u1EE0':
                case '\u1EE2':
                case '\u020E': {
                    result[i] = 'O';
                    break;
                }
                case '\u00DA':
                case '\u00D9':
                case '\u1EE6':
                case '\u0168':
                case '\u1EE4':
                case '\u01AF':
                case '\u1EE8':
                case '\u1EEA':
                case '\u1EEC':
                case '\u1EEE':
                case '\u1EF0': {
                    result[i] = 'U';
                    break;
                }

                case '\u00DD':
                case '\u1EF2':
                case '\u1EF6':
                case '\u1EF8':
                case '\u1EF4': {
                    result[i] = 'Y';
                    break;
                }
                case '\u0110':
                case '\u00D0':
                case '\u0089': {
                    result[i] = 'D';
                    break;
                }
                default:
                    result[i] = arrChar[i];
            }
        }
        return new String(result);
    }

    public static long getMaxTime() {
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            String currentDate = format.format(new Date());
            currentDate = currentDate + " 23:59:59";
            SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date maxDate = format1.parse(currentDate);
            return maxDate.getTime();
        } catch (Exception ex) {
        }
        return 0;
    }

    public static int getExpireTime() {
        return Integer.parseInt(String.valueOf((getMaxTime() - new Date().getTime()) / (1000)));
    }

    public static String[] getTimeGift(String input) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf_ngay = new SimpleDateFormat("ddMMyyyy-HHmm");
        boolean sendNow = true;
        if (input.length() > 0) {
            // Gui hen gio
            try {
                Date date_hen = sdf_ngay.parse(input);
                Date date_system = new Date();
                long a = date_hen.getTime();
                long b = date_system.getTime();
                long c = (a - b) / 1000 / 60;
                int minute_between = Integer.parseInt(ConfigStack.getConfig("gift", "time_between_mt_and_gift", "5"));
                if (c > minute_between) {
                    sendNow = false;
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date_hen);
                    String[] resp = new String[2];
                    // Thoi gian gui Gift
                    resp[1] = sdf.format(cal.getTime());
                    // Thoi gian gui MT
                    cal.add(Calendar.MINUTE, 0 - minute_between);
                    resp[0] = sdf.format(cal.getTime());
                    return resp;
                }
            } catch (Exception e) {
                return null;
            }
        }
        if (sendNow) {
            // Gui ngay
            Calendar cal = Calendar.getInstance();
            String[] resp = new String[2];
            // Thoi gian gui MT
            resp[0] = sdf.format(cal.getTime());
            // Thoi gian gui Gift
            int minute_between = Integer.parseInt(ConfigStack.getConfig("gift", "time_between_mt_and_gift", "5"));
            cal.add(Calendar.MINUTE, minute_between);
            resp[1] = sdf.format(cal.getTime());
            return resp;
        }
        return null;
    }

    public static boolean isBlacklist(int userId, String blacklistUserIds, String separator) {
        boolean isBlacklist = false;

        if (userId > 0 && blacklistUserIds != null) {
            isBlacklist = blacklistUserIds.indexOf(separator + String.valueOf(userId) + separator) >= 0;
        }

        return isBlacklist;
    }

    public static ProvinceInfo getProvinceById(int id, ArrayList<ProvinceInfo> provinces) {
        ProvinceInfo p = null;

        if (provinces != null && id > 0) {
            for (int i = 0; i < provinces.size(); i++) {
                if (provinces.get(i).getProvinceId() == id) {
                    return provinces.get(i);
                }
            }
        }

        return p;
    }

    public static boolean checkDateOutOfRange(String strLastDate, int dayRange, String dateFormat) {
        boolean outOfRange = true;

        Calendar lastDate = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        Date d = null;
        try {
            d = sdf.parse(strLastDate);
        } catch (Exception e) {
            d = new Date();
        }
        lastDate.setTime(d);
        lastDate.set(Calendar.HOUR_OF_DAY, 0);
        lastDate.set(Calendar.MINUTE, 0);
        lastDate.set(Calendar.SECOND, 0);
        lastDate.set(Calendar.MILLISECOND, 0);

        Calendar currentDate = Calendar.getInstance();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        currentDate.set(Calendar.MILLISECOND, 0);

        lastDate.add(Calendar.DAY_OF_MONTH, dayRange);
        if (lastDate.after(currentDate)) {
            outOfRange = false;
        }

        return outOfRange;
    }

    public static ProvinceInfo getProvinceFromSign(String sign, ArrayList<ProvinceInfo> provinces) {
        ProvinceInfo p = null;

        if (provinces != null && sign != null) {
            for (int i = 0; i < provinces.size(); i++) {
                if (provinces.get(i).findProvince(sign)) {
                    return provinces.get(i);
                }
            }
        }

        return p;
    }

    public static String processMobile(String mobile_number) {
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

    public static String processVina(String mobile_number) {
        String resp = mobile_number;
        if (mobile_number.startsWith("84")) {
            resp = mobile_number.substring(2);
            resp = "0" + resp;
        } else if (mobile_number.startsWith("084")) {
            resp = mobile_number.substring(3);
            resp = "0" + resp;
        } else if (mobile_number.startsWith("0")) {
            resp = mobile_number;
        } else {
            resp = "0" + mobile_number;
        }
        return resp;
    }

    public static boolean isOutOfRangeValidHour(String rangeHour, String separator) {
        boolean isOutOfRange = false;
        if (rangeHour != null && separator != null) {
            try {
                String[] part = rangeHour.split(separator);
                int fromHour = getInt(part[0], 0);
                int toHour = getInt(part[1], 0);

                int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                isOutOfRange = !(currentHour >= fromHour && currentHour <= toHour);
            } catch (Exception e) {
                isOutOfRange = false;
            }
        }

        return isOutOfRange;
    }

    public static boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static void copyListToArray(ArrayList<LotteryInfo> list, LotteryInfo[] data) {

        for (int i = 0; i < list.size(); i++) {
            data[i] = list.get(i);
        }
    }

    public static <T> void copyListToArray(List<T> list, Object[] arr) {
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
    }

    public static Calendar[] formatTimeGift(String input, String format, int delayMinuteToCall) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        if (input.length() > 0) {
            try {
                Date callDate = sdf.parse(input);
                Date currentDate = new Date();
                long a = callDate.getTime();
                long b = currentDate.getTime();
                long c = (a - b) / 1000 / 60;

                if (c > delayMinuteToCall) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(callDate);
                    Calendar[] resp = new Calendar[2];
                    //Time to call
                    resp[1] = (Calendar) cal.clone();
                    //Time to sms
                    cal.add(Calendar.MINUTE, 0 - delayMinuteToCall);
                    resp[0] = cal;

                    return resp;
                }
            } catch (Exception e) {
                return null;
            }
        }

        Calendar cal = Calendar.getInstance();
        Calendar[] resp = new Calendar[2];
        // Time to sms
        resp[0] = (Calendar) cal.clone();
        // Time to call
        cal.add(Calendar.MINUTE, delayMinuteToCall);
        resp[1] = cal;

        return resp;
    }
    public static long getLong(String value, long defaultvalue) {
        try {
            return Long.valueOf(value);
        } catch (Exception ex) {
            return defaultvalue;
        }
    }
}
