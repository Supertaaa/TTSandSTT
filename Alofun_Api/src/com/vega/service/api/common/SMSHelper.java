package com.vega.service.api.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Random;


public class SMSHelper {
	
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
	
	public static boolean isNull(String str){
		boolean ret = false;
		if(str == null)
			ret = true;
		else 
			ret = str.trim().equals("");
			
		return ret;
	}
	
	public static String convertToFriendly(String a)
    {
		a = compositeToPrecomposed(a);
        String str = getUnsignedString(a);
        return str.replaceAll("[^A-Za-z0-9 -]", "");
    }
	
	public static String compositeToPrecomposed(String str)
    {
        // Perform Unicode NFC on NFD string
        return Normalizer.normalize(str, Normalizer.Form.NFC);
    }
	
	public static String getUnsignedString(String s)
    {
        StringBuilder unsignedString = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            unsignedString.append(getUnsignedChar(s.charAt(i)));
        }
        return unsignedString.toString();
    }
	
	public static char getUnsignedChar(char c)
    {
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
	
	public static String removeDoubleSpace(String content){
		if(isNull(content)) return content;
		
		while(content.contains("  ")){
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
            ex.printStackTrace();
            return "";
        }
    }
	
	public static String getRandomString(int length){
		Random random = new Random();

		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int baseCharNumber = Math.abs(random.nextInt()) % 62;
			int charNumber = 0;
			if (baseCharNumber < 26) {
				charNumber = 65 + baseCharNumber;
			}
			else if (baseCharNumber < 52){
				charNumber = 97 + (baseCharNumber - 26);
			}
			else {
				charNumber = 48 + (baseCharNumber - 52);
			}
			stringBuffer.append((char)charNumber);
		}
		
		return stringBuffer.toString();
	}
}
