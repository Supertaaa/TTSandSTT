package com.vega.service.api.http;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.object.CCUInfo;

public class HttpRequest {

    static transient Logger logger = Logger.getLogger(HttpRequest.class);
    private Gson gson = new Gson();
    private DBStack db;
    private Client client;

    public HttpRequest(DBStack dbService) {
        this.db = dbService;

        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, Integer.parseInt(ConfigStack.getConfig("api_http", "connect_time_out", "0")));
        clientConfig.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, Integer.parseInt(ConfigStack.getConfig("api_http", "read_time_out", "0")));
        clientConfig.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, Integer.parseInt(ConfigStack.getConfig("api_http", "thread_pool", "1")));
        client = Client.create(clientConfig);
    }

    public String getRequest(String url) throws Exception {
        WebResource webResource = client.resource(url);
        String resp = webResource.type("text/html").get(String.class);
        logger.debug("getRequest: " + url + "; response: " + resp);
        return resp;
    }

    public int postRequest(String url, String input) throws Exception {
        WebResource webResource = client.resource(url);
        ClientResponse response = webResource
                .type("application/json").post(ClientResponse.class, input);
        int status = response.getStatus();
        logger.debug("postRequest: " + url + "; Input: " + input + "; Response: " + status);
        return status;
    }

    public void getCCU() {
        logger.debug("getCCU is called");
        String a = ConfigStack.getConfig("api_sipcm", "urls", "0");
        String[] callMonitorUrls = a.split(",");
        if (callMonitorUrls.length > 0) {
            ArrayList<CCUInfo> ccuList = new ArrayList<CCUInfo>();
            String url = null;
            int n = callMonitorUrls.length;
            String ccuResp = null;
            int total = 0;

            //1. CCU by Call
            for (int i = 0; i < n; i++) {
                url = callMonitorUrls[i] + "/getCCU";
                try {
                    ccuResp = getRequest(url);
                } catch (Exception e) {
                    logger.error(e);
                }

                if (ccuResp != null) {
                    CCUInfo ccuInfo = gson.fromJson(ccuResp, CCUInfo.class);
                    if (ccuInfo != null && ccuInfo.getError_code() == 0) {
                        total += Integer.parseInt(ccuInfo.getCcu());
                    }
                }
            }

            CCUInfo ccu = new CCUInfo();
            ccu.setCcu_total(total);
            ccu.setCcu_type(CCUInfo.CCU_BY_CALL);
            ccuList.add(ccu);

            //2. CCU by Package
            HashMap<Integer, Integer> ccuData = new HashMap<Integer, Integer>();
            for (int i = 0; i < n; i++) {
                url = callMonitorUrls[i] + "/getCCUAllPackageId";
                try {
                    ccuResp = getRequest(url);
                } catch (Exception e) {
                    logger.error(e);
                }

                if (ccuResp != null) {
                    CCUInfo ccuInfo = gson.fromJson(ccuResp, CCUInfo.class);
                    if (ccuInfo != null && ccuInfo.getError_code() == 0) {
                        String info = ccuInfo.getCcu();
                        if (info != null && info.length() > 0 && !info.equals("")) {
                            String[] ccuPackage = info.split(",");
                            for (int j = 0, m = ccuPackage.length; j < m; j++) {
                                String[] ccuPart = ccuPackage[j].split("\\|");
                                Integer packageId = Integer.parseInt(ccuPart[0]);
                                total = Integer.parseInt(ccuPart[1]);

                                total = ccuData.containsKey(packageId) ? total + ccuData.get(packageId) : total;
                                ccuData.put(packageId, total);
                            }
                        }
                    }
                }
            }

            for (Integer packageId : ccuData.keySet()) {
                ccu = new CCUInfo();
                ccu.setCcu_total(ccuData.get(packageId));
                ccu.setCcu_type(CCUInfo.CCU_BY_PACKAGE);
                ccu.setPackage_id(packageId);
                ccuList.add(ccu);
            }
            ccuData.clear();

            //3. CCU by Key
            for (int i = 0; i < n; i++) {
                url = callMonitorUrls[i] + "/getCCUAllChannelId";
                try {
                    ccuResp = getRequest(url);
                } catch (Exception e) {
                    logger.error(e);
                }

                if (ccuResp != null) {
                    CCUInfo ccuInfo = gson.fromJson(ccuResp, CCUInfo.class);
                    if (ccuInfo != null && ccuInfo.getError_code() == 0) {
                        String info = ccuInfo.getCcu();
                        if (info != null && info.length() > 0 && !info.equals("")) {
                            String[] ccuChannel = info.split(",");
                            for (int j = 0, m = ccuChannel.length; j < m; j++) {
                                String[] ccuPart = ccuChannel[j].split("\\|");
                                Integer channelId = Integer.parseInt(ccuPart[0]);
                                total = Integer.parseInt(ccuPart[1]);
                                total = ccuData.containsKey(channelId) ? total + ccuData.get(channelId) : total;
                                ccuData.put(channelId, total);
                            }
                        }
                    }
                }
            }

            for (Integer channelId : ccuData.keySet()) {
                ccu = new CCUInfo();
                ccu.setCcu_total(ccuData.get(channelId));
                ccu.setCcu_type(CCUInfo.CCU_BY_KEY);
                ccu.setKey(channelId);
                ccuList.add(ccu);
            }

            //insert DB
            if (ccuList.size() > 0) {
                db.insertCCU(ccuList);
            }
        }
    }

    public class SIPCMRequest extends Thread {

        private String requestUrl;
        private HashMap<String, String> params;

        public SIPCMRequest(String url, HashMap<String, String> p) {
            this.requestUrl = url;
            this.params = p;
        }

        @Override
        public void run() {
            try {
                if (params != null && params.size() > 0) {
                    String requestParams = "";
                    for (String k : params.keySet()) {
                        requestParams += requestParams.length() == 0 ? k + "=" + URLEncoder.encode(params.get(k), "UTF-8") : "&" + k + "=" + URLEncoder.encode(params.get(k), "UTF-8");
                    }

                    this.requestUrl += "?" + requestParams;
                }
                getRequest(this.requestUrl);
            } catch (Exception e) {
                logger.error(e.getStackTrace());
            }
        }
    }
}
