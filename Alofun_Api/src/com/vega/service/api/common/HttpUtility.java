package com.vega.service.api.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * This class encapsulates methods for requesting a server via HTTP GET/POST and
 * provides methods for parsing response from the server.
 *
 * @author www.codejava.net
 *
 */
public class HttpUtility {

    public static String getValue(String xml, String tagName) {
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";

        int f = xml.indexOf(openTag) + openTag.length();
        int l = xml.indexOf(closeTag);

        return (f > l) ? "" : xml.substring(f, l);
    }

    /**
     * Makes an HTTP request using POST method to the specified URL.
     *
     * @param requestURL
     *            the URL of the remote server
     * @param params
     *            A map containing POST data in form of key-value pairs
     * @return An HttpURLConnection object
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public static HttpURLConnection sendPostRequest(String requestURL,
            String xml, int timeout) throws IOException, IllegalArgumentException {
        URL url = new URL(requestURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setUseCaches(false);

        httpConn.setDoInput(true); // true indicates the server returns response
        httpConn.setReadTimeout(timeout);

        httpConn.setDoOutput(true); // true indicates POST request

        // sends POST data
        OutputStreamWriter writer = new OutputStreamWriter(
                httpConn.getOutputStream());
        writer.write(xml);
        writer.flush();
        return httpConn;
    }

    /**
     * Returns an array of lines from the server's response. This method should
     * be used if the server returns multiple lines of String.
     *
     * @return an array of Strings of the server's response
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public static String sendXML(String url, String xml, int timeout) {
        InputStream inputStream = null;
        HttpURLConnection httpConn = null;
        BufferedReader reader = null;

        try {
            httpConn = sendPostRequest(url, xml, timeout);
            if (httpConn != null) {
                inputStream = httpConn.getInputStream();
                StringBuffer sb = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(
                        inputStream));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (httpConn != null) {
                    httpConn.disconnect();
                }
            } catch (IOException ex) {
            }
        }
        return "<return>-999</return>";
    }
    public static String get(String url, int timeout) throws IOException {

        InputStream inputStream = null;
        HttpURLConnection httpConn = sendGetRequest(url, timeout);
        if (httpConn != null) {
            inputStream = httpConn.getInputStream();
        } else {
            throw new IOException("Connection is not established.");
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line = "";
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(System.getProperty("line.separator"));
        }
        reader.close();

        if (httpConn != null) {
            httpConn.disconnect();
        }

        return sb.toString();
    }
 public static HttpURLConnection sendGetRequest(String requestURL, int timeout)
            throws IOException {
        URL url = new URL(requestURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);

        httpConn.setDoInput(true); // true indicates the server returns response
        httpConn.setConnectTimeout(timeout);
        httpConn.setReadTimeout(timeout);

        return httpConn;
    }
public static String buildParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String param : params.keySet()) {
                if (first) {
                    builder.append("?");
                    first = false;
                } else {
                    builder.append("&");
                }
                try {
                    builder.append(param).append("=").append(URLEncoder.encode(params.get(param), "utf8"));
                } catch (UnsupportedEncodingException ex) {
                     ex.printStackTrace();
                }
            }
            return builder.toString();
        }
    }
}
