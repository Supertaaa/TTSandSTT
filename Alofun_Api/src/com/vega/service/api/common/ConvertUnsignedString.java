/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.common;

import java.io.UnsupportedEncodingException;
import java.text.Normalizer;

/**
 *
 * @author User
 */
public class ConvertUnsignedString
{

    public static String getUnsignedString(String s)
    {
    	if(s == null) return "";
    	
        StringBuilder unsignedString = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            unsignedString.append(getUnsignedChar(s.charAt(i)));
        }
        return unsignedString.toString();
    }

    public static char getUnsignedChar1(char c)
    {


        if (c == '\u00E1' || c == '\u00E0' || c == '\u1EA3' || c == '\u00E3' || c == '\u1EA1'
                || c == '\u0103' || c == '\u1EAF' || c == '\u1EB1' || c == '\u1EB3' || c == '\u1EB5' || c == '\u1EB7'
                || c == '\u00E2' || c == '\u1EA5' || c == '\u1EA7' || c == '\u1EA9' || c == '\u1EAB' || c == '\u1EAD') {
            return 'a';
        } else if (c == '\u00C1' || c == '\u00C0' || c == '\u1EA2' || c == '\u00C3' || c == '\u1EA0'
                || c == '\u0102' || c == '\u1EAE' || c == '\u1EB0' || c == '\u1EB2' || c == '\u1EB4' || c == '\u1EB6'
                || c == '\u00C2' || c == '\u1EA4' || c == '\u1EA6' || c == '\u1EA8' || c == '\u1EAA' || c == '\u1EAC') {
            return 'A';
        } else if (c == '\u00E9' || c == '\u00E8' || c == '\u1EBB' || c == '\u1EBD' || c == '\u1EB9'
                || c == '\u00EA' || c == '\u1EBF' || c == '\u1EC1' || c == '\u1EC3' || c == '\u1EC5' || c == '\u1EC7') {
            return 'e';
        } else if (c == '\u00C9' || c == '\u00C8' || c == '\u1EBA' || c == '\u1EBC' || c == '\u1EB8'
                || c == '\u00CA' || c == '\u1EBE' || c == '\u1EC0' || c == '\u1EC2' || c == '\u1EC4' || c == '\u1EC6') {
            return 'E';
        } else if (c == '\u00ED' || c == '\u00EC' || c == '\u1EC9' || c == '\u0129' || c == '\u1ECB') {
            return 'i';
        } else if (c == '\u00CD' || c == '\u00CC' || c == '\u1EC8' || c == '\u0128' || c == '\u1ECA') {
            return 'I';
        } else if (c == '\u00F3' || c == '\u00F2' || c == '\u1ECF' || c == '\u00F5' | c == '\u1ECD'
                || c == '\u00F4' || c == '\u1ED1' || c == '\u1ED3' || c == '\u1ED5' || c == '\u1ED7' || c == '\u1ED9'
                || c == '\u01A1' || c == '\u1EDB' || c == '\u1EDD' || c == '\u1EDF' || c == '\u1EE1' || c == '\u1EE3') {
            return 'o';
        } else if (c == '\u00D3' || c == '\u00D2' || c == '\u1ECE' || c == '\u00D5' | c == '\u1ECC'
                || c == '\u00D4' || c == '\u1ED0' || c == '\u1ED2' || c == '\u1ED4' || c == '\u1ED6' || c == '\u1ED8'
                || c == '\u01A0' || c == '\u1EDA' || c == '\u1EDC' || c == '\u1EDE' || c == '\u1EE0' || c == '\u1EE2') {
            return 'O';
        } else if (c == '\u00FA' || c == '\u00F9' || c == '\u1EE7' || c == '\u0169' | c == '\u1EE5'
                || c == '\u01B0' || c == '\u1EE9' || c == '\u1EEB' || c == '\u1EED' || c == '\u1EEF' || c == '\u1EF1') {
            return 'u';
        } else if (c == '\u00DA' || c == '\u00D9' || c == '\u1EE6' || c == '\u0168' | c == '\u1EE4'
                || c == '\u01AF' || c == '\u1EE8' || c == '\u1EEA' || c == '\u1EEC' || c == '\u1EEE' || c == '\u1EF0') {
            return 'U';
        } else if (c == '\u00FD' || c == '\u1EF3' || c == '\u1EF7' || c == '\u1EF9' || c == '\u1EF5') {
            return 'y';
        } else if (c == '\u00DD' || c == '\u1EF2' || c == '\u1EF6' || c == '\u1EF8' || c == '\u1EF4') {
            return 'Y';
        } else if (c == '\u0111') {
            return 'd';
        } else if (c == '\u0110') {
            return 'D';
        } else if (c == '\u0020') {
            return ' ';
        } else if (c == '\u005D' || c == '\u005B' || c == '\u0028' || c == '\u0026' || c == '\u0029' || c == '\u0021' || c == '\u002A' || c == '\u002C' || c == '\u002E' || c == '\u002F' || c == '\u003A' || c == '\u003A' || c == '\u003B' || c == '\u003F') {
            return '_';
        }
        return c;
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

    public static String convert(String a)
    {
        String str = ConvertUnsignedString.getUnsignedString(a);
        return str.replaceAll("[^A-Za-z0-9 -]", "");
    }

    public static String compositeToPrecomposed(String str)
    {
        // Perform Unicode NFC on NFD string
        return Normalizer.normalize(str, Normalizer.Form.NFC);
    }

    public static String ucs2ToUTF8(byte[] ucs2Bytes) throws UnsupportedEncodingException
    {

        String unicode = new String(ucs2Bytes, "UTF-16");

        String utf8 = new String(unicode.getBytes("UTF-8"), "Cp1252");

        return utf8;
    }
}
