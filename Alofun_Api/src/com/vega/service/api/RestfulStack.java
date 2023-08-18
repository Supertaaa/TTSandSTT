package com.vega.service.api;

import java.util.ArrayList;
import com.vega.service.api.sms.SMSStack;
import com.vega.service.api.billing.BillingStack;
import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.vega.alome.sbb.billing.bundletype.BillingSBBInterface;
import com.vega.alome.sbb.billing.bundletype.SubPackageInfo;
import com.vega.alome.sbb.billing.bundletype.SubPackageInfo.SubPackageStatus;
import com.vega.rabbit.message.Message;
import com.vega.service.api.charging.ChargingStack;
import com.vega.service.api.common.Constants;
import com.vega.service.api.common.DateUtil;
import com.vega.service.api.common.Helper;
import com.vega.service.api.common.ProvinceInfo;
import com.vega.service.api.common.SubMessageInfo;
import com.vega.service.api.common.SubProfileInfo;
import com.vega.service.api.config.ConfigStack;
import com.vega.service.api.db.DBStack;
import com.vega.service.api.http.HttpRequest;
import com.vega.service.api.logfile.LogFileStack;
import com.vega.service.api.object.BillingErrorCode;
import com.vega.service.api.object.City;
import com.vega.service.api.object.Content;
import com.vega.service.api.object.CustomDate;
import com.vega.service.api.object.HoroscopeInDateRange;
import com.vega.service.api.object.ListStory;
import com.vega.service.api.object.ListenHistory;
import com.vega.service.api.object.PackageInfo;
import com.vega.service.api.object.SMS;
import com.vega.service.api.object.SMSType;
import com.vega.service.api.object.StoryInfo;
import com.vega.service.api.object.User;
import com.vega.service.api.redis.RedisManager;
import com.vega.service.api.redis.RedisManagerFactory;
import com.vega.service.api.resources.Service;
import com.vega.service.api.response.Result;
import com.vega.service.api.response.ResultAbstract;
import com.vega.service.api.response.ResultGoogleService;
import com.vega.service.api.googleUtils.*;
import com.vega.service.api.googleUtils.ServiceStack.ServiceStack;
import com.vega.vcs.service.alert.AlertService;
import com.vega.vcs.service.config.ConfigService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jettison.json.JSONObject;

public class RestfulStack {

    static Logger logger = Logger.getLogger(RestfulStack.class);
    RestfulRA ra;
    Client client;
    DBStack db;
    private BillingStack billing;
    BillingSBBInterface billingSbb;
    SMSStack smsStack;
    LotteryStack lotteryStack;
    GameStack gameStack;
    GiftStack giftStack;
    WeatherStack weatherStack;
    //gamification
    GamificationStack gamificationStack;
    public static AlertService alertService = null;
    AtomicInteger counter;
    public static RedisManager redisManager;
    // Parse Json
    Gson gson = new Gson();
    ObjectMapper objectMapper = new ObjectMapper();
    boolean isMaster;
    ConcurrentHashMap<String, Calendar> msisdnReceiveMTNotEnough;
    // CCU
    HttpRequest httpRequest;
    ChargingStack chargingStack;
    LogFileStack logFileStack;
    // RabbitMQ
    //RabbitManager manager;
    // IDOL
    IdolStack idolStack;
    // TOPTEN
    ToptenStack toptenStack;
    //STUDIO
    StudioStack studioStack;
    // Connect
    ConnectStack connectStack;

    GameIntellectualStack gameIntellectualStack;

    public RestfulStack() {
    }

    public RestfulStack(RestfulRA ra) {
        this.ra = ra;
    }

    public RestfulRA getRA() {
        return this.ra;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean start() throws Exception {
        logger.info("resfulStack start  : ");
        Context ctx = new InitialContext();
        ConfigService configService = (ConfigService) ctx.lookup("service/config");
        ConfigStack.loadConfig(configService);
        // Alert
        alertService = (AlertService) ctx.lookup("service/alert");

        // DB
        db = new DBStack();
        db.start();
        logger.info("resfulStack start  db: ");
        // Config
        ConfigStack.db = db;
        String abc = ConfigStack.getConfig("api_charging", "username", null);
        logger.info("Fuck abc : " + abc);
        // Charging
        chargingStack = new ChargingStack();
        // Billing
        billingSbb = (BillingSBBInterface) ctx.lookup("bundle/billingsbb");
        billing = new BillingStack();
        billing.setDb(db);
        billing.setBillingSBB(billingSbb);
        // SMS
        smsStack = new SMSStack();
        smsStack.setBilling(getBilling());
        smsStack.setDb(db);
        smsStack.setRestfulStack(this);
        smsStack.setChargingStack(chargingStack);
        // lotteryStack
        lotteryStack = new LotteryStack();
        gameStack = new GameStack();
        gameStack.setRestfulStack(this);
        giftStack = new GiftStack();
        giftStack.start(this);
        weatherStack = new WeatherStack();
        // IDOL
        idolStack = new IdolStack();
        idolStack.setDbStack(db);
        idolStack.setRestfulStack(this);
        idolStack.setSmsStack(smsStack);
        idolStack.setBilling(billing);
        // TOPTEN
        toptenStack = new ToptenStack();
        toptenStack.setDbStack(db);
        toptenStack.setRestfulStack(this);
        toptenStack.setSmsStack(smsStack);
        toptenStack.setBilling(billing);
        //STUDIO
        studioStack = new StudioStack();
        studioStack.setDbStack(db);
        studioStack.setRestfulStack(this);
        studioStack.setSmsStack(smsStack);
        studioStack.setBilling(billing);

        // mini game
        gamificationStack = new GamificationStack();
        gamificationStack.setRestfulStack(this);
        gamificationStack.setDbStack(db);
        // connect
        connectStack = new ConnectStack();
        connectStack.setResfulStack(this);

        counter = new AtomicInteger();
        Service.setStack(this);

        gameIntellectualStack = new GameIntellectualStack();
        gameIntellectualStack.setResfulStack(this);

        msisdnReceiveMTNotEnough = new ConcurrentHashMap<String, Calendar>();

        // Redis
        //redisManager = RedisManagerFactory.createRedisManager();
        // CCU
        httpRequest = new HttpRequest(db);
        // RabbitMQ
//        manager = new RabbitManager();
//        manager.loadConfig(ConfigStack.path_rabbitmq, ConfigStack.path_queue);
//        RabbitListener listener = new RabbitListener();
//        listener.setRestfulStack(this);
//        ArrayList<MessageListener> listListener = new ArrayList<MessageListener>();
//        listListener.add(listener);
//        manager.setMessageListener(listListener);
//        manager.start();
//        // WS
//        WSServiceExecutor ws = new WSServiceExecutor(new WSService());
//        ws.getService().setServiceUrl(ConfigStack.getConfig("api_webservice", "url", "http://0.0.0.0:12345/ws/listener"));
//        ws.getService().setRestfulStack(this);
//        ws.start();
        return true;
    }

    public boolean stop() {
        return true;
    }

    public boolean reload() {
        try {
            this.stop();
            this.start();
            return true;
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    public void refreshConfig() throws Exception {
        db.refreshConfig();
    }

    public void refreshListPackage() {
        db.refreshListPackage();
    }

    public static String pushToCacheWithExpiredTime(String key, String value,
            int expiredTime) {
        if (redisManager != null) {
            logger.debug("pushToCacheWithExpiredTime, key = " + key + ", value = "
                    + value);
            String rs = redisManager.set(key, value);
            redisManager.setExpiredTime(key, expiredTime);
            return rs;
        }
        return null;
    }

    public static String pushToCache(String key, String value) {
        logger.debug("pushToCache, key = " + key + ", value = "
                + value);
        if (redisManager != null) {
            String rs = redisManager.set(key, value);
            return rs;
        }
        return null;
    }

    public static void setExpiredTime(String key, int expire_time) {
        logger.debug("setExpiredTime, key = " + key + ", expire_time = "
                + expire_time);
        if (redisManager != null) {
            redisManager.setExpiredTime(key, expire_time);
        }

    }

    public static String getFromCache(String key) {
        if (redisManager != null) {
            return redisManager.get(key);
        }
        return null;
    }

    public void onCCUEvent() {
        httpRequest.getCCU();
    }

    public void sendSubMessage() {
        // get du lieu o db
        logger.info(">>>>>>>>>>>> sendSubMessage : ");
        HashMap<String, Object> resp = db.getListSubMessageSendMt(0);
        String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));

        if (Constants.SUCCESS.equals(errorCode)) {
            // sendmt
            ArrayList<SubMessageInfo> array = (ArrayList<SubMessageInfo>) resp.get("data");
            logger.info("data SubMessage :" + array.size());
            if (array.size() > 0) {

                int i = 0;
                for (i = 0; i < array.size(); i++) {
                    SubMessageInfo subMessage;
                    String mt = "";
                    HashMap<String, String> params = new HashMap<>();
                    subMessage = array.get(i);
                    if (subMessage.getMessageType() == 2) {
                        db.updateSubMessageSendMt(subMessage);
                        mt = ConfigStack.getConfig("mt", "mt_send_sms_message", "Ban vua nhan dươc tin nhan tu thanh vien so {idsender}: {content}");
                        logger.info("mt: " + mt);
                        logger.info("userId: " + String.valueOf(subMessage.getIdSender()));
                        if (mt != null) {
                            mt = mt.replaceAll("\\{idsender\\}", String.valueOf(subMessage.getIdSender()));
                            mt = mt.replaceAll("\\{content\\}", String.valueOf(subMessage.getSmsContent()));
                            logger.info("mt: " + mt);
                        }

                    } else {
                        logger.info("data SubMessage thu i :" + i);
                        logger.info("data SubMessage thu i :" + subMessage.getMessageId());
                        logger.info("data SubMessage thu i :" + subMessage.getIdSender());
                        logger.info("data SubMessage thu i :" + subMessage.getIdReceiver());
                        logger.info("data SubMessage thu i :" + subMessage.getSender());
                        logger.info("data SubMessage thu i :" + subMessage.getReceiver());
                        db.updateSubMessageSendMt(subMessage);

                        mt = ConfigStack.getConfig("mt", "FRIEND_NEW_VOICE_MSG", "He thong loi");
                        logger.info("mt: " + mt);
                        logger.info("userId: " + String.valueOf(subMessage.getIdSender()));
                        if (mt != null) {
                            mt = mt.replaceAll("\\{danh_sach\\}", String.valueOf(subMessage.getIdSender()));
                            logger.info("mt: " + mt);
                        }
                    }

                    params.put("msisdn", subMessage.getReceiver());
                    params.put("content", mt);
                    params.put("source", "SYSTEM");
                    params.put("packageId", "0");
                    params.put("type", "10");
                    sendSMS(params);
                    // update lai du lieu cho khach hang
                }
            }
        }
    }

    public Object process(HashMap<String, String> properties) {
        Result result = new Result();
        try {
            String functionName = properties.get("function_name");
            String msisdn = properties.get("msisdn");
            if (msisdn != null) {
                msisdn = Helper.formatMobileNumber(msisdn);
                properties.put("msisdn", msisdn);
            }
            logger.info("function_name = " + functionName);
            Method n = this.getClass().getMethod(functionName, HashMap.class);
            return n.invoke(this, properties);
        } catch (Exception ex) {
            logger.error("Exception in process function: ", ex);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return result;
    }

    // SMS
    public String onSMS(String keyWord, String msisdn, String serviceId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        logger.info("Received MO onSMS: " + msisdn + " => " + keyWord);
        String mt = "";
        msisdn = Helper.formatMobileNumber(msisdn);
        SMS sms = new SMS();
        SMS smsRespone = new SMS();
        sms.setMoContent(Helper.convert(keyWord));
        sms.setMsisdn(msisdn);
        sms.setServiceNumber(serviceId);
        sms.setHaveMO(true);
        sms.setSource("SMS");
        try {
            Calendar cal = Calendar.getInstance();
            sms.setMoReceivedTime(new java.sql.Timestamp(cal.getTimeInMillis()));
            smsRespone = smsStack.receiveSMS(sms);
            logger.info(">>>> respone : " + smsRespone.getMtContent());
            mt = "0|" + smsRespone.getMtContent();
        } catch (Exception ex) {
            logger.error("onSMS Error : " + ex);
            return "1|Recevice MO Error";
        }
        return mt;
    }

    public void sendMT(SMS sms) {
        logger.info(">>>>>> sendMT >>>>");
        try {
            String msisdn = Helper.formatMobileNumber(sms.getMsisdn());
            String content = sms.getMtContent();
            int type = sms.getType();
            int delayTime = (int) sms.getDelay();
            billingSbb.sendSMS(msisdn, content, type, delayTime);
        } catch (Exception ex) {
            logger.error("send mt error :" + ex);
        }
    }
    
   

    
    
//    public String getPositionOfCity(HashMap<String, String> params) throws Exception {
//    	//System.out.println("FFFFFFF");
//    	City city = new City();
//    	
//    	com.vega.service.api.googleUtils.db db = new com.vega.service.api.googleUtils.db();
//    	
//    	try {
//    		
//    		
//			String cityName = ARSUtils.getAsr();
//			System.out.println(cityName);
//			city = db.getCityByName(cityName);
//			
//			return cityName;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    	return null;
//    }
    
    public List<String> getRoute(HashMap<String, String> params) throws IOException, URISyntaxException{
    	List<String> result = new ArrayList<>();
    	String origin = params.get("origin");
    	String destination = params.get("destination");
    	result = ServiceStack.getRoute(origin, destination);
    	System.out.println(result);
    	return result;
    }
    
    public ResultGoogleService answerInformationChannel(HashMap<String, String> params) throws SQLException, Exception {
    	
    	//System.out.println("NGU");
    	String question = ARSUtils.getAsr().toLowerCase();
    	
    	//String question = "cho tao nhạc hồng";
    	
    	//String question = params.get("ques");
    	
    	
    	com.vega.service.api.googleUtils.db db = new com.vega.service.api.googleUtils.db();
    	System.out.println(question);
    	ResultGoogleService result = new ResultGoogleService();
    	
    	if(question.contains("thời tiết")) {
    		
    		City city = db.getProvince(question);
    		
    		CustomDate customDate= new CustomDate();
    		List<String> action = db.getActionService(question);
    		
    		if(action.isEmpty() || city.getLat() == null || city.getLon() == null || city.getName() == null) {
    			
    			return result;
    		}
    		
    		else if(action.get(0).equals("tomorrow") ) {
    			LocalDate today = LocalDate.now();
    			LocalDate tomorrow = today.plusDays(1);
    			customDate.setDay(Integer.toString(tomorrow.getDayOfMonth()));
        		customDate.setMonth(Integer.toString(tomorrow.getMonthValue()));
        		
        		
    			
    		}
    		
    		else if (action.get(0).equals("nexttomorrow")) {
    			
    			LocalDate today = LocalDate.now();
    			LocalDate tomorrow = today.plusDays(2);
    			customDate.setDay(Integer.toString(tomorrow.getDayOfMonth()));
        		customDate.setMonth(Integer.toString(tomorrow.getMonthValue()));
    			
    		}
    		
    		else if (action.get(0).equals("today")) {
    			LocalDate today = LocalDate.now();
    			customDate.setDay(Integer.toString(today.getDayOfMonth()));
        		customDate.setMonth(Integer.toString(today.getMonthValue()));
    			
    		}
    		
    		else {
    			
    			if(action.contains("day") && action.contains("month")) {
    				String[] part = question.split(" ");
    				int dayIndex = db.getIndex(part, "ngày") + 1;
    				int monthIndex = db.getIndex(part, "tháng") + 1;
    				customDate.setDay(part[dayIndex]);
    				customDate.setMonth(part[monthIndex]);
    			}
    			else {
    				return null;
    			}
        		
    		}
    		
			result.setLat(city.getLat());
			result.setLon(city.getLon());
			result.setProvince(city.getName());
			result.setDate(customDate);
		
    		
			return result;
    		
    		
    	}
    	else if (question.contains("nhạc")) {
    		if(!db.getActionService(question).isEmpty()) {
    			result.setMusic(db.getActionService(question).get(0));
    		}
    		return result;
    	}
    	
    	else if (question.contains("nghe bài hát")) {
    		
    		
    	}
    	
    	else if (question.contains("đường")) {
    		
    		List<String> action = db.getActionService(question);
    		
    		if(action.contains("from") && action.contains("to")) {
    			
    			int originIndexBegin = question.indexOf("từ") + 3;
        	    int originIndexEnd = question.indexOf("đến") - 1;
        	    int desIndexBegin = question.indexOf("đến") + 4;
        	    String origin = question.substring(originIndexBegin, originIndexEnd);
        	    String destination = question.substring(desIndexBegin);
        	    
        	    System.out.println("Bat dau tu: " + origin);
        	    System.out.println("Ket thuc tai: " + destination);
        	    
        	    result.setRoute(ServiceStack.getRoute(origin, destination));
				
			}

    	    return result;
    		
    	}
    	else if (question.contains("biển số")) {
    		
    		
    		List<String> action = db.getActionService(question);
    		
    		if(!action.isEmpty()) {
    			
    			if(action.contains("province?")) {

        			String[] part = question.split(" ");
        			List<String> infor = new ArrayList<>();
        			for(int i = 0; i < part.length; i++) {
        				 
    			        if (part[i].matches("-?\\d+(\\.\\d+)?")) {
    			        	infor.add(part[i]);
    			        	infor.add(db.getProvinceByLicencePlate(part[i]));
    			        }
        			}
        			
        			result.setProvinceBylicencePlate(infor);
        			
        			return result;
        		}
        		else if(action.contains("plate?")) {
        			City city = db.getProvince(question);
        			List<String> infor = new ArrayList<>();
        			
        			System.out.println(city.getName());
        			if(city.getName() != null & city != null) {
//        				infor.add(question)
        				result.setLicencePlateByProvince(db.getLicencePlateByProvince(city.getName()));
        			}
        			
        			return result;
        		}
    			
    			
    			
    		}
    		
    		
    		
    		
    		
			
    	}
    	
    	
    	return null;
    }
    
    
   
    
    
    public Result insertServey(HashMap<String, String> params) throws NumberFormatException, ClassNotFoundException, SQLException {
    	Result result = new Result();
    	com.vega.service.api.googleUtils.db db = new com.vega.service.api.googleUtils.db();
    	
    	if(db.insertServey(params.get("msisd"), Integer.valueOf(params.get("isEnjoy")), Integer.valueOf(params.get("enjoyLevel")))) {
    		result.setCount("1");
    		result.setErrorCode("0");
    	}
    	
    	return result;
    }
    
    
//    public CustomDate getDate(HashMap<String, String> params) {
//    	
//    	CustomDate resultDate = new CustomDate();
//    	String[] resultPart;
//    	try {
//			String rawDate = ARSUtils.getAsr(params.get("filePath"));
//    		//String rawDate = ARSUtils.getAsr("C:/Users/Admin PC/Downloads/a.wav");
//			System.out.println(rawDate);
//			if (rawDate.length() >= 14) {
//				resultPart = rawDate.substring(5).split(" tháng ");
//				resultDate.setDay(resultPart[0]);
//				resultDate.setMonth(resultPart[1]);
//				
//				
//				return resultDate;
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    	return null;
//    }
    
    public Result getVoice(HashMap<String, String> params) {
    	return ServiceStack.getVoice(params);
    }

    
    
    
    public Result notifyMoneyNotEnough(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        if (msisdn == null) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        boolean sendMT = false;
        Calendar sentTime = msisdnReceiveMTNotEnough.get(msisdn);
        if (sentTime != null) {
            Calendar currentTime = Calendar.getInstance();
            currentTime.set(Calendar.MILLISECOND, 0);
            currentTime.set(Calendar.SECOND, 0);
            currentTime.set(Calendar.MINUTE, 0);
            currentTime.set(Calendar.HOUR_OF_DAY, 0);

            if (currentTime.compareTo(sentTime) != 0) {
                sendMT = true;
                msisdnReceiveMTNotEnough.remove(msisdn);
                sentTime = null;
            }
        } else {
            sendMT = true;
        }
        if (sendMT) {
            String mt = ConfigStack.getConfig("mt", "mt_notify_not_nough_money_to_call", "");
            int mtType = SMSType.Nofity.getValue();
            SMS sms = new SMS();
            sms.setMtContent(mt);
            sms.setMsisdn(msisdn);
            sms.setType(mtType);
            sms.setHaveMO(false);
            sms.setAction("NOTIFY_NOT_ENOUGH_MONEY");
            sms.setSource("IVR");
            sendMT(sms);

            sentTime = Calendar.getInstance();
            sentTime.set(Calendar.MILLISECOND, 0);
            sentTime.set(Calendar.SECOND, 0);
            sentTime.set(Calendar.MINUTE, 0);
            sentTime.set(Calendar.HOUR_OF_DAY, 0);
            msisdnReceiveMTNotEnough.put(msisdn, sentTime);
        } else {
            logger.info("MT not enough money was sent in same day to msisdn: " + msisdn);
        }
        r.setErrorCode(Constants.SUCCESS);
        return r;
    }

    /**
     * sent mt bao thue bao da het phut nghe mien phi
     *
     * @param params
     * @return
     */
    public Result notifyOverFreeMinute(HashMap<String, String> params) {
        logger.info(">>> notifyOverFreeMinute");
        Result r = new Result();
        String msisdn = params.get("msisdn");
        int packageId = NumberUtils.toInt(params.get("package_id"));
        int freeminute = 0;

        if (packageId == 1) {
            freeminute = 10;
        } else if (packageId == 2) {
            freeminute = 30;
        }

        logger.info("msisdn: " + msisdn + ", packageId: " + packageId + ", freeminutes: " + freeminute);
        int minute_charge = freeminute + 1;

        if (msisdn == null) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        String mt = ConfigStack.getConfig("mt", "mt_notify_over_free_minute", "");
        mt = mt.replace("{freeminute}", String.valueOf(freeminute));
        mt = mt.replace("{minute_charge}", String.valueOf(minute_charge));
        int mtType = SMSType.Nofity.getValue();
        SMS sms = new SMS();
        sms.setMtContent(mt);
        sms.setMsisdn(msisdn);
        sms.setType(mtType);
        sms.setHaveMO(false);
        sms.setAction("NOTIFY_OVER_FREE_MINUTE");
        sms.setSource("IVR");
        sendMT(sms);

        r.setErrorCode(Constants.SUCCESS);
        return r;
    }

    public Result sendSMS(HashMap<String, String> params) {
        logger.info(">>>>>> sendSMS >>>>");
        Result r = new Result();
        String msisdn = params.get("msisdn");
        String content = params.get("content");
        String source = params.get("source");
        String packageId = params.get("packageId");
        String priority = params.get("priority");
        if (priority == null || priority.equals("")) {
            priority = "10";
        }
        if (packageId == null) {
            packageId = "0";
        }
        String type = params.get("type");
        if (msisdn == null || content == null || source == null || type == null || packageId == null) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        logger.info(">>>>>> sendSMS 2>>>>");
        int mtType = Integer.parseInt(type);
        SMS sms = new SMS();
        sms.setMtContent(content);
        sms.setMsisdn(msisdn);
        sms.setType(mtType);
        sms.setHaveMO(false);
        if (type.equals("7")) {
            sms.setAction("SEND_FROM_WEB");
        } else if (type.equals("8")) {
            sms.setAction("SEND_FROM_WAP");
        } else if (type.equals("9")) {
            sms.setAction("SEND_FROM_APP_CALLOUT");
        } else if (type.equals("10")) {
            sms.setAction("send_notify_sub_message");
        } else {
            sms.setAction("SEND_FROM_CMS");
        }
        sms.setSource(source);
        sms.setPriority(Integer.valueOf(priority));
        sms.setPackageId(Integer.parseInt(packageId));
        logger.info(">>>>>> sendSMS 3>>>>");
        sendMT(sms);
        r.setErrorCode(Constants.SUCCESS);
        logger.info(">>>>>> sendSMS errorCOde :" + r.getErrorCode());
        return r;
    }

    public Result sendSmsNotify(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        String mt_config = params.get("mt_config");
        String content = ConfigStack.getConfig("mt", mt_config, "");
        String source = params.get("source");
        String packageId = "0";
        int type = 1;
        if (msisdn == null || mt_config == null) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        if (content == null || content.equals("")) {
            r.setErrorCode(Constants.NO_DATA_FOUND);
            return r;
        }
        SMS sms = new SMS();
        sms.setMtContent(content);
        sms.setMsisdn(msisdn);
        sms.setType(type);
        sms.setHaveMO(false);
        sms.setAction("SEND_MT");
        sms.setSource(source);
        sms.setPackageId(Integer.parseInt(packageId));
        sendMT(sms);
        r.setErrorCode(Constants.SUCCESS);
        return r;
    }

    // Package
    public Object getSubPackage(HashMap<String, String> params) {
        logger.info(" >>>>>> getSubPackage :" + params.get("msisdn"));
        SubPackageInfo sub = new SubPackageInfo();
        sub = billing.getSubPackage(params);
        Result result = new Result();
        result.setErrorCode(String.valueOf(sub.getErrorCode().getValue()));
        if ("0".equals(result.getErrorCode())) {
            result.setPackageId(String.valueOf(sub.getPackageId()));
            result.setSubPackageId(String.valueOf(sub.getSubPackageId()));
            result.setExpireAt(sub.getExpireAt());
            result.setFreeMinutes(String.valueOf(sub.getFreeMinutes()));
            result.setMsisdn(sub.getMsisdn());
            result.setStatus(sub.getStatus().getValue());
            result.setPromotion(String.valueOf(sub.getPromotion().getValue()));
            result.setPromtListen("1");
            result.setPromtOrd("1");
        } else if ("2".equals(result.getErrorCode())) {
            result.setPackageId(String.valueOf(sub.getPackageId()));
            result.setExpireAt(sub.getExpireAt());
        } else {
            result.setPromotion(String.valueOf(sub.getPromotion().getValue()));
        }
        return result;
    }
    
    // getSubPackageFull
    public SubPackageInfo getSubPackageFull(HashMap<String, String> params) {
        logger.info(" >>>>>> getSubPackageFull :" + params.get("msisdn"));
        SubPackageInfo sub = billing.getSubPackage(params);
        return sub;
    }

    public Object checkNewVoiceMessage(HashMap<String, String> params) {

        /*
         * Kiem tra hom thu thoai
         */
        Result r = new Result();
        r.setErrorCode(Constants.SUCCESS);
        if (params.get("msisdn") == null || params.get("msisdn").equals("")) {
            r.setErrorCode(Constants.SYSTEM_ERROR);
            return r;
        }
        String msisdn = params.get("msisdn");
        String keyVoiceMsg = Constants.MSG_UNREAD_VOICE_PREFIX + msisdn;
        String hasUnreadVoiceMsg = getFromCache(keyVoiceMsg);
        if (hasUnreadVoiceMsg != null && hasUnreadVoiceMsg.equalsIgnoreCase("true")) {
            r.setHasUnreadVoiceMsg(1);
        } else {
            Boolean hasUnreadMsgFromDB = db.hasUnReadVoiceMessage(msisdn);
            r.setHasUnreadVoiceMsg(hasUnreadMsgFromDB != null ? hasUnreadMsgFromDB ? 1 : 0 : 0);
            if (hasUnreadMsgFromDB) {
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "music_day_for_check_promt", "7")) * 24 * 60 * 60;
                pushToCacheWithExpiredTime(keyVoiceMsg, "true", timeOut);
            }
        }
        return r;
    }

    public Object registerPackage(HashMap<String, String> params) {
        logger.info("RegisterPackage: " + params.get("msisdn") + "; packageId: " + params.get("packageId")
                + "; ipAdress: " + params.get("ipAddress"));
        SubPackageInfo sub = new SubPackageInfo();
        sub = billingSbb.registerPackage(params);
        logger.info("get result in registerPackage");
        Result result = new Result();
        result.setErrorCode(String.valueOf(sub.getErrorCode().getValue()));
        if (sub.getErrorCode().getValue() == 401 && sub.getPackageId() == 1) {
            String mt = ConfigStack.getConfig("mt", "mt_notify_not_enough_money_regis_daily", "");
            int mtType = SMSType.Nofity.getValue();
            SMS sms = new SMS();
            sms.setMtContent(mt);
            sms.setMsisdn(sub.getMsisdn());
            sms.setType(mtType);
            sms.setHaveMO(false);
            sms.setAction("NOTIFY_NOT_ENOUGH_MONEY");
            sms.setSource("IVR");
            sendMT(sms);
        } else if (sub.getErrorCode().getValue() == 401 && sub.getPackageId() == 2) {
            String mt = ConfigStack.getConfig("mt", "mt_notify_not_enough_money_regis_weekly", "");
            int mtType = SMSType.Nofity.getValue();
            SMS sms = new SMS();
            sms.setMtContent(mt);
            sms.setMsisdn(sub.getMsisdn());
            sms.setType(mtType);
            sms.setHaveMO(false);
            sms.setAction("NOTIFY_NOT_ENOUGH_MONEY");
            sms.setSource("IVR");
            sendMT(sms);
        }
        return result;
    }

    public Object cancelSubPackage(HashMap<String, String> params) {
        logger.info("cancelSubPackage: " + params.get("msisdn") + "; packageId: " + params.get("packageId")
                + "; ipAdress: " + params.get("ipAddress"));
        SubPackageInfo sub = new SubPackageInfo();
        sub = billingSbb.cancelSubPackage(params);
        Result result = new Result();
        result.setErrorCode(String.valueOf(sub.getErrorCode().getValue()));
        return result;
    }

    // Music
    public Object getListMusicByChannel(HashMap<String, String> params) {
        logger.debug("getListMusicByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 1;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListMusicByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            if (array_new.isEmpty()) {
                                for (int i = 0, n = array_listened.size(); i < n; i++) {
                                    Content item = array_listened.get(i);
                                    String a = ListenHistory.SEPERATOR + item.getContentId()
                                            + ListenHistory.SEPERATOR;
                                    if (contentListened.endsWith(a)) {
                                        r.setLastPartNumber(item.getContentId());
                                        break;
                                    }
                                }
                            } else {
                                r.setLastPartNumber("0");
                            }
                            array_new.addAll(array_listened);
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListMusicByChannel." + topicType + "." + topicOrd + "." + channelOrd + "." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "music_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object addMusicIntoAlbum(HashMap<String, String> params) {
        logger.debug("addMusicIntoAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentId = params.get("contentId");
            String partNumber = params.get("partNumber");
            String packageId = params.get("packageId");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.addMusicIntoAlbum(msisdn, contentId, partNumber);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                if (errorCode.equals(Constants.SUCCESS)) {
                    // Tra tin nhan
                    String mt = ConfigStack.getConfig("mt", "mt_add_music_to_album", "");
                    String contentName = String.valueOf(resp.get("content_name"));
                    String code = String.valueOf(resp.get("code"));
                    mt = Helper.prepaidContent(mt, "", "", "", contentName, code, "", "", "", "", "", "", "", "");
                    int mtType = SMSType.Genral.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setPackageId(Integer.parseInt(packageId));
                    sms.setSource("IVR");
                    sms.setAction("ADD_TO_ALBUM");
                    sendMT(sms);
                }
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object removeMusicFromAlbum(HashMap<String, String> params) {
        logger.debug("removeMusicFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentId = params.get("contentId");
            String partNumber = params.get("partNumber");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.removeMusicFromAlbum(msisdn, contentId, partNumber);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListMusicChannel(HashMap<String, String> params) {
        logger.debug("getListMusicChannel");
        Result r = new Result();
        try {
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int topic_type = 1;
            HashMap<String, Object> resp = db.getListChannel(topicOrd, topic_type);
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            r.setErrorCode(errorCode);
            if (errorCode.equals(Constants.SUCCESS)) {
                ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                r.setTotal(String.valueOf(array.size()));
                Content[] data = new Content[array.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = array.get(i);
                }
                r.setData((Content[]) Helper.shuffleList(data));
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListMusicFromAlbum(HashMap<String, String> params) {
        logger.debug("getListMusicFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            HashMap<String, Object> resp = db.getListMusicFromAlbum(msisdn);
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            r.setErrorCode(errorCode);
            if (errorCode.equals(Constants.SUCCESS)) {
                ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                r.setTotal(String.valueOf(array.size()));
                Content[] data = new Content[array.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = array.get(i);
                }
                r.setData(data);
                // Check 7 ngay o cho nay
                String key = this.getClass().getCanonicalName() + ".getListMusicFromAlbum." + msisdn;
                String value = getFromCache(key);
                if (value == null) {
                    r.setPromtListen("0");
                } else {
                    r.setPromtListen("1");
                }
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "music_day_for_check_promt", "7")) * 24 * 60 * 60;
                pushToCacheWithExpiredTime(key, msisdn, timeOut);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object updateHistoryContent(HashMap<String, String> params) {
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String channelId = params.get("channelId");
            String contentId = params.get("contentId");
            String topicType = params.get("topicType");
            if (msisdn == null || msisdn.equals("") || Integer.parseInt(contentId) <= 0
                    || Integer.parseInt(channelId) <= 0 || Integer.parseInt(topicType) <= 0) {
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }
            ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(channelId), Integer.parseInt(topicType));
            if (listenHis == null) {
                listenHis = new ListenHistory();
            }

            String contentListened = listenHis.getContentListened() == null
                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                            : listenHis.getContentListened();
            boolean hasHistory = !contentListened.equals("");
            if (hasHistory) {
                String key = ListenHistory.SEPERATOR + contentId
                        + ListenHistory.SEPERATOR;
                if (!contentListened.contains(key)) {
                    contentListened = contentListened + contentId + ListenHistory.SEPERATOR;
                } else {
                    key = contentId + ListenHistory.SEPERATOR;
                    contentListened = contentListened.replaceAll(key, "");
                    contentListened = contentListened + key;
                }
            } else {
                contentListened = ListenHistory.SEPERATOR + contentId
                        + ListenHistory.SEPERATOR;
            }
            listenHis.setContentListened(contentListened);
            listenHis.setMsisdn(msisdn);
            listenHis.setChannelId(Integer.parseInt(channelId));
            int result = -1;
            if (!hasHistory) {
                logger.error("Vao insert");
                result = db.insertListenHistory(listenHis, Integer.parseInt(topicType));
            } else {
                logger.error("Vao update");
                result = db.updateListenHistory(listenHis, Integer.parseInt(topicType));
            }
            r.setErrorCode(String.valueOf(result));
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListMusicMultiPartByChannel(HashMap<String, String> params) {
        logger.debug("getListMusicMultiPartByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            String test = params.get("musicTopic");
            int topicType = 1;
            int musicTopic = 0;
            if (params.get("musicTopic") != null && !params.get("musicTopic").equals("")) {
                musicTopic = Integer.parseInt(params.get("musicTopic"));
            }
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListMusicMultiPartByChannel(topicOrd, channelOrd, topicType, musicTopic);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    ListenHistory listenHis = db.getListenHistory(msisdn, topicOrd, topicType);
                    if (listenHis != null) {
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        String contentListened = listenHis.getContentListened() == null
                                || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                        ArrayList<Content> array_listened = new ArrayList<Content>();
                        for (int i = 0, n = array.size(); i < n; i++) {
                            Content item = array.get(i);
                            String a = ListenHistory.SEPERATOR
                                    + item.getContentId()
                                    + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                array_listened.add(item);
                            } else {
                                array_new.add(item);
                            }
                        }
                        array_new.addAll(array_listened);
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getMusicMultiPart(HashMap<String, String> params) {
        logger.debug("getMusicMultiPart");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int contentId = Integer.parseInt(params.get("contentId"));
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getMusicMultiPart(contentId);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setTotalPart(String.valueOf(resp.get("total_part")));
                    r.setTotal(String.valueOf(array.size()));
                    int topic_type = 1;
                    int channel_id = 0;
                    ListenHistory listenHis = db.getListenHistory(msisdn, channel_id, topic_type);
                    boolean found = false;
                    if (listenHis != null) {
                        String contentListened = listenHis.getContentListened() == null
                                || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                        for (int i = 0; i < array.size(); i++) {
                            Content item = array.get(i);
                            String a = ListenHistory.SEPERATOR
                                    + contentId
                                    + ":" + item.getPartNumber() + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                found = true;
                                r.setLastPartNumber(item.getPartNumber());
                                break;
                            }
                        }
                        String a = ListenHistory.SEPERATOR
                                + contentId
                                + ":0" + ListenHistory.SEPERATOR;
                        if (contentListened.contains(a)) {
                            found = true;
                            r.setLastPartNumber("0");
                        }
                    }
                    if (!found) {
                        r.setLastPartNumber("-1");
                    }
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getMusicMultiPart." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "music_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getMusicByCode(HashMap<String, String> params) {
        logger.debug("getMusicByCode");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int code = Integer.parseInt(params.get("code"));
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getMusicByCode(code);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setTotalPart(String.valueOf(resp.get("total_part")));
                    r.setTotal(String.valueOf(array.size()));
                    r.setContentId(String.valueOf(resp.get("content_id")));
                    int topic_type = 1;
                    int channel_id = 0;
                    ListenHistory listenHis = db.getListenHistory(msisdn, channel_id, topic_type);
                    boolean found = false;
                    if (listenHis != null) {
                        String contentListened = listenHis.getContentListened() == null
                                || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                        for (int i = 0; i < array.size(); i++) {
                            Content item = array.get(i);
                            String a = ListenHistory.SEPERATOR
                                    + r.getContentId()
                                    + ":" + item.getPartNumber() + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                found = true;
                                r.setLastPartNumber(item.getPartNumber());
                                break;
                            }
                        }
                        String a = ListenHistory.SEPERATOR
                                + r.getContentId()
                                + ":0" + ListenHistory.SEPERATOR;
                        if (contentListened.contains(a)) {
                            found = true;
                            r.setLastPartNumber("0");
                        }
                    }
                    if (!found) {
                        r.setLastPartNumber("-1");
                    }
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                    // Check 7 ngay o cho nay
                    int index = 0;
                    if (Integer.parseInt(r.getTotalPart()) > 1) {
                        index = 1;
                    }
                    String key = this.getClass().getCanonicalName() + ".getMusicByCode." + msisdn + "." + index;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "music_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object updateHistoryContentMultiPart(HashMap<String, String> params) {
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String duration = params.get("duration");
            if (duration == null) {
                duration = "0";
            }
            int channelId = Integer.parseInt(params.get("channelId"));
            int topicType = Integer.parseInt(params.get("topicType"));
            int contentId = Integer.parseInt(params.get("contentId"));
            int partNumber = Integer.parseInt(params.get("partNumber"));
            if (msisdn == null || msisdn.equals("") || contentId < 0
                    || topicType < 0 || partNumber < 0) {
                r.setErrorCode(Constants.WRONG_PARAM);
                return r;
            }
            ListenHistory listenHis = db.getListenHistory(msisdn, channelId, topicType);
            if (listenHis == null) {
                listenHis = new ListenHistory();
            }
            String contentListened = listenHis.getContentListened() == null
                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                            : listenHis.getContentListened();

            boolean hasHistory = !contentListened.equals("");
            logger.info("hasHistory value :" + hasHistory + " msisdn :" + msisdn + " content listend :" + listenHis.getContentListened());
            if (hasHistory) {
                logger.info("Vao day khi da ton tai lich su nghe");
                String key = ListenHistory.SEPERATOR + contentId + ":";
                if (!contentListened.contains(key)) {
                    contentListened = ListenHistory.SEPERATOR + contentId + ":" + partNumber
                            + contentListened;
                } else {
                    key = ListenHistory.SEPERATOR + contentId + ":" + partNumber + ListenHistory.SEPERATOR;
                    if (!contentListened.contains(key)) {
                        String[] a = contentListened.split("-");
                        for (int i = 0; i < a.length; i++) {
                            String[] b = a[i].split(":");
                            if (b[0].equalsIgnoreCase("" + contentId)) {
                                b[1] = "" + partNumber;
                                a[i] = b[0] + ":" + b[1];
                                break;
                            }
                        }
                        contentListened = "";
                        for (int i = 0; i < a.length; i++) {
                            if (a[i].length() > 0 && !a[i].equals("")) {
                                key = ListenHistory.SEPERATOR + a[i];
                                contentListened = contentListened + key;
                            }
                        }
                        contentListened = contentListened + ListenHistory.SEPERATOR;
                    } else {
                        // Khong lam j ca
                        r.setErrorCode(Constants.SUCCESS);
                        return r;
                    }
                }
            } else {
                contentListened = ListenHistory.SEPERATOR + contentId + ":" + partNumber + ListenHistory.SEPERATOR;
            }
            listenHis.setContentListened(contentListened);
            listenHis.setMsisdn(msisdn);
            listenHis.setChannelId(channelId);
            int result = -1;
            if (!hasHistory) {
                result = db.insertListenHistory(listenHis, topicType);
            } else {
                result = db.updateListenHistory(listenHis, topicType);
            }
            r.setErrorCode(String.valueOf(result));
            // Call to ranking for story
            if (topicType == 2) {
                StoryInfo storyInfo = new StoryInfo();
                storyInfo.setStoryId(contentId);
                storyInfo.setUserId(Long.parseLong(msisdn));
                storyInfo.setPartId(partNumber);
                storyInfo.setLastUseTime(System.currentTimeMillis());
                if (partNumber != 0) {
                    storyInfo.setFinishPart("false");
                }
                storyInfo.setDurSec(Long.parseLong(duration));
                // ra.getEngine().getThreadPool("call_to_ranking_pool").submit(callAddHistoryToRanking(storyInfo));
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // Log
    public Object insertLogIVR(HashMap<String, String> params) {
        logger.debug("insertLogIVR");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String dtmf = params.get("dtmf");
            int subPackageId = Integer.parseInt(params.get("subPackageId"));
            int packageId = Integer.parseInt(params.get("packageId"));
            String startTime = params.get("beginAt");
            String endTime = params.get("endAt");
            Date beginAt = DateUtil.parseDate(startTime);
            Date endAt = DateUtil.parseDate(endTime);
            String contentListen = params.get("contentListen");
            String storyListening = params.get("storyListening");
            if (msisdn != null) {
                db.insertLogIVR(msisdn, beginAt, endAt, dtmf, packageId, subPackageId);
                int daily_call_id = Integer.parseInt(DateUtil.formatTime(new Date()));
                if (contentListen != null && contentListen.length() > 0) {
                    db.insertAllSubListen(subPackageId, packageId, msisdn, contentListen, daily_call_id, startTime);
                }
                if (storyListening != null && storyListening.length() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat sdf_ngay = new SimpleDateFormat("yyyy-MM-dd");
                    Date date = new Date();
                    ListenHistory listenHistory = db.getSMSRemiderHistory(msisdn);
                    boolean sendSMS = true;
                    boolean hasHistory = false;
                    if (listenHistory != null) {
                        hasHistory = true;
                        if (listenHistory.getStatus() == 0) {
                            sendSMS = false;
                        }
                    } else {
                        String a = "";
                        String[] contents = storyListening.split(":");
                        for (int i = 0; i < contents.length; i++) {
                            int contentId = Integer.parseInt(contents[i]);
                            a = a + ListenHistory.SEPERATOR + contentId;
                        }
                        a = a + ListenHistory.SEPERATOR;
                        ListenHistory listenHistory2 = new ListenHistory();
                        listenHistory2.setMsisdn(msisdn);
                        listenHistory2.setMusicListened("");
                        listenHistory2.setMusicMtInDay(0);
                        listenHistory2.setStatus(1);
                        listenHistory2.setStoryListened(a);
                        listenHistory2.setStoryMtInDay(1);
                        listenHistory2.setStorySentDate(sdf.format(date));
                        listenHistory2.setLastestSentDate(sdf.format(date));
                        db.insertSMSRemiderHistory(listenHistory2);
                    }
                    // Gui tin nhan
                    if (sendSMS) {
                        boolean hasContent = false;
                        String b = "";
                        String contentListened = "";
                        if (hasHistory) {
                            contentListened = listenHistory.getStoryListened() == null
                                    || listenHistory.getStoryListened().equalsIgnoreCase("") ? "" : listenHistory.getStoryListened();
                        }
                        String[] contents = storyListening.split(":");
                        int index = 0;
                        for (int i = 0; i < contents.length; i++) {
                            boolean found = false;
                            int contentId = Integer.parseInt(contents[i]);
                            String a = ListenHistory.SEPERATOR
                                    + contentId
                                    + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                found = true;
                            } else {
                                if (hasHistory) {
                                    try {
                                        if (sdf_ngay.format(date).equals(sdf_ngay.format(sdf.parse(listenHistory.getStorySentDate())))) {
                                            if (listenHistory.getStoryMtInDay() >= Integer.parseInt(ConfigStack.getConfig("notify", "mt_story_in_day", "3"))) {
                                                found = true;
                                            } else {
                                                int c = listenHistory.getStoryMtInDay() + 1;
                                                listenHistory.setStoryMtInDay(c);
                                                listenHistory.setStorySentDate(sdf.format(date));
                                                listenHistory.setLastestSentDate(sdf.format(date));
                                            }
                                        } else {
                                            listenHistory.setStoryMtInDay(1);
                                            listenHistory.setStorySentDate(sdf.format(date));
                                            listenHistory.setLastestSentDate(sdf.format(date));
                                        }
                                    } catch (Exception ex) {
                                        logger.error(ex);
                                    }
                                }
                            }
                            if (!found) {
                                index++;
                                if (index <= Integer.parseInt(ConfigStack.getConfig("api_notify", "story_length_limit", "10"))) {
                                    if (hasHistory) {
                                        contentListened = contentListened + contentId + ListenHistory.SEPERATOR;
                                        listenHistory.setStoryListened(contentListened);
                                    }
                                    String content_name = db.getListStoryContents().get(contentId);
                                    int content_code = db.getListStoryCodes().get(contentId);
                                    String c = ConfigStack.getConfig("mt", "story_notify", "");
                                    c = Helper.prepaidContent(c, "", "", "", Helper.getUnsignedString(content_name), content_code + "", "", "", "", "", "", "", "", "");
                                    b = b + c;
                                    hasContent = true;
                                }
                            }
                        }
                        if (hasContent) {
                            int mtType = SMSType.Nofity.getValue();
                            String mt = ConfigStack.getConfig("mt", "mt_notify_story_success", "");
                            mt = Helper.prepaidContent(mt, "", "", "", b, "", "", "", "", "", "", "", "", "");
                            SMS sms = new SMS();
                            sms.setMtContent(mt);
                            sms.setMsisdn(params.get("msisdn"));
                            sms.setType(mtType);
                            sms.setHaveMO(false);
                            sms.setAction("NOTIFY CONTENT STORY");
                            sms.setSource("IVR");
                            sms.setPackageId(subPackageId);
                            sms.setBrandName(true);
                            sendMT(sms);
                            if (hasHistory) {
                                db.updateSMSReminderHistory(listenHistory);
                            }
                        }
                    }
                }
                r.setErrorCode("0");
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // Story
    public Object getListStoryByChannel(HashMap<String, String> params) {
        logger.debug("getListStoryByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 2;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListStoryByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setListened("0");
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            if (array_new.isEmpty()) {
                                for (int i = 0, n = array_listened.size(); i < n; i++) {
                                    Content item = array_listened.get(i);
                                    String a = ListenHistory.SEPERATOR + item.getContentId()
                                            + ListenHistory.SEPERATOR;
                                    if (contentListened.endsWith(a)) {
                                        r.setLastPartNumber(item.getContentId());
                                        break;
                                    }
                                }
                            } else {
                                r.setLastPartNumber("0");
                            }
                            array_new.addAll(array_listened);
                            r.setListened("1");
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListStoryByChannel." + topicType + "." + topicOrd + "." + channelOrd + "." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "story_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListStoryMultiPartByChannel(HashMap<String, String> params) {
        logger.debug("getListStoryMultiPartByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 2;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListStoryMultiPartByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    // Goi api ben CN
                    int play_limit = Integer.parseInt(String.valueOf(resp.get("play_limit")));
                    String url = ConfigStack.getConfig("api_ranking", "ranking_url", "") + "/" + msisdn + "?category_id=" + String.valueOf(resp.get("channel_id")) + "&top_n=" + play_limit;
                    String ccuResp = null;
                    try {
                        //ccuResp = httpRequest.getRequest(url);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                    if (ccuResp != null) {
                        ListStory listStory = objectMapper.readValue(ccuResp, ListStory.class);
                        StoryInfo[] oldLists = listStory.getOldStories();
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ArrayList<Content> array_listened = new ArrayList<Content>();
                        // Lay danh sach da nghe
                        if (oldLists != null && oldLists.length > 0) {
                            for (int j = 0; j < oldLists.length; j++) {
                                StoryInfo storyInfo = oldLists[j];
                                for (int i = 0; i < array.size(); i++) {
                                    Content item = array.get(i);
                                    if (Long.parseLong(item.getContentId()) == storyInfo.getStoryId()) {
                                        array_listened.add(item);
                                        break;
                                    }
                                }
                            }
                        }
                        // Lay danh sach chua nghe
                        for (int i = 0, n = array.size(); i < n; i++) {
                            Content item = array.get(i);
                            if (oldLists != null && oldLists.length > 0) {
                                boolean found = false;
                                for (int j = 0; j < oldLists.length; j++) {
                                    StoryInfo storyInfo = oldLists[j];
                                    if (Long.parseLong(item.getContentId()) == storyInfo.getStoryId()) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    array_new.add(item);
                                }
                            } else {
                                array_new.add(item);
                            }
                        }
                        ArrayList<Content> array_all = new ArrayList<Content>();
                        int size = array_new.size();
                        if (size < array_listened.size()) {
                            size = array_listened.size();
                        }
                        for (int i = 0; i < size; i++) {
                            if (i < array_listened.size()) {
                                array_all.add(array_listened.get(i));
                            }
                            if (i < array_new.size()) {
                                array_all.add(array_new.get(i));
                            }
                        }
                        Content[] data = new Content[array_all.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_all.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Neu khong goi dc thi lay trong DB
                        ListenHistory listenHis = db.getListenHistory(msisdn, topicOrd, topicType);
                        if (listenHis != null) {
                            ArrayList<Content> array_new = new ArrayList<Content>();
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR
                                        + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            // Sap xep danh sach da nghe
                            ArrayList<Content> array_listened_full = new ArrayList<Content>();
                            ArrayList<Content> array_listened_not_full = new ArrayList<Content>();
                            int channel_id = 0;
                            listenHis = db.getListenHistory(msisdn, channel_id, topicType);
                            if (listenHis != null) {
                                contentListened = listenHis.getContentListened() == null
                                        || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                                for (int i = 0; i < array.size(); i++) {
                                    Content item = array_listened.get(i);
                                    String a = ListenHistory.SEPERATOR
                                            + item.getContentId()
                                            + ":0";
                                    if (contentListened.contains(a)) {
                                        array_listened_full.add(item);
                                    } else {
                                        array_listened_not_full.add(item);
                                    }
                                }
                            }
                            array_listened_not_full.addAll(array_listened_full);
                            ArrayList<Content> array_all = new ArrayList<Content>();
                            int size = array_listened_not_full.size();
                            if (size < array_new.size()) {
                                size = array_new.size();
                            }
                            for (int i = 0; i < size; i++) {
                                if (i < array_listened_not_full.size()) {
                                    array_all.add(array_listened_not_full.get(i));
                                }
                                if (i < array_new.size()) {
                                    array_all.add(array_new.get(i));
                                }
                            }
                            Content[] data = new Content[array_all.size()];
                            for (int i = 0; i < data.length; i++) {
                                data[i] = array_all.get(i);
                            }
                            r.setData(data);
                        } else {
                            Content[] data = new Content[array.size()];
                            for (int i = 0; i < data.length; i++) {
                                data[i] = array.get(i);
                            }
                            r.setData(data);
                        }
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getStoryMultiPart(HashMap<String, String> params) {
        logger.debug("getStoryMultiPart");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int contentId = Integer.parseInt(params.get("contentId"));
            int scriptType = Integer.parseInt(params.get("scriptType"));
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getStoryMultiPart(contentId);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setTotalPart(String.valueOf(resp.get("total_part")));
                    r.setTotal(String.valueOf(array.size()));
                    if (resp.get("summary_story_path") != null) {
                        r.setSummaryStoryPath(String.valueOf(resp.get("summary_story_path")));
                    }
                    int topic_type = 2;
                    int channel_id = 0;
                    ListenHistory listenHis = db.getListenHistory(msisdn, channel_id, topic_type);
                    boolean found = false;
                    if (listenHis != null) {
                        String contentListened = listenHis.getContentListened() == null
                                || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();

                        for (int i = 0; i < array.size(); i++) {
                            Content item = array.get(i);
                            String a = ListenHistory.SEPERATOR
                                    + contentId
                                    + ":" + item.getPartNumber() + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                found = true;
                                r.setLastPartNumber(item.getPartNumber());
                                break;
                            }
                        }
                        String a = ListenHistory.SEPERATOR
                                + contentId
                                + ":0" + ListenHistory.SEPERATOR;
                        if (contentListened.contains(a)) {
                            found = true;
                            r.setLastPartNumber("0");
                        }
                    }
                    if (!found) {
                        r.setLastPartNumber("-1");
                    }
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        Content item = array.get(i);
                        item.setDuration(null);
                        data[i] = item;
                    }
                    r.setData(data);
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getStoryMultiPart." + msisdn + "." + scriptType;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "story_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getStoryByCode(HashMap<String, String> params) {
        logger.debug("getStoryByCode");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int code = Integer.parseInt(params.get("code"));
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getStoryByCode(code);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setTotalPart(String.valueOf(resp.get("total_part")));
                    if (resp.get("summary_path") != null) {
                        r.setSummaryStoryPath(String.valueOf(resp.get("summary_path")));
                    }
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setContentPath(String.valueOf(resp.get("content_path")));
                    r.setDuration(String.valueOf(resp.get("duration")));
                    r.setContentId(String.valueOf(resp.get("content_id")));
                    if (array != null) {
                        r.setTotal(String.valueOf(array.size()));
                        int topic_type = 2;
                        int channel_id = 0;
                        ListenHistory listenHis = db.getListenHistory(msisdn, channel_id, topic_type);
                        boolean found = false;
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();

                            for (int i = 0; i < array.size(); i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR
                                        + r.getContentId()
                                        + ":" + item.getPartNumber() + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    found = true;
                                    r.setLastPartNumber(item.getPartNumber());
                                    break;
                                }
                            }
                            String a = ListenHistory.SEPERATOR
                                    + r.getContentId()
                                    + ":0" + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                found = true;
                                r.setLastPartNumber("0");
                            }
                        }
                        if (!found) {
                            r.setLastPartNumber("-1");
                        }
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            Content item = array.get(i);
                            item.setDuration(null);
                            data[i] = item;
                        }
                        r.setData(data);
                    } else {
                        r.setLastPartNumber("-1");
                        r.setTotal(String.valueOf(resp.get("total_part")));
                    }
                    // Check 7 ngay o cho nay
                    int index = 0;
                    if (Integer.parseInt(r.getTotalPart()) > 1) {
                        index = 1;
                    }
                    String key = this.getClass().getCanonicalName() + ".getStoryByCode." + msisdn + "." + index;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "story_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object addStoryIntoAlbum(HashMap<String, String> params) {
        logger.debug("addStoryIntoAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentId = params.get("contentId");
            String contentType = params.get("contentType");
            String packageId = params.get("packageId");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.addStoryIntoAlbum(msisdn, contentId, contentType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                if (errorCode.equals(Constants.SUCCESS)) {
                    // Tra tin nhan
                    String mt = ConfigStack.getConfig("mt", "mt_add_story_to_album", "");
                    int mtType = SMSType.Genral.getValue();
                    String contentName = String.valueOf(resp.get("content_name"));
                    String code = String.valueOf(resp.get("code"));
                    mt = Helper.prepaidContent(mt, "", "", "", contentName, code, "", "", "", "", "", "", "", "");
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setPackageId(Integer.parseInt(packageId));
                    sms.setSource("IVR");
                    sms.setAction("ADD_TO_ALBUM");
                    sendMT(sms);
                    // Call to ranking
//                    StoryInfo storyInfo = new StoryInfo();
//                    storyInfo.setStoryId(Long.parseLong(contentId));
//                    storyInfo.setUserId(Long.parseLong(msisdn));
//                    ra.getEngine().getThreadPool("call_to_ranking_pool").submit(callAddAlbumToRanking(storyInfo));
                }
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.info(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object removeStoryFromAlbum(HashMap<String, String> params) {
        logger.debug("removeStoryFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentId = params.get("contentId");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.removeStoryFromAlbum(msisdn, contentId);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object checkStoryContentTypeFromAlbum(HashMap<String, String> params) {
        logger.debug("checkStoryContentTypeFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.checkStoryContentTypeFromAlbum(msisdn);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                String contentType = String.valueOf(resp.get("content_type"));
                r.setErrorCode(errorCode);
                r.setContentType(contentType);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListStoryFromAlbum(HashMap<String, String> params) {
        logger.debug("getListStoryFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentType = params.get("contentType");
            HashMap<String, Object> resp = db.getListStoryFromAlbum(msisdn, contentType);
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            r.setErrorCode(errorCode);
            if (errorCode.equals(Constants.SUCCESS)) {
                ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                r.setTotal(String.valueOf(array.size()));
                Content[] data = new Content[array.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = array.get(i);
                }
                r.setData(data);
                // Check 7 ngay o cho nay
                String key = this.getClass().getCanonicalName() + ".getListStoryFromAlbum." + msisdn + "." + contentType;
                String value = getFromCache(key);
                if (value == null) {
                    r.setPromtListen("0");
                } else {
                    r.setPromtListen("1");
                }
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "story_day_for_check_promt", "7")) * 24 * 60 * 60;
                pushToCacheWithExpiredTime(key, msisdn, timeOut);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // Fun
    public Object getListFunByChannel(HashMap<String, String> params) {
        logger.debug("getListFunByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 4;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListFunByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    r.setListened("0");
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            if (array_new.isEmpty()) {
                                for (int i = 0, n = array_listened.size(); i < n; i++) {
                                    Content item = array_listened.get(i);
                                    String a = ListenHistory.SEPERATOR + item.getContentId()
                                            + ListenHistory.SEPERATOR;
                                    if (contentListened.endsWith(a)) {
                                        r.setLastPartNumber(item.getContentId());
                                        break;
                                    }
                                }
                            } else {
                                r.setLastPartNumber("0");
                            }
                            array_new.addAll(array_listened);
                            r.setListened("1");
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListFunByChannel." + topicType + "." + topicOrd + "." + channelOrd + "." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "fun_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListFunMultiPartByChannel(HashMap<String, String> params) {
        logger.debug("getListFunMultiPartByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            String topic_Type = params.get("topicType");
            int topicType = 0;
            if ("".equals(topic_Type) || topic_Type == null) {
                topicType = 4;
            } else {
                topicType = Integer.parseInt(topic_Type);
            }
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListFunMultiPartByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    ListenHistory listenHis = db.getListenHistory(msisdn, topicOrd, topicType);
                    if (listenHis != null) {
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        String contentListened = listenHis.getContentListened() == null
                                || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                        ArrayList<Content> array_listened = new ArrayList<Content>();
                        for (int i = 0, n = array.size(); i < n; i++) {
                            Content item = array.get(i);
                            String a = ListenHistory.SEPERATOR
                                    + item.getContentId()
                                    + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                array_listened.add(item);
                            } else {
                                array_new.add(item);
                            }
                        }
                        // Sap xep danh sach da nghe
                        ArrayList<Content> array_listened_full = new ArrayList<Content>();
                        ArrayList<Content> array_listened_not_full = new ArrayList<Content>();
                        int channel_id = 0;
                        listenHis = db.getListenHistory(msisdn, channel_id, topicType);
                        if (listenHis != null) {
                            contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                            for (int i = 0; i < array.size(); i++) {
                                Content item = array_listened.get(i);
                                String a = ListenHistory.SEPERATOR
                                        + item.getContentId()
                                        + ":0";
                                if (contentListened.contains(a)) {
                                    array_listened_full.add(item);
                                } else {
                                    array_listened_not_full.add(item);
                                }
                            }
                        }
                        array_listened_not_full.addAll(array_listened_full);
                        ArrayList<Content> array_all = new ArrayList<Content>();
                        int size = array_listened_not_full.size();
                        if (size < array_new.size()) {
                            size = array_new.size();
                        }
                        for (int i = 0; i < size; i++) {
                            if (i < array_listened_not_full.size()) {
                                array_all.add(array_listened_not_full.get(i));
                            }
                            if (i < array_new.size()) {
                                array_all.add(array_new.get(i));
                            }
                        }
                        Content[] data = new Content[array_all.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_all.get(i);
                        }
                        r.setData(data);
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getFunMultiPart(HashMap<String, String> params) {
        logger.debug("getFunMultiPart");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int contentId = Integer.parseInt(params.get("contentId"));
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getFunMultiPart(contentId);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setTotalPart(String.valueOf(resp.get("total_part")));
                    r.setTotal(String.valueOf(array.size()));
                    int topic_type = 4;
                    int channel_id = 0;
                    ListenHistory listenHis = db.getListenHistory(msisdn, channel_id, topic_type);
                    boolean found = false;
                    if (listenHis != null) {
                        String contentListened = listenHis.getContentListened() == null
                                || listenHis.getContentListened().equalsIgnoreCase("") ? "" : listenHis.getContentListened();
                        for (int i = 0; i < array.size(); i++) {
                            Content item = array.get(i);
                            String a = ListenHistory.SEPERATOR
                                    + contentId
                                    + ":" + item.getPartNumber() + ListenHistory.SEPERATOR;
                            if (contentListened.contains(a)) {
                                found = true;
                                r.setLastPartNumber(item.getPartNumber());
                                break;
                            }
                        }
                        String a = ListenHistory.SEPERATOR
                                + contentId
                                + ":0" + ListenHistory.SEPERATOR;
                        if (contentListened.contains(a)) {
                            found = true;
                            r.setLastPartNumber("0");
                        }
                    }
                    if (!found) {
                        r.setLastPartNumber("-1");
                    }
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                    String key = this.getClass().getCanonicalName() + ".getFunMultiPart." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "fun_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object addFunIntoAlbum(HashMap<String, String> params) {
        logger.debug("addFunIntoAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentId = params.get("contentId");
            String partNumber = params.get("partNumber");
            String packageId = params.get("packageId");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.addFunIntoAlbum(msisdn, contentId, partNumber);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                if (errorCode.equals(Constants.SUCCESS)) {
                    // Tra tin nhan
                    String mt = ConfigStack.getConfig("mt", "mt_add_fun_to_album", "");
                    String contentName = String.valueOf(resp.get("content_name"));
                    mt = Helper.prepaidContent(mt, "", "", "", contentName, "", "", "", "", "", "", "", "", "");
                    int mtType = SMSType.Genral.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setPackageId(Integer.parseInt(packageId));
                    sms.setSource("IVR");
                    sms.setAction("ADD_TO_ALBUM");
                    // sendMT(sms);
                }
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object removeFunFromAlbum(HashMap<String, String> params) {
        logger.debug("removeFunFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String contentId = params.get("contentId");
            String partNumber = params.get("partNumber");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.removeFunFromAlbum(msisdn, contentId, partNumber);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListFunFromAlbum(HashMap<String, String> params) {
        logger.debug("getListFunFromAlbum");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            HashMap<String, Object> resp = db.getListFunFromAlbum(msisdn);
            String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
            r.setErrorCode(errorCode);
            if (errorCode.equals(Constants.SUCCESS)) {
                ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                r.setTotal(String.valueOf(array.size()));
                Content[] data = new Content[array.size()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = array.get(i);
                }
                r.setData(data);
                String key = this.getClass().getCanonicalName() + ".getListFunFromAlbum." + msisdn;
                String value = getFromCache(key);
                if (value == null) {
                    r.setPromtListen("0");
                } else {
                    r.setPromtListen("1");
                }
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "fun_day_for_check_promt", "7")) * 24 * 60 * 60;
                pushToCacheWithExpiredTime(key, msisdn, timeOut);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // Horoscope
    public Object getHoroByDate(HashMap<String, String> params) {
        logger.debug("getHoroByDate");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String str_date = params.get("date");
            int topicType = 5;
            int lengtgDate = str_date.trim().length();
            if (msisdn != null && str_date != null && lengtgDate == 4) {
                Calendar date = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("ddMM");
                sdf.setLenient(false);
                try {
                    Date d = sdf.parse(str_date);
                    date.setTime(d);
                } catch (Exception e) {
                    r.setErrorCode("1");
                    return r;
                }
                int type = -1;
                ArrayList<HoroscopeInDateRange> listConfig = db.getListHoroScopeInDateRange();
                for (int i = 0; i < listConfig.size(); i++) {
                    if (listConfig.get(i).checkInDateRange(date)) {
                        type = listConfig.get(i).getType();
                        break;
                    }
                }
                HashMap<String, Object> resp = db.getHoroByDate(type, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setChannelOrd(String.valueOf(resp.get("channel_ord")));
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setContentPath(String.valueOf(resp.get("content_path")));
                    r.setContentId(String.valueOf(resp.get("content_id")));
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListHoroByChannel(HashMap<String, String> params) {
        logger.debug("getListHoroByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 5;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListHoroByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            array_new.addAll(array_listened);
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListHoroByChannel." + topicOrd + "." + channelOrd + "." + topicType;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "horo_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // Calendar
    public Object getCalendarByDate(HashMap<String, String> params) {
        logger.debug("getCalendarByDate");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String str_date = params.get("date");
            String channelOrd = params.get("channelOrd");
            int topicType = 5;
            int lengtgDate = str_date.trim().length();
            if (msisdn != null && str_date != null && channelOrd != null && lengtgDate == 4) {
                Calendar date = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("ddMM");
                sdf.setLenient(false);
                try {
                    Date d = sdf.parse(str_date);
                    date.setTime(d);
                } catch (Exception e) {
                    r.setErrorCode("1");
                    return r;
                }

                /*
                 * Thang thap nhat cho phep xem lich van su nam tiep theo
                 */
                int minimumMonthAllowViewNextYear = Helper.getInt(ConfigStack.getConfig("horo", "min_month_view_next_year", "10"));
                /*
                 * Thang toi da cua nam tiep theo duoc phep xem lich van su
                 */
                int maximumMonthViewNextYear = Helper.getInt(ConfigStack.getConfig("horo", "max_month_view_next_year", "6"));

                int viewYear = Calendar.getInstance().get(Calendar.YEAR);
                if (Calendar.getInstance().get(Calendar.MONTH) >= minimumMonthAllowViewNextYear && date.get(Calendar.MONTH) <= maximumMonthViewNextYear) {
                    /*
                     * Xem cua nam tiep theo
                     */
                    viewYear++;
                }

                String name = sdf.format(date.getTime()) + String.valueOf(viewYear);
                HashMap<String, Object> resp = db.getCalendarByDate(name, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setChannelOrd(String.valueOf(resp.get("channel_ord")));
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setContentPath(String.valueOf(resp.get("content_path")));
                    r.setContentId(String.valueOf(resp.get("content_id")));
                    r.setDesc(String.valueOf(viewYear));
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getHoroByPhongThuy(HashMap<String, String> params) {
        logger.debug("getHoroByPhongThuy");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String channelOrd = params.get("channelOrd");
            String name = params.get("name");
            int topicType = 5;
            if (msisdn != null && name != null && channelOrd != null) {
                HashMap<String, Object> resp = db.getCalendarByPhongThuy(name, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setTotal(String.valueOf(array.size()));
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                }
                // Check 7 ngay o cho nay
                String key = this.getClass().getCanonicalName() + ".getCalendarByDate." + channelOrd + "." + topicType;
                String value = getFromCache(key);
                if (value == null) {
                    r.setPromtListen("0");
                } else {
                    r.setPromtListen("1");
                }
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "horo_day_for_check_promt", "7")) * 24 * 60 * 60;
                pushToCacheWithExpiredTime(key, msisdn, timeOut);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getHoroByName(HashMap<String, String> params) {
        logger.debug("getHoroByName");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String channelOrd = params.get("channelOrd");
            String name = params.get("name");
            int topicType = 5;
            if (msisdn != null && name != null && channelOrd != null) {
                HashMap<String, Object> resp = db.getCalendarByDate(name, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setChannelOrd(String.valueOf(resp.get("channel_ord")));
                    r.setNamePath(String.valueOf(resp.get("name_path")));
                    r.setContentPath(String.valueOf(resp.get("content_path")));
                    r.setContentId(String.valueOf(resp.get("content_id")));
                }
                // Check 7 ngay o cho nay
                String key = this.getClass().getCanonicalName() + ".getCalendarByDate." + name + "." + channelOrd + "." + topicType;
                String value = getFromCache(key);
                if (value == null) {
                    r.setPromtListen("0");
                } else {
                    r.setPromtListen("1");
                }
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "horo_day_for_check_promt", "7")) * 24 * 60 * 60;
                pushToCacheWithExpiredTime(key, msisdn, timeOut);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getNumberOfYearHoroscope(HashMap<String, String> params) {
        Result result = new Result();
        result.setErrorCode(Constants.SUCCESS);
        result.setDesc(String.valueOf(Helper.getInt(ConfigStack.getConfig("horo", "number_year_horoscope", "60"), 60)));

        return result;
    }

    // News
    public Object getListNewsByChannel(HashMap<String, String> params) {
        logger.debug("getListNewsByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int checkAccessHealthGame = Helper.getInt(params.get("checkAccessHealthGame"), 0);
            int topicType = 3;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListNewsByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    /*
                     * Kiem tra dieu kien choi game Suc khoe
                     */
                    int enableStatus = Helper.getInt(ConfigStack.getConfig("game_health", "enable", "1"), 1);
                    if (checkAccessHealthGame == 1 && enableStatus == 1) {
                        int questionCount = gameStack.getGameDAO().checkAccessGame(msisdn, Constants.GAME_KEY_HEALTH);
                        if (questionCount >= 0) {
                            r.setCanPlayHealthGame("1");

                            //Loai bo cac cau hoi da tra loi
                            String questionList = gameStack.getGameDAO().getAnsweredQuestionList(msisdn, Constants.GAME_KEY_HEALTH);

                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String keyQuestion = "-" + item.getContentId() + "-";
                                if (questionList.contains(keyQuestion)) {
                                    item.setG_question_path(null);
                                    item.setG_answer_keys(null);
                                    item.setG_right_key(null);
                                    item.setG_right_path(null);
                                    item.setG_wrong_path(null);

                                    array.set(i, item);
                                }
                            }
                        }
                    }
                    logger.debug(">>>>>  :" + play_type);
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            logger.debug("contentListened :" + contentListened);
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                logger.debug(">>>>> a :" + a);
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            array_new.addAll(array_listened);
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }

                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListNewsByChannel." + topicType + "." + topicOrd + "." + channelOrd + "." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "news_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListNewsMultiPart(HashMap<String, String> params) {
        Result result = new Result();

        String msisdn = params.get("msisdn");
        int topicOrd = Helper.getInt(params.get("topicOrd"), 0);
        int channelOrd = Helper.getInt(params.get("channelOrd"), 0);
        int topicType = Constants.NEW_TOPIC_TYPE;
        if (msisdn != null && topicOrd > 0 && channelOrd > 0) {
            ArrayList<Content> resp = this.getListNewsMultiPart(msisdn, topicOrd, channelOrd, topicType);
            logger.info("resp size: " + resp.size());
            if (resp == null || resp.isEmpty()) {
                result.setErrorCode(Constants.NO_DATA_FOUND);
            } else {
                Content[] newsData = new Content[resp.size()];
                for (int i = 0, n = resp.size(); i < n; i++) {
                    newsData[i] = resp.get(i);
                }

                result.setData(newsData);
                result.setChannelId(String.valueOf(resp.get(0).getChannelId()));
                result.setTotal(String.valueOf(resp.size()));
                result.setErrorCode(Constants.SUCCESS);
            }
        } else {
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }
    
    

    public ArrayList<Content> getListNewsMultiPart(String msisdn, int topicOrd,int channelOrd, int topicType) {
        ArrayList<Content> listNews = new ArrayList<Content>();
        if (msisdn == null || msisdn.equals("") || topicOrd <= 0 || channelOrd <= 0) {
            return listNews;
        }

        ArrayList<Content> news = db.getNewsMultiPartByChannel(channelOrd, topicOrd, topicType);
        logger.info("news size: " + news.size());
        if (news.size() > 0) {
            int channelId = Integer.parseInt(news.get(0).getChannelId());
            ListenHistory listenHis = db.getListenHistory(msisdn, channelId, ListenHistory.CONTENT_TYPE_NEWS);
            logger.info("news size: ");
            String historyListened = "";
            if (listenHis != null) {
                historyListened = Helper.isNull(listenHis.getContentListened()) ? "" : listenHis.getContentListened();
            }
            logger.info("news size: ");

            ArrayList<Content> listNotListen = new ArrayList<Content>();
            ArrayList<Content> listListened = new ArrayList<Content>();

            for (int i = 0, n = news.size(); i < n; i++) {
                Content item = news.get(i);
                String a = ListenHistory.SEPERATOR + item.getContentId() + ":";
                logger.info("AAAAA a= " + a);
                if (historyListened.contains(a)) {
                    listListened.add(item);
                    logger.info("ADD listListened a= " + a);
                } else {
                    listNotListen.add(item);
                    logger.info("ADD listNotListened a= " + a);
                }
            }
            logger.info("news size: " + news.size());
            // Sap xep danh sach da nghe
            ArrayList<Content> listListenedFull = new ArrayList<Content>();

            for (int i = 0; i < listListened.size(); i++) {
                Content item = listListened.get(i);
                String a = ListenHistory.SEPERATOR + item.getContentId() + ":" + item.getTotalPart();
                if (historyListened.contains(a)) {
                    listListenedFull.add(item);
                    logger.info("ADD listListenedFull a= " + a);
                } else {
                    logger.info("ADD listNews a= " + a);
                    listNews.add(item);
                }
            }
            logger.info("news size: " + news.size());
            /*
             * Sap xep theo thu tu
             * - CT dang nghe do
             * - CT chua nghe
             * - CT da nghe full
             */
            listNews.addAll(listNotListen);
            listNews.addAll(listListenedFull);
        }

        return listNews;
    }

    public Object getNewsMultiPart(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        int contentId = Helper.getInt(params.get("contentId"), 0);
        int channelId = Helper.getInt(params.get("channelId"), 0);

        if (msisdn != null && contentId > 0 && channelId > 0) {
            ArrayList<Content> partsOfNews = db.getListPartOfNews(contentId);
            logger.debug(">>>>>>>>>>> partsOfNews :" + partsOfNews.size());
            if (partsOfNews == null || partsOfNews.isEmpty()) {
                r.setErrorCode(Constants.SYSTEM_ERROR);
                return r;
            }

//            r.setTotalPart(String.valueOf(partsOfNews.get(0).getTotalPart()));
//            r.setNamePath(partsOfNews.get(0).getNamePath());
            r.setIndex("0");

            int contentType = ListenHistory.CONTENT_TYPE_NEWS;
            ListenHistory listenHis = db.getListenHistory(msisdn, channelId, contentType);
            String contentListened = (listenHis == null || Helper.isNull(listenHis.getContentListened())) ? "" : listenHis.getContentListened();
            //Lay tap da nghe gan nhat
            if (contentListened.length() > 0) {
                String a = ListenHistory.SEPERATOR + contentId + ":0" + ListenHistory.SEPERATOR;
                logger.info("AAAAAA bien a: " + a);
                if (contentListened.contains(a)) {
                    r.setIndex("0");
                    logger.info("AAAAAA bien a: " + a);
                } else {
                    for (int i = 0; i < partsOfNews.size(); i++) {
                        Content item = partsOfNews.get(i);
                        a = ListenHistory.SEPERATOR + contentId + ":" + item.getPartID() + ListenHistory.SEPERATOR;
                        logger.info("AAAAAA bien a: " + a);
                        if (contentListened.contains(a)) {
                            r.setIndex(item.getPartID());
                            break;
                        }
                    }
                }
            }

            Content[] data = new Content[partsOfNews.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = partsOfNews.get(i);
            }

            r.setData(data);
            r.setTotal(String.valueOf(data.length));
            r.setErrorCode(Constants.SUCCESS);

            /*
             * Promt option
             */
            String keyCache = "getNewsMultiPart." + msisdn;
            String showPromtStatus = getFromCache(keyCache);
            if (showPromtStatus == null) {
                showPromtStatus = "1";
                int day = Helper.getInt(ConfigStack.getConfig("api", "news_day_show_promt", "2"));
                pushToCacheWithExpiredTime(keyCache, showPromtStatus, day * 24 * 60 * 60);

                r.setPromtListen(showPromtStatus);
            } else {
                r.setPromtListen("0");
            }

        } else {
            r.setErrorCode(Constants.WRONG_PARAM);
        }

        return r;
    }

    public ArrayList<PackageInfo> getListPackage(HashMap<String, String> params) {
        return db.getListPackage();
    }

    private Callable<Integer> callAddHistoryToRanking(final StoryInfo object) {
        Callable<Integer> task = new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                String url = ConfigStack.getConfig("api_ranking", "ranking_url", "") + "/user_exit_listening";
                try {
                    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    String input = ow.writeValueAsString(object);
                    httpRequest.postRequest(url, input);
                } catch (Exception e) {
                    logger.error(e);
                }
                return 0;
            }
        };
        return task;
    }

    private Callable<Integer> callAddAlbumToRanking(final StoryInfo object) {
        Callable<Integer> task = new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                String url = ConfigStack.getConfig("api_ranking", "ranking_url", "") + "/user_add_bst";
                try {
                    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                    String input = ow.writeValueAsString(object);
                    httpRequest.postRequest(url, input);
                } catch (Exception e) {
                    logger.error(e);
                }
                return 0;
            }
        };
        return task;
    }

    // News Sport
    public Object getListMatchByChannel(HashMap<String, String> params) {
        logger.debug("getListMatchByChannel");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 6;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListMatchByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            array_new.addAll(array_listened);
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListMatchByChannel." + topicType + "." + topicOrd + "." + channelOrd + "." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "sport_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListSport(HashMap<String, String> params) {
        logger.debug("getListSport");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int topicOrd = Integer.parseInt(params.get("topicOrd"));
            int channelOrd = Integer.parseInt(params.get("channelOrd"));
            int topicType = 6;
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getListMatchByChannel(topicOrd, channelOrd, topicType);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    int play_type = Integer.parseInt(resp.get("play_type").toString());
                    r.setPlayType(play_type);
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    if (play_type == 0) {
                        // Phat theo chi so
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                    } else if (play_type == 1) {
                        // Phat theo chi so + lish su
                        ArrayList<Content> array_new = new ArrayList<Content>();
                        ListenHistory listenHis = db.getListenHistory(msisdn, Integer.parseInt(String.valueOf(resp.get("channel_id"))), topicType);
                        if (listenHis != null) {
                            String contentListened = listenHis.getContentListened() == null
                                    || listenHis.getContentListened().equalsIgnoreCase("") ? ""
                                            : listenHis.getContentListened();
                            ArrayList<Content> array_listened = new ArrayList<Content>();
                            for (int i = 0, n = array.size(); i < n; i++) {
                                Content item = array.get(i);
                                String a = ListenHistory.SEPERATOR + item.getContentId()
                                        + ListenHistory.SEPERATOR;
                                if (contentListened.contains(a)) {
                                    array_listened.add(item);
                                } else {
                                    array_new.add(item);
                                }
                            }
                            array_new.addAll(array_listened);
                        } else {
                            array_new.addAll(array);
                        }
                        Content[] data = new Content[array_new.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array_new.get(i);
                        }
                        r.setData(data);
                    } else {
                        // Random
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData((Content[]) Helper.shuffleList(data));
                    }
                    r.setChannelId(String.valueOf(resp.get("channel_id")));
                    r.setTotal(String.valueOf(array.size()));
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getListSport." + topicType + "." + topicOrd + "." + channelOrd + "." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "sport_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getContentMatch(HashMap<String, String> params) {
        logger.debug("getContentMatch");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            int contentId = Integer.parseInt(params.get("contentId"));
            if (msisdn != null) {
                HashMap<String, Object> resp = db.getContentMatch(contentId);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
                if (errorCode.equals(Constants.SUCCESS)) {
                    ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                    r.setTotal(String.valueOf(array.size()));
                    Content[] data = new Content[array.size()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.get(i);
                    }
                    r.setData(data);
                    // Check 7 ngay o cho nay
                    String key = this.getClass().getCanonicalName() + ".getContentMatch." + msisdn;
                    String value = getFromCache(key);
                    if (value == null) {
                        r.setPromtListen("0");
                    } else {
                        r.setPromtListen("1");
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "sport_day_for_check_promt", "7")) * 24 * 60 * 60;
                    pushToCacheWithExpiredTime(key, msisdn, timeOut);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // Qua tang
    public Object checkGift(HashMap<String, String> params) {
        logger.debug("checkGift");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            if (msisdn != null) {
                HashMap<String, Object> resp = db.checkGift(msisdn);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object checkMsisdn(HashMap<String, String> params) {
        logger.debug("checkMsisdn");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String receiver = params.get("receiver");
            receiver = Helper.formatMobileNumber(receiver);
            if (msisdn != null && receiver != null) {
                if (Helper.isMobileNumber(receiver)) {
                    User user = db.checkMsisdnReceiveGift(msisdn, receiver, false);
                    if (user.getCode() == BillingErrorCode.Success.getValue()) {
                        r.setErrorCode(Constants.SUCCESS);
                    } else {
                        r.setErrorCode(Constants.NO_DATA_FOUND);
                    }
                } else {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Result sendGuideGiftSMS(HashMap<String, String> params) {
        Result r = new Result();
        String msisdn = params.get("msisdn");
        String source = params.get("source");
        String packageId = params.get("packageId");
        if (msisdn == null || source == null || packageId == null) {
            r.setErrorCode(Constants.WRONG_PARAM);
            return r;
        }
        int mtType = SMSType.Genral.getValue();
        String mt = ConfigStack.getConfig("mt", "mt_guide_send_gift", "");
        mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");

        SMS sms = new SMS();
        sms.setMtContent(mt);
        sms.setMsisdn(msisdn);
        sms.setType(mtType);
        sms.setHaveMO(false);
        sms.setAction("HELP_SEND_GIFT");
        sms.setSource(source);
        sms.setPackageId(Integer.parseInt(packageId));
        sendMT(sms);
        r.setErrorCode(Constants.SUCCESS);
        return r;
    }

    public Object getListNewGift(HashMap<String, String> params) {
        logger.debug("getListNewGift");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String str_topic_type = params.get("topicType");
            if (str_topic_type == null) {
                str_topic_type = "1";
            }
            int topic_type = Integer.parseInt(str_topic_type);
            if (msisdn != null) {
                HashMap<String, Object> resp = null;
                if (topic_type == 1) {
                    resp = db.getListNewGiftMusic(msisdn);
                }
                if (resp != null) {
                    String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                    r.setErrorCode(errorCode);
                    if (errorCode.equals(Constants.SUCCESS)) {
                        ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                        r.setTotal(String.valueOf(array.size()));
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                        com.vega.service.api.object.SubPackageInfo sub = db.checkSubPackage(msisdn);
                        if (sub.getErrorCode().equals(BillingErrorCode.Success)) {
                            r.setIsSub("0");
                        } else {
                            r.setIsSub("1");
                        }
                        // Check 7 ngay o cho nay
                        String key = this.getClass().getCanonicalName() + ".getListNewGift." + topic_type + "." + msisdn;
                        String value = getFromCache(key);
                        if (value == null) {
                            r.setPromtListen("0");
                        } else {
                            r.setPromtListen("1");
                        }
                        int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "new_gift_day_for_check_promt", "7")) * 24 * 60 * 60;
                        pushToCacheWithExpiredTime(key, msisdn, timeOut);
                    }
                } else {
                    r.setErrorCode(Constants.WRONG_PARAM);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object checkDayForGift(HashMap<String, String> params) {
        logger.debug("checkDayForGift");
        Result r = new Result();
        try {
            String str_date = params.get("date");
            if (str_date != null && str_date.length() == 4) {
                Calendar date = Calendar.getInstance();
                Calendar date_now = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
                SimpleDateFormat sdf_year = new SimpleDateFormat("yyyy");
                sdf.setLenient(false);
                try {
                    Date d = sdf.parse(str_date + sdf_year.format(date_now.getTime()) + "235959");
                    date.setTime(d);
                    if (date.before(date_now)) {
                        if (!date.equals(date_now)) {
                            // Hen gio nam sau
                            date.add(Calendar.YEAR, 1);
                        }
                    }
                    r.setYear(sdf_year.format(date.getTime()));
                    r.setErrorCode(Constants.SUCCESS);
                } catch (Exception e) {
                    r.setErrorCode(Constants.NO_DATA_FOUND);
                    return r;
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object updateHistoryGift(HashMap<String, String> params) {
        logger.debug("updateHistoryGift");
        Result r = new Result();
        try {
            String historyGift = params.get("historyGift");
            if (historyGift != null) {
                String[] info = historyGift.split("-");
                if (info.length >= 4) {
                    String callType = info[0];
                    String giftId = info[1];
                    String listenFull = info[2];
                    String duration = info[3];
                    db.updateGiftSuccess(callType, giftId, listenFull, duration);
                    r.setErrorCode(Constants.SUCCESS);
                } else {
                    r.setErrorCode(Constants.WRONG_PARAM);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object getListGift(HashMap<String, String> params) {
        logger.debug("getListGift");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String str_topic_type = params.get("topicType");
            if (str_topic_type == null) {
                str_topic_type = "1";
            }
            int topic_type = Integer.parseInt(str_topic_type);
            if (msisdn != null) {
                HashMap<String, Object> resp = null;
                if (topic_type == 1) {
                    resp = db.getListGiftMusic(msisdn);
                }
                if (resp != null) {
                    String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                    r.setErrorCode(errorCode);
                    if (errorCode.equals(Constants.SUCCESS)) {
                        ArrayList<Content> array = (ArrayList<Content>) resp.get("data");
                        r.setTotal(String.valueOf(array.size()));
                        Content[] data = new Content[array.size()];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = array.get(i);
                        }
                        r.setData(data);
                        // Check 7 ngay o cho nay
                        String key = this.getClass().getCanonicalName() + ".getListGift." + topic_type + "." + msisdn;
                        String value = getFromCache(key);
                        if (value == null) {
                            r.setPromtListen("0");
                        } else {
                            r.setPromtListen("1");
                        }
                        int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "gift_day_for_check_promt", "7")) * 24 * 60 * 60;
                        pushToCacheWithExpiredTime(key, msisdn, timeOut);
                    }
                } else {
                    r.setErrorCode(Constants.WRONG_PARAM);
                }
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    public Object removeGift(HashMap<String, String> params) {
        logger.debug("removeGift");
        Result r = new Result();
        try {
            String msisdn = params.get("msisdn");
            String code = params.get("code");
            if (msisdn != null && code != null) {
                HashMap<String, Object> resp = db.removeGift(msisdn, code);
                String errorCode = String.valueOf(resp.get(Constants.ERROR_CODE));
                r.setErrorCode(errorCode);
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }

    // script horo 
    public Object getConfigureBranchScript(HashMap<String, String> params) {
        logger.debug("getConfigureBranchScript");
        Result r = new Result();
        try {
            String promt = params.get("promt");
            if (promt != null) {
                r.setErrorCode(Constants.SUCCESS);
                r.setInvalikey(ConfigStack.getConfig("branch_script", promt, ""));
            } else {
                r.setErrorCode(Constants.WRONG_PARAM);
            }
        } catch (Exception ex) {
            logger.error(ex);
            r.setErrorCode(Constants.SYSTEM_ERROR);
        }
        return r;
    }
    /*
     * ============ FRIENDLY ==================== 
     */

    public Object sendGuideChatSMS(HashMap<String, String> params) {
        Result result = new Result();
        result.setErrorCode("-1");

        try {
            String msisdn = params.get("msisdn");
            String userId = params.get("userId");
            String packageId = params.get("packageId");

            if (!Helper.isNull(msisdn) && !Helper.isNull(userId)) {
//				String mt = sms.getSMSConfig("FRIEND_GUIDE_SMS");
//				if(mt != null){
//					mt = mt.replaceAll("\\{ma_so\\}", userId);
//					smppRA.sendMT(mt, msisdn, null, null);
//					result.setErrorCode(Constants.SUCCESS);
//				}
                String mt = ConfigStack.getConfig("mt", "FRIEND_GUIDE_SMS", "");
                mt = mt.replaceAll("\\{ma_so\\}", userId);
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                int mtType = SMSType.Genral.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("SEND_FRIENDLY");
                sms.setSource("");
                sms.setPackageId(Integer.parseInt(packageId));
                sendMT(sms);
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode("-1");
        }

        return result;
    }

    public Object getFriendList(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            String msisdn = params.get("msisdn");
            if (userId > 0) {
                ArrayList<SubProfileInfo> friends = db.getFriendList(userId);
                if (friends != null) {
                    if (friends.size() > 0) {
                        int limit = Helper.getInt(ConfigStack.getConfig("api", "max_friend_list", "300"));
                        limit = friends.size() > limit ? limit : friends.size();

                        SubProfileInfo[] friendData = new SubProfileInfo[limit];
                        for (int i = 0, n = friendData.length; i < n; i++) {
                            friendData[i] = friends.get(i);
                        }

                        result.setFriendList(friendData);
                        result.setTotal(String.valueOf(limit));
                        result.setErrorCode(Constants.SUCCESS);
                        String key = this.getClass().getCanonicalName() + ".getFriendList." + msisdn;
                        String value = getFromCache(key);
                        if (value == null) {
                            result.setPromtListen("0");
                        } else {
                            result.setPromtListen("1");
                        }
                        int timeOut = Integer.parseInt(ConfigStack.getConfig("api", "friend_list_day_for_check_promt", "7")) * 24 * 60 * 60;
                        pushToCacheWithExpiredTime(key, msisdn, timeOut);
                    } else {
                        result.setErrorCode(Constants.NO_DATA_FOUND);
                    }
                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object addToFriendList(HashMap<String, String> params) {
        Result result = new Result();

        try {
            String msisdn = params.get("msisdn");
            String packageId = params.get("packageId");
            int userId = Helper.getInt(params.get("userId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);

            if (userId > 0 && friendUserId > 0) {
                String addResult = db.addToFriendList(userId, friendUserId);
                result.setErrorCode(addResult);
                String mt = ConfigStack.getConfig("mt", "FRIEND_LIST_ADD_SUCCESS", "");
                mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(friendUserId));
                if (addResult.equalsIgnoreCase(Constants.DATA_EXIST)) {
                    mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_EXIST", "");
                }
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                int mtType = SMSType.Genral.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("SEND_FRIENDLY");
                sms.setSource("");
                sms.setPackageId(Integer.parseInt(packageId));
                sendMT(sms);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object removeFromFriendList(HashMap<String, String> params) {
        Result result = new Result();

        try {
            String msisdn = params.get("msisdn");
            //   String packageId = params.get("packageId");
            int userId = Helper.getInt(params.get("userId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);

            if (userId > 0 && friendUserId > 0) {
                String removeResult = db.removeFromFriendList(userId, friendUserId);
                result.setErrorCode(removeResult);

                String mt = ConfigStack.getConfig("mt", "FRIEND_LIST_REMOVE_SUCCESS", "");
                if (removeResult.equalsIgnoreCase(Constants.DATA_EXIST)) {
                    mt = ConfigStack.getConfig("mt", "FRIEND_ID_INVALID_OR_EXIST", "");
                }

                mt = Helper.prepaidContent(mt, "", "", "", "", String.valueOf(friendUserId), "", "", "", "", "", "", "", "");
                int mtType = SMSType.Genral.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("SEND_FRIENDLY");
                sms.setSource("");
                //    sms.setPackageId(Integer.parseInt(packageId));
                sendMT(sms);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object getVoiceMsgList(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            String msisdn = params.get("msisdn");

            if (userId > 0 && !Helper.isNull(msisdn)) {
                int limit = Helper.getInt(ConfigStack.getConfig("api", "max_voice_message_list", "500"));
                ArrayList<SubMessageInfo> messages = db.getVoiceMsgList(userId, limit);
                if (messages != null) {
                    if (messages.size() > 0) {
                        SubMessageInfo[] msgData = new SubMessageInfo[messages.size()];
                        for (int i = 0, n = msgData.length; i < n; i++) {
                            msgData[i] = messages.get(i);
                        }
                        result.setMsgList(msgData);
                        result.setTotal(String.valueOf(msgData.length));
                        // Check 7 ngay guide promt
                        int showGuidePromt = 0;
                        String key = this.getClass().getCanonicalName() + "interact_" + msisdn + "_" + String.valueOf(Constants.INTERACT_ACCESS_MAIL);
                        String value = getFromCache(key);
                        if (value == null) {
                            showGuidePromt = 0;
                        } else {
                            showGuidePromt = 1;
                        }
                        int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
                        pushToCacheWithExpiredTime(key, msisdn, timeOut);
                        result.setDesc(String.valueOf(showGuidePromt));
                        result.setErrorCode(Constants.SUCCESS);
                    } else {
                        result.setErrorCode(Constants.NO_DATA_FOUND);
                    }

                    /*
                     * Check friendlist
                     */
                    int countFriendList = db.countFriendList(userId);
                    result.setCount(String.valueOf(countFriendList));
                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object getVoiceMsgListFromUser(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);

            if (userId > 0 && friendUserId > 0) {

                int limit = Integer.parseInt(ConfigStack.getConfig("api", "max_voice_message_list", "500"));
                ArrayList<SubMessageInfo> messages = db.getVoiceMsgListFromUser(userId, friendUserId, limit);
                if (messages != null) {
                    if (messages.size() > 0) {
                        SubMessageInfo[] msgData = new SubMessageInfo[messages.size()];
                        for (int i = 0, n = msgData.length; i < n; i++) {
                            msgData[i] = messages.get(i);
                        }

                        result.setMsgList(msgData);
                        result.setTotal(String.valueOf(msgData.length));
                        result.setErrorCode(Constants.SUCCESS);
                    } else {
                        result.setErrorCode(Constants.NO_DATA_FOUND);
                    }
                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object checkUserId(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);
            int userId = Helper.getInt(params.get("userId"), 0);

            if (userId > 0 && friendUserId > 0) {
                SubProfileInfo sender = db.getSubProfile("", userId, -1);

                int telcoFilter = isOnlyAllowTestNumberFriends(sender.getMsisdn()) ? Constants.Telco.VINAPHONE.getValue() : -1;
                SubProfileInfo profile = db.getSubProfile("", friendUserId, telcoFilter);

                if (profile != null) {
                    if (profile.getUserId() == 0 || profile.getStatus() == Constants.PROFILE_STATUS_REMOVE) {
                        result.setErrorCode(Constants.NO_DATA_FOUND);
                    } else {
                        result.setProfileInfo(profile);
                        logger.info("@@@@@@@@@: lay duoc profile");
                        /*
                         * Kiem tra blacklist
                         */
//                        String key = "blacklist_" + String.valueOf(userId);
//                        String blacklistUserIds = getFromCache(key);
//                        if (blacklistUserIds == null) {
                        String blacklistUserIds = db.getBlackListOfUser(userId);
                        logger.info("@@@@@@@@@: lay duoc blacklists");
//                            if (blacklistUserIds == null) {
//                                result.setErrorCode(Constants.SYSTEM_ERROR);
//                                return result;
//                            }
//                            int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
//                            pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
//                        }

                        result.setDesc(Helper.isBlacklist(friendUserId, blacklistUserIds, "-") ? "1" : "0");
                        result.setErrorCode(Constants.SUCCESS);
                    }

                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    /**
     *
     * @param msisdn
     * @return True neu chi cho phep ket ban noi mang
     */
    public boolean isOnlyAllowTestNumberFriends(String msisdn) {
        String msisdnWithoutPrefix = Helper.formatMobileNumberWithoutPrefix(msisdn);
        String enableTestOnly = ConfigStack.getConfig("ivr_store", "sub_profile_test_only", "false");
        if ("true".equalsIgnoreCase(enableTestOnly)) {
            String testNumbers = ConfigStack.getConfig("ivr_store", "sub_profile_test_numbers", "");
            if (Helper.isEmpty(testNumbers) || !testNumbers.contains(msisdnWithoutPrefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOnlyAllowTestNumberInvite(String msisdn) {
        String msisdnWithoutPrefix = Helper.formatMobileNumberWithoutPrefix(msisdn);
        String enableTestOnly = ConfigStack.getConfig("ivr_store", "sub_invite_test_only", "false");
        if ("true".equalsIgnoreCase(enableTestOnly)) {
            String testNumbers = ConfigStack.getConfig("ivr_store", "sub_invite_test_numbers", "");
            if (Helper.isEmpty(testNumbers) || !testNumbers.contains(msisdnWithoutPrefix)) {
                return true;
            }
        }
        return false;
    }

    public Result getProfileInfo(HashMap<String, String> params) {
        Result result = new Result();
        String thefistID = "2";
        String msisdn = params.get("msisdn");
        int userId = Helper.getInt(params.get("userId"), -1);
        int requireIntro = Helper.getInt(params.get("requireIntro"), 0);

        if (!Helper.isNull(msisdn) || userId > 0) {
            SubProfileInfo profile = db.getSubProfile(msisdn, userId, -1);
            if (profile != null) {
                logger.info(">>>>>>>>>>>>>>>>>>>> profile: " + profile.getUserId());
                if (profile.getUserId() == 0) {
                    if (userId <= 0) {
                        thefistID = "1";
                        // goi sang API cua kho tap trung
//                        String url = ConfigStack.getConfig("auth_api", "url", "");
//                        String user = ConfigStack.getConfig("auth_api", "username", "");
//                        String pass = ConfigStack.getConfig("auth_api", "password", "");
//                        if (url.length() > 0) {
//                            url = url + "/getSubProfile?msisdn=" + msisdn + "&telco=2&source=IVR&username=" + user + "&password=" + pass;
//                            logger.info("request to global Storage url: " + url);
//                            try {
//                                // khoi tao client
//                                String resp = readUrl(url);
//                                logger.info("return JSON: " + resp);
//                                JSONObject obj = new JSONObject(resp);
//                                String errorCode = obj.getString("errorCode");
//                                JSONObject subProfile = obj.getJSONObject("subProfile");
//                                String userIdStr = subProfile.getString("userId");
//                                String telco = subProfile.getString("telco");
//                                String updatedDate = subProfile.getString("updatedDate");
//                                logger.info("BBBBBBBBBBB@@@@@: " + errorCode + " ;" + userIdStr + " ;" + telco);
//                                profile.setUpdatedDate(updatedDate);
//                                profile.setErrorCode(Integer.parseInt(errorCode));
//                                profile.setUserId(Integer.parseInt(userIdStr));
//                                profile.setTelco(Integer.parseInt(telco));
//                            } catch (Exception e) {
//                                logger.error("Error while request to global Storage: " + url + "; Exception: " + e);
//                                result.setErrorCode(Constants.SYSTEM_ERROR);
//                                return result;
//                            }
//                            /*
//                             * insert user vao db local
//                             */
//                            if (profile.getErrorCode() == 0) {
//                                // ket noi dc kho tap trung va tra lai ket qua dung
//                                profile = db.generateNewUserId(msisdn, profile.getUserId(), profile.getUpdatedDate(), profile.getCreatedDate());
//
//                            } else {
//                                logger.info("BBBBBBBBBBB@@@@@ ERRCODE tra lai:" + profile.getErrorCode());
//                                result.setErrorCode(Constants.SYSTEM_ERROR);
//                            }
//                        }
                        profile = db.generateNewUserId(msisdn, -1, "", "");
                        if (profile == null) {
                            result.setErrorCode(Constants.SYSTEM_ERROR);
                        } else {

                            if (requireIntro != 1 && profile.getStatus() != Constants.PROFILE_STATUS_ACTIVE
                                    && profile.getStatus() != Constants.PROFILE_STATUS_GOOD) {
                                profile.setIntroPath(null);
                            }
                            result.setProfileInfo(profile);
                            result.setErrorCode(Constants.SUCCESS);
                        }

                    } else {
                        result.setErrorCode(Constants.NO_DATA_FOUND);
                    }
                } else {
                    logger.info("getProfileInfo UserId <= 0  : " + msisdn);
                    if (profile.getProvinceId() > 0) {
                        ProvinceInfo p = Helper.getProvinceById(profile.getProvinceId(), db.getListProvince());
                        profile.setProvinceName(p.getFileName());
                        logger.info("getProfileInfo  getProvinceId :" + profile.getProvinceId());

                    }
                    if (requireIntro != 1 && profile.getStatus() != Constants.PROFILE_STATUS_ACTIVE
                            && profile.getStatus() != Constants.PROFILE_STATUS_GOOD) {
                        profile.setIntroPath(null);
                    }
                    result.setProfileInfo(profile);
                    result.setErrorCode(Constants.SUCCESS);
                    logger.info("getProfileInfo success :" + msisdn);
                }
                result.setThefistID(thefistID);
            } else {
                logger.info("getSubProfile khong lay dc thong tin khach hang sdt :" + msisdn);
                result.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }

        return result;
    }

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }

            return buffer.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public Object checkValidDate(HashMap<String, String> params) {
        Result result = new Result();
        result.setErrorCode(Constants.SYSTEM_ERROR);

        String date = params.get("date");
        if (!Helper.isNull(date)) {
            Calendar testDate = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
            sdf.setLenient(false);

            if (date.length() != 8) {
                result.setErrorCode(Constants.WRONG_PARAM);
            } else {
                try {
                    Date d = sdf.parse(date);
                    testDate.setTime(d);
                    result.setErrorCode(Constants.SUCCESS);
                } catch (Exception e) {
                    result.setErrorCode(Constants.WRONG_PARAM);
                }
            }
        }

        return result;
    }

    public Object updateProfileInfo(HashMap<String, String> params) {
        Result result = new Result();

        try {
            String msisdn = params.get("msisdn");
            int sex = Helper.getInt(params.get("sex"), 0);

            int provinceId = Helper.getInt(params.get("provinceId"), 0);
            int job = Helper.getInt(params.get("job"), -1);
            String birthTime = "";
            if (!Helper.isNull(params.get("birthTime"))) {
                birthTime = params.get("birthTime");
            }
            String introPath = "";
            if (!Helper.isNull(params.get("introPath"))) {
                introPath = params.get("introPath");
            }
            int lastStep = 0;
            if (!Helper.isNull(params.get("lastStep"))) {
                lastStep = Helper.getInt(params.get("lastStep"), 0);
            }
            int bonusPoint = 0;
            if (!Helper.isNull(params.get("bonusPoint"))) {
                bonusPoint = Helper.getInt(params.get("bonusPoint"), 0);
            }
            int finish = 0;
            if (!Helper.isNull(params.get("finish"))) {
                finish = Helper.getInt(params.get("finish"), 0);
            }
            String source = Helper.isNull(params.get("source")) ? "IVR" : params.get("source").toUpperCase();
            String birthDay = "";
            int birthYear = 0;
            int StatusProfire = 0;
            if (!Helper.isNull(birthTime) && Integer.parseInt(birthTime) > 0) {
                logger.info("Vao den day");
                Calendar testDate = Calendar.getInstance();
                SimpleDateFormat sdf = birthTime.length() == 4 ? new SimpleDateFormat("yyyy") : new SimpleDateFormat("ddMMyyyy");
                sdf.setLenient(false);
                try {
                    Date d = sdf.parse(birthTime);
                    testDate.setTime(d);
                } catch (Exception e) {
                    result.setErrorCode(Constants.WRONG_PARAM);
                    return result;
                }

                if (birthTime.length() > 4) {
                    logger.info("Vao den day");
                    sdf = new SimpleDateFormat("ddMM");
                    birthDay = sdf.format(testDate.getTime());
                }
                birthYear = testDate.get(Calendar.YEAR);
            }

            if (!Helper.isNull(msisdn)) {
                logger.info("Vao den day");
                SubProfileInfo profile = new SubProfileInfo();
                profile.setMsisdn(msisdn);
                profile.setBirthDay(birthDay);
                profile.setBirthYear(birthYear);
                profile.setSex(sex);
                profile.setJob(job);
                profile.setProvinceId(provinceId);
                profile.setIntroPath(introPath);
                profile.setLastStep(lastStep);
                profile.setSource(source);
                profile.setTelco(Constants.TELCO_VINA);
                profile.setStatus(StatusProfire);

                int point = 0;
                if (bonusPoint == 1) {
                    point = Integer.parseInt(ConfigStack.getConfig("game", "pointDeclareProfile", "0"));
                }

                /*
                 * Tu dong duyet khi ket thuc nhap lan dau
                 */
                boolean approveStatus = false;
                boolean updateIntroPath = finish == 1 && !Helper.isNull(introPath);
                if (finish == 1) {
                    int autoApproveProfile = Integer.parseInt(ConfigStack.getConfig("FRIEND", "AUTO_APPROVE_PROFILE", "0"));
                    if (autoApproveProfile == 1) {
                        approveStatus = true;
                    }
                }
                logger.info("Vao den day");
                if (db.updateSubProfile(profile, point, approveStatus, updateIntroPath)) {
                    result.setErrorCode(Constants.SUCCESS);
                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.info("Vao den day");
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object checkProvinceFromSign(HashMap<String, String> params) {
        Result result = new Result();

        try {
            String sign = params.get("sign");

            if (!Helper.isNull(sign)) {
                ProvinceInfo province = Helper.getProvinceFromSign(sign, db.getListProvince());
                if (province != null) {
                    result.setDesc(String.valueOf(province.getProvinceId()));
                    result.setName(province.getFileName());
                    result.setErrorCode(Constants.SUCCESS);
                } else {
                    result.setErrorCode(Constants.NO_DATA_FOUND);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object checkBlacklistUser(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);

            if (userId > 0 && friendUserId > 0) {
//                String key = "blacklist_" + String.valueOf(friendUserId);
//                String blacklistUserIds = getFromCache(key);
//                if (blacklistUserIds == null) {
                String blacklistUserIds = db.getBlackListOfUser(friendUserId);
//                    if (blacklistUserIds == null) {
//                        result.setErrorCode(Constants.SYSTEM_ERROR);
//                        return result;
//                    }
//                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
//                    pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
//                }

                result.setDesc(Helper.isBlacklist(userId, blacklistUserIds, "-") ? "1" : "0");
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object sendMTNoUpdate(HashMap<String, String> params) {
        Result result = new Result();
        String msisdn = "";
        try {
            if (params.get("msisdn") != null) {

                msisdn = params.get("msisdn");
                msisdn = Helper.processMobile(msisdn);
                SubProfileInfo profile = db.getSubProfile(msisdn, 0, -1);
                // send MT
                String mt = ConfigStack.getConfig("mt", "APPROVE_PROFILE_NO_UPDATE", "");
                mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
                logger.info("mt: " + mt);
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                int mtType = SMSType.Genral.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("SEND_FRIEND");
                sms.setSource("IVR");
                //  sms.setPackageId(packageId);
                sendMT(sms);
                // update DB
                db.updateStatusNoUpdateProfile(msisdn);
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object sendMTWeak(HashMap<String, String> params) {
        Result result = new Result();
        String msisdn = "";
        try {
            if (params.get("msisdn") != null) {
                msisdn = params.get("msisdn");
                msisdn = Helper.processMobile(msisdn);
                SubProfileInfo profile = db.getSubProfile(msisdn, 0, -1);
                int checkStausProfile = profile.getStatus();
                if (checkStausProfile != -1 && checkStausProfile != 3) {
                    // send MT
                    String mt = ConfigStack.getConfig("mt", "APPROVE_PROFILE_WEAK", "");
                    mt = mt.replaceAll("\\{ma_so\\}", String.valueOf(profile.getUserId()));
                    logger.info("mt: " + mt);
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                    int mtType = SMSType.Genral.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("SEND_FRIEND");
                    sms.setSource("IVR");
                    //  sms.setPackageId(packageId);
                    sendMT(sms);
                    // update DB
                    db.updateStatusWeakProfile(msisdn);
                }
                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object addBlacklistUser(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);

            if (userId > 0 && friendUserId > 0) {
//                String key = "blacklist_" + String.valueOf(userId);
//                String blacklistUserIds = getFromCache(key);
//                if (blacklistUserIds == null) {
                String blacklistUserIds = db.getBlackListOfUser(userId);
                logger.info("blacklistUserIds: " + blacklistUserIds);
//                    if (blacklistUserIds == null) {
//                        result.setErrorCode(Constants.SYSTEM_ERROR);
//                        return result;
//                    }
//                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
//                    pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
                // }

                String seperator = "-";
                boolean inBlacklist = Helper.isBlacklist(userId, blacklistUserIds, seperator);
                logger.info("inBlacklist: " + inBlacklist);
                if (!inBlacklist) {
                    blacklistUserIds += blacklistUserIds.length() > 0 ? String.valueOf(friendUserId) + seperator : seperator + String.valueOf(friendUserId) + seperator;
                    logger.info("blacklistUserIds: " + blacklistUserIds);
                    if (Constants.SUCCESS.equalsIgnoreCase(db.updateBlacklistOfUser(userId, blacklistUserIds))) {
                        //  int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                        //  pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
                        result.setErrorCode(Constants.SUCCESS);
                    } else {
                        result.setErrorCode(Constants.SYSTEM_ERROR);
                    }
                } else {
                    result.setErrorCode(Constants.SUCCESS);
                }
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object removeBlacklistUser(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);

            if (userId > 0 && friendUserId > 0) {
//                String key = "blacklist_" + String.valueOf(userId);
//                String blacklistUserIds = getFromCache(key);
//                if (blacklistUserIds == null) {
                String blacklistUserIds = db.getBlackListOfUser(userId);
//                    if (blacklistUserIds == null) {
//                        result.setErrorCode(Constants.SYSTEM_ERROR);
//                        return result;
//                    }
//                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
//                    pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
//                }

                String seperator = "-";
                String item = seperator + String.valueOf(friendUserId) + seperator;
                blacklistUserIds = blacklistUserIds.replaceAll(item, "-");
                if (seperator.equals(blacklistUserIds)) {
                    blacklistUserIds = "";
                }

                if (Constants.SUCCESS.equalsIgnoreCase(db.updateBlacklistOfUser(userId, blacklistUserIds))) {
//                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
//                    pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
                    result.setErrorCode(Constants.SUCCESS);
                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object sendVoiceMessage(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            int packageId = Helper.getInt(params.get("packageId"), 0);
            int friendUserId = Helper.getInt(params.get("friendUserId"), 0);
            int fromTelco = 0;
            if (Helper.isNull(params.get("fromTelco"))) {
                fromTelco = Constants.TELCO_VINA;
            } else {
                fromTelco = Helper.getInt(params.get("fromTelco"), 0);
            }
            String msisdn = params.get("msisdn");
            String voicePath = params.get("voicePath");
            String reciver = db.getMsisdnFromID(friendUserId);
            int toTelco = db.getTelcoFromID(friendUserId);
            reciver = Helper.formatMobileNumber(reciver);

            if (userId > 0 && friendUserId > 0 && !Helper.isNull(msisdn) && !Helper.isNull(voicePath)) {
                /*
                 * Lay profile thue bao nhan
                 */
                SubProfileInfo friendProfile = db.getSubProfile("", friendUserId, -1);
                if (friendProfile == null || friendProfile.getUserId() == 0) {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                    return result;
                }

                /*
                 * Kiem tra blacklist
                 */
//                String key = "blacklist_" + String.valueOf(friendUserId);
//                String blacklistUserIds = getFromCache(key);
//                if (blacklistUserIds == null) {
                String blacklistUserIds = db.getBlackListOfUser(userId);
//                    if (blacklistUserIds == null) {
//                        result.setErrorCode(Constants.SYSTEM_ERROR);
//                        return result;
//                    }
//                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
//                    pushToCacheWithExpiredTime(key, blacklistUserIds, timeOut);
//                }

                if (!Helper.isBlacklist(userId, blacklistUserIds, SubProfileInfo.SEPERATOR)) {
                    /*
                     * Che do duyet
                     * 1: Tu dong duyet
                     * 0: Admin duyet
                     */
                    int autoApproveVoiceMsg = Integer.parseInt(ConfigStack.getConfig("FRIEND", "AUTO_APPROVE_VOICE_MSG", "0"));
                    int approveStatus = autoApproveVoiceMsg == 1 ? Constants.MSG_APPROVED_STATUS : Constants.MSG_NOT_APPROVE_STATUS;

                    SubMessageInfo msg = new SubMessageInfo();
                    msg.setIdSender(userId);
                    msg.setIdReceiver(friendUserId);
                    msg.setSender(msisdn);
                    msg.setReceiver(friendProfile.getMsisdn());
                    msg.setVoicePath(voicePath);
                    msg.setFromTelco(fromTelco);
                    msg.setToTelco(toTelco);
                    msg.setPushStatus(approveStatus == Constants.MSG_APPROVED_STATUS ? 1 : 0);

                    if (db.insertVoiceMessage(msg, approveStatus)) {
                        result.setErrorCode(Constants.SUCCESS);

                        if (approveStatus == Constants.MSG_APPROVED_STATUS) {
                            /*
                             * Thong bao qua sms
                             */
                            String mt = ConfigStack.getConfig("mt", "FRIEND_NEW_VOICE_MSG", "");
                            logger.info("mt: " + mt);
                            logger.info("userId: " + String.valueOf(userId));
                            if (mt != null) {
                                mt = mt.replaceAll("\\{danh_sach\\}", String.valueOf(userId));
                                logger.info("mt: " + mt);
                            }
                            if (toTelco == Constants.TELCO_VINA) {
                                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                                int mtType = SMSType.Genral.getValue();
                                SMS sms = new SMS();
                                sms.setMtContent(mt);
                                sms.setMsisdn(reciver);
                                sms.setType(mtType);
                                sms.setHaveMO(false);
                                sms.setAction("SEND_FRIEND");
                                sms.setSource("IVR");
                                sms.setPackageId(packageId);
                                sendMT(sms);
                            }

                            /*
                             * Update trang thai thu moi
                             */
                            String keyVoiceMsg = Constants.MSG_UNREAD_VOICE_PREFIX + friendProfile.getMsisdn();
                            int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                            pushToCacheWithExpiredTime(keyVoiceMsg, "true", timeOut);
                        }
                    } else {
                        result.setErrorCode(Constants.SYSTEM_ERROR);
                    }

                } else {
                    result.setErrorCode(Constants.NO_DATA_FOUND);
                }
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object markAsReadedVoiceMsg(HashMap<String, String> params) {
        Result result = new Result();

        try {
            String msisdn = params.get("msisdn");
            String fromTelco = params.get("from_telco");
            //    String hasReadAllNewMsg = Helper.isNull(params.get("readAllMsg")) ? "false" : params.get("readAllMsg");

            int messageId = Helper.getInt(params.get("messageId"), 0);
            if (Helper.isNull(fromTelco)) {
                fromTelco = "0";
            }
            if (messageId > 0 && !Helper.isNull(msisdn)) {
                if (db.updateReadedVoiceMessage(messageId, fromTelco)) {
                    result.setErrorCode(Constants.SUCCESS);

                    //    if ("true".equalsIgnoreCase(hasReadAllNewMsg)) {
                    String keyVoiceMsg = Constants.MSG_UNREAD_VOICE_PREFIX + msisdn;
                    /*
                     * Xu ly xoa key trong cache 
                     */
                    if ("true".equalsIgnoreCase(getFromCache(keyVoiceMsg))) {
                        /*
                         * Kiem tra lai trang thai thu moi trong DB
                         */
                        Boolean hasNewMsg = db.hasUnReadVoiceMessage(msisdn);
                        if (!hasNewMsg) {
                            redisManager.del(keyVoiceMsg);
                        }
                    }
                    //  }
                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object deleteVoiceMsg(HashMap<String, String> params) {
        Result result = new Result();

        try {
            logger.info("AAAAAAA");
            logger.info("AAAAAAA msisdn = " + params.get("msisdn"));
            String msisdn = Helper.formatMobileNumber(params.get("msisdn"));

            int messageId = Helper.getInt(params.get("messageId"), 0);
            String fromTelco = params.get("from_telco");
            logger.info("AAAAAAA");
            if (Helper.isNull(fromTelco)) {
                fromTelco = String.valueOf(Constants.TELCO_VINA);
            }
            // String hasReadAllNewMsg = Helper.isNull(params.get("readAllMsg")) ? "false" : params.get("readAllMsg");
            if (messageId > 0) {
                if (db.deleteVoiceMessage(messageId, Integer.parseInt(fromTelco))) {
                    result.setErrorCode(Constants.SUCCESS);
                    //  if ("true".equalsIgnoreCase(hasReadAllNewMsg)) {
                    String keyVoiceMsg = Constants.MSG_UNREAD_VOICE_PREFIX + msisdn;
                    logger.info("AAAAAAA");
                    /*
                     * Xu ly xoa key trong cache 
                     */
                    if ("true".equalsIgnoreCase(getFromCache(keyVoiceMsg))) {
                        /*
                         * Kiem tra lai trang thai thu moi trong DB
                         */
                        logger.info("AAAAAAA");
                        Boolean hasNewMsg = db.hasUnReadVoiceMessage(msisdn);
                        if (!hasNewMsg) {
                            redisManager.del(keyVoiceMsg);
                        }
                    }
                    //    }
                }
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object markAsViewedUser(HashMap<String, String> params) {
        Result result = new Result();

        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            String msisdn = params.get("msisdn");

            if (userId > 0 && !Helper.isNull(msisdn)) {
                String key = "viewed_userid_" + msisdn;
                String viewedUserIds = getFromCache(key);
                if (viewedUserIds == null) {
                    viewedUserIds = db.getViewedUserIds(msisdn);
                    if (viewedUserIds == null) {
                        result.setErrorCode(Constants.SYSTEM_ERROR);
                        return result;
                    }
                    int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                    pushToCacheWithExpiredTime(key, viewedUserIds, timeOut);
                }

                String userIdItem = SubProfileInfo.SEPERATOR + String.valueOf(userId) + SubProfileInfo.SEPERATOR;
                if (viewedUserIds.indexOf(userIdItem) < 0) {
                    viewedUserIds = viewedUserIds.length() > 0 ? viewedUserIds + String.valueOf(userId) + SubProfileInfo.SEPERATOR : viewedUserIds + userIdItem;

                    if (db.updateViewedUserIds(msisdn, viewedUserIds)) {
                        int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                        pushToCacheWithExpiredTime(key, viewedUserIds, timeOut);
                        result.setErrorCode(Constants.SUCCESS);
                    } else {
                        result.setErrorCode(Constants.SYSTEM_ERROR);
                    }
                }

                result.setErrorCode(Constants.SUCCESS);

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object getListSubProfile(HashMap<String, String> params) {
        Result result = new Result();
        logger.info(">>> getListSubProfile");
        try {
            int userId = Helper.getInt(params.get("userId"), 0);
            int sex = Helper.getInt(params.get("sex"), 0);
            int fromYear = Helper.getInt(params.get("fromYear"), 0);
            int toYear = Helper.getInt(params.get("toYear"), 0);
            int provinceId = Helper.getInt(params.get("provinceId"), 0);
            int status = Helper.getInt(params.get("status"), -1);
            int regionProvince = Helper.getInt(params.get("regionProvince"), 0);
            String msisdn = params.get("msisdn");

            if (userId > 0 && !Helper.isNull(msisdn)) {
                int limit = Integer.parseInt(ConfigStack.getConfig("api", "max_profile_list", "300"));

                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                int fromBirthYear = 0;
                int toBirthYear = 0;
                if (fromYear > 0 && toYear > 0) {
                    fromBirthYear = currentYear - toYear;
                    toBirthYear = currentYear - fromYear;
                }
                // get content msisdn rightList
                int telcoFilter = isOnlyAllowTestNumberFriends(msisdn) ? Constants.Telco.VINAPHONE.getValue() : -1;

                ArrayList<SubProfileInfo> profiles = db.getListSubProfile(status, userId, sex, fromBirthYear, toBirthYear, provinceId, regionProvince, limit, telcoFilter);
                if (profiles != null) {
                    if (profiles.size() > 0) {

                        String key = "viewed_userid_" + msisdn;
                        String viewedUserIds = getFromCache(key);
                        if (viewedUserIds == null) {
                            viewedUserIds = db.getViewedUserIds(msisdn);
                            if (viewedUserIds == null) {
                                result.setErrorCode(Constants.SYSTEM_ERROR);
                                return result;
                            }
                            int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                            pushToCacheWithExpiredTime(key, viewedUserIds, timeOut);
                        }

                        /*
                         * Sap xep userId theo thu tu uu tien
                         * - Chua tung nghe
                         * - Da tung nghe
                         */
                        ArrayList<SubProfileInfo> listenedProfiles = new ArrayList<SubProfileInfo>();
                        ArrayList<SubProfileInfo> newProfiles = new ArrayList<SubProfileInfo>();
                        for (int i = 0; i < profiles.size(); i++) {
                            String userItem = SubProfileInfo.SEPERATOR + String.valueOf(profiles.get(i).getUserId()) + SubProfileInfo.SEPERATOR;

                            if (viewedUserIds.indexOf(userItem) >= 0) {
                                listenedProfiles.add(profiles.get(i));
                            } else {
                                newProfiles.add(profiles.get(i));
                            }
                        }
                        newProfiles.addAll(listenedProfiles);

                        SubProfileInfo[] profileData = new SubProfileInfo[newProfiles.size()];
                        for (int i = 0; i < newProfiles.size(); i++) {
                            profileData[i] = newProfiles.get(i);
                        }
                        result.setFriendList(profileData);
                        result.setTotal(String.valueOf(profileData.length));

                        /*
                         * Check lich su tuong tac tinh nang
                         */
                        int actionInteract = Constants.INTERACT_SEARCH_FRIEND;

                        // Check 7 ngay guide promt
                        int showGuidePromt = 1;
                        key = this.getClass().getCanonicalName() + "interact_" + msisdn + "_" + String.valueOf(actionInteract);
                        String value = getFromCache(key);
                        if (value == null) {
                            showGuidePromt = 1;
                        } else {
                            showGuidePromt = 0;
                        }
                        int timeOut = Integer.parseInt(ConfigStack.getConfig("FRIEND", "GUIDE_VOICE_MAIL_DAYS", "7")) * 24 * 60 * 60;
                        pushToCacheWithExpiredTime(key, msisdn, timeOut);
                        result.setDesc(String.valueOf(showGuidePromt));
                        result.setErrorCode(Constants.SUCCESS);
                    } else {
                        result.setErrorCode(Constants.NO_DATA_FOUND);
                    }

                } else {
                    result.setErrorCode(Constants.SYSTEM_ERROR);
                }

            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object checkMobileNumber(HashMap<String, String> params) {
        Result result = new Result();
        System.out.println("IN checkMobileNumber ()");
        try {
            String mobileNumber = params.get("mobileNumber");
            System.out.println("IN mobileNumber :" + mobileNumber);
            System.out.println(">>>>  :" + Helper.isMobileNumber(mobileNumber));

            if (Helper.isMobileNumber(mobileNumber)) {
                // check xem da dk chua

                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.WRONG_PARAM);
            }

        } catch (Exception e) {
            logger.error(e);
            result.setErrorCode(Constants.SYSTEM_ERROR);
        }

        return result;
    }

    public Object inviteFriend(HashMap<String, String> params) {
        Result result = new Result();
        String mt = null;

        String msisdn = params.get("msisdn");
        String receiver = params.get("receiver");
        receiver = Helper.processMobile(receiver);
        int packageId = Integer.parseInt(params.get("packageId"));
        String source = Helper.isNull(params.get("source")) ? "IVR" : params.get("source").toUpperCase();

        if (Helper.isNull(receiver) || Helper.isNull(msisdn)) {
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }
        receiver = Helper.processVina(receiver);

        /*
         * Kiem tra trang thai dang ky nguoi nhan
         */
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("msisdn", receiver);
        param.put("source", "CHECK_INVITE");
        SubPackageInfo subPackage = getBilling().getSubPackage(param);
        if (subPackage.getErrorCode().getValue() == BillingErrorCode.SystemError.getValue()
                || subPackage.getErrorCode().getValue() == BillingErrorCode.ChargingSubProcessing.getValue()) {
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }

        if (subPackage.getErrorCode().getValue() == BillingErrorCode.Success.getValue()
                && subPackage.getStatus() == SubPackageStatus.Active) {
            /*
             * Kiem tra ho so nguoi nhan
             */
            SubProfileInfo profile = db.getSubProfile(receiver, 0, -1);
            if (profile == null) {
                result.setErrorCode(Constants.SYSTEM_ERROR);
                return result;
            }

            /*
             * Gui MT cho nguoi gioi thieu
             */
            logger.info("AAAAAAAAAAAAAAAAAAAAAAAAA Gui MT cho nguoi gioi thieu");
            if (profile.getUserId() > 0) {
                mt = ConfigStack.getConfig("mt", "INVITE_SENDER_REGED_HAS_ID", "");
            } else {
                mt = ConfigStack.getConfig("mt", "INVITE_SENDER_REGED_NOT_ID", "");
            }
            if (mt != null) {
                mt = mt.replaceAll("\\{so_dien_thoai\\}", receiver);
                // smppRA.sendMT(mt, msisdn, null, null);
                mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                int mtType = SMSType.Genral.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("SEND_FRIEND");
                sms.setSource("IVR");
                sms.setPackageId(packageId);
                sendMT(sms);
            }

            /*
             * Gui MT cho nguoi duoc gioi thieu
             */
            logger.info("AAAAAAAAAAAAAAAAAAAAAAAAA Gui MT cho nguoi duoc gioi thieu");
            mt = ConfigStack.getConfig("mt", "INVITE_RECEIVER_REGED", "");
            logger.info("MT toi nguoi duoc gioi thieu: " + mt);
            if (mt != null) {
                mt = mt.replaceAll("\\{so_dien_thoai\\}", msisdn);

                if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", "-"), "-")) {
                    mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
                    int mtType = SMSType.Genral.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(receiver);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("SEND_FRIEND");
                    sms.setSource("IVR");
                    sms.setPackageId(packageId);
                    sendMT(sms);
                } else {
                    // db.insertSMSDelay(receiver, mt); // t vao queue sms delay
                    sendMTLow(receiver, packageId, mt, SMSType.Genral.getValue(), true);

                }
            }

            result.setErrorCode(Constants.WRONG_PARAM);
            return result;
        }

        /*
         * Kiem tra trang thai gioi thieu
         */
        String inviteResult = db.checkSubInvited(msisdn, receiver);
        if (inviteResult == null) {
            result.setErrorCode(Constants.SYSTEM_ERROR);
            return result;
        }

        if (inviteResult == Constants.SUCCESS) {
            /*
             * Tao loi gioi thieu
             */
            int point = Integer.parseInt(ConfigStack.getConfig("game", "pointInviteFriend", "0"));

            if (db.insertSubInvitation(msisdn, receiver, point, source).equalsIgnoreCase(Constants.SUCCESS)) {
                /*
                 * Gui MT cho nguoi gioi thieu
                 */
                mt = ConfigStack.getConfig("mt", "INVITE_SENDER_SUCCESS", "");
                if (mt != null) {
                    mt = mt.replaceAll("\\{so_dien_thoai\\}", receiver);
                    mt = mt.replaceAll("\\{diem\\}", String.valueOf(point));
                    //smppRA.sendMT(mt, msisdn, null, null);
                    int mtType = SMSType.Genral.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("SEND_FRIEND");
                    sms.setSource("IVR");
                    sms.setPackageId(packageId);
                    sendMT(sms);
                }
                /*
                 * Gui MT cho nguoi duoc gioi thieu
                 */
                mt = ConfigStack.getConfig("mt", "INVITE_RECEIVER_SUCCESS", "");
                if (mt != null) {
                    mt = mt.replaceAll("\\{so_dien_thoai\\}", msisdn);

                    if (!Helper.isOutOfRangeValidHour(ConfigStack.getConfig("general", "validRangeHourToPushSMS", "-"), "-")) {
                        int mtType = SMSType.Genral.getValue();
                        SMS sms = new SMS();
                        sms.setMtContent(mt);
                        sms.setMsisdn(receiver);
                        sms.setType(mtType);
                        sms.setHaveMO(false);
                        sms.setAction("SEND_FRIEND");
                        sms.setSource("IVR");
                        sms.setPackageId(packageId);
                        sendMT(sms);
                    } else {
                        //   db.insertSMSDelay(receiver, mt);
                        sendMTLow(receiver, packageId, mt, SMSType.Genral.getValue(), true);
                    }
                }

                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.SYSTEM_ERROR);
            }

        } else {
            /*
             * Gui MT thong bao da duoc gioi thieu truoc do
             */
            mt = ConfigStack.getConfig("mt", "INVITE_SENDER_EXISTED", "");
            if (mt != null) {
                mt = mt.replaceAll("\\{so_dien_thoai\\}", receiver);
                int mtType = SMSType.Genral.getValue();
                SMS sms = new SMS();
                sms.setMtContent(mt);
                sms.setMsisdn(msisdn);
                sms.setType(mtType);
                sms.setHaveMO(false);
                sms.setAction("SEND_FRIEND");
                sms.setSource("IVR");
                sms.setPackageId(packageId);
                sendMT(sms);
            }

            result.setErrorCode(Constants.WRONG_PARAM);
        }

        return result;
    }

    public Object markAsNewVoiceMsg(HashMap<String, String> params) {
        Result result = new Result();

        String listMsisdn = params.get("msisdn");
        if (!Helper.isNull(listMsisdn)) {
            String[] parts = listMsisdn.split("-");

            HashMap<String, String> subs = new HashMap<String, String>();
            for (int i = 0; i < parts.length; i++) {
                String msisdn = parts[i];
                subs.put(msisdn, msisdn);
            }

            for (String msisdn : subs.keySet()) {
                String keyVoiceMsg = Constants.MSG_UNREAD_VOICE_PREFIX + msisdn;
                int timeOut = Integer.parseInt(ConfigStack.getConfig("api_redis", "time_out", "600"));
                pushToCacheWithExpiredTime(keyVoiceMsg, "true", timeOut);
            }

            result.setErrorCode(Constants.SUCCESS);
        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }

        return result;
    }

    public Object sendTopFriendViaSMS(HashMap<String, String> params) {
        Result result = new Result();
        int status = Constants.PROFILE_STATUS_GOOD;
        int sex = 0;
        int limit = Integer.parseInt(ConfigStack.getConfig("api", "top_friend_via_sms", "10"));
        int fromBirthYear = 0;
        int toBirthYear = 0;
        int provinceId = 0;
        int regionProvince = 0;
        String msisdn = params.get("msisdn");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        //  int packageId = Integer.parseInt(params.get("msisdn"));
        int userId = Helper.getInt(params.get("userId"), 0);
        if (!Helper.isNull(params.get("sex"))) {
            sex = Helper.getInt(params.get("sex"), 0);
        }
        if (!Helper.isNull(params.get("fromYear"))) {
            if (Helper.getInt(params.get("toYear"), 0) > 0) {
                fromBirthYear = currentYear - Helper.getInt(params.get("toYear"), 0);
            }
        }
        if (!Helper.isNull(params.get("toYear"))) {
            if (Helper.getInt(params.get("toYear"), 0) > 0) {
                toBirthYear = currentYear - Helper.getInt(params.get("fromYear"), 0);
            }
        }
        if (!Helper.isNull(params.get("provinceId"))) {
            provinceId = Helper.getInt(params.get("provinceId"), 0);
        }
        if (!Helper.isNull(params.get("regionProvince"))) {
            regionProvince = Helper.getInt(params.get("regionProvince"), 0);
        }
        if (!Helper.isNull(msisdn) && userId > 0) {

            ArrayList<SubProfileInfo> profiles = db.getListSubProfile(status, userId, sex, fromBirthYear, toBirthYear, provinceId, regionProvince, limit, -1);
            if (profiles != null && profiles.size() > 0) {
                String mt = ConfigStack.getConfig("mt", "TOP_FRIEND_GOOD", "");
                if (mt != null) {
                    String friendIds = "";
                    for (int i = 0; i < profiles.size(); i++) {
                        friendIds += friendIds.length() == 0 ? profiles.get(i).getUserId() : "; " + profiles.get(i).getUserId();
                    }

                    mt = mt.replaceAll("\\{danh_sach\\}", friendIds);
                    int mtType = SMSType.Genral.getValue();
                    SMS sms = new SMS();
                    sms.setMtContent(mt);
                    sms.setMsisdn(msisdn);
                    sms.setType(mtType);
                    sms.setHaveMO(false);
                    sms.setAction("SEND_FRIEND");
                    sms.setSource("IVR");
                    //  sms.setPackageId(packageId);
                    sendMT(sms);
                }

                result.setErrorCode(Constants.SUCCESS);
            } else {
                result.setErrorCode(Constants.NO_DATA_FOUND);
            }
        } else {
            result.setErrorCode(Constants.WRONG_PARAM);
        }

        return result;
    }

    public void sendMTLow(String msisdn, int packageId, String content, int mtType, boolean isBrandName) {
        SMS sms = new SMS();
        sms.setMtContent(content);
        sms.setMsisdn(msisdn);
        sms.setType(mtType);
        sms.setHaveMO(false);
        sms.setPackageId(packageId);
        sms.setSource("System");
        sms.setBrandName(isBrandName);
        // gui tin
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Message msg = new Message();
            msg.setHeader("Content", sms.getMtContent());
            msg.setHeader("Msisdn", sms.getMsisdn());
            if (sms.isBrandName()) {
                msg.setHeader("ShortCode", ConfigStack.getConfig("api_sms", "brand_name", ""));
            } else {
                msg.setHeader("ShortCode", ConfigStack.getConfig("api_sms", "service_number", ""));
            }
            if (sms.isHaveMO()) {
                msg.setHeader("MoContent", sms.getMoContent());
                msg.setHeader("Sms_id", sms.getSmsId());
                msg.setHeader("ReceiveAt", sdf.format(sms.getMoReceivedTime()));
                msg.setHeader("Method", "ALL");
            } else {
                msg.setHeader("MoContent", "");
                msg.setHeader("Sms_id", "");
                msg.setHeader("ReceiveAt", "");
                msg.setHeader("Method", "MT_ONLY");
            }
            msg.setHeader("Channel", "SMS");
            msg.setHeader("Action", sms.getAction());
            try {
                sendMT(sms);
            } catch (Exception e) {
                logger.error(e);
            }
        } catch (Exception ex) {
            logger.error(ex);
        }
        // insert db
        //db.insertSMS(sms);
    }

    public Object sendSMSViaCMS(HashMap<String, String> params) {
        logger.info("sendSMSViaCMS: " + params.get("msisdn"));
        Result result = new Result();
        try {
            SMS sms = new SMS();
            String mt = params.get("mt");
            //int packageId = Integer.parseInt(params.get("packageId"));
            String msisdn = (Helper.formatMobileNumber(params.get("msisdn")));

            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            int mtType = SMSType.Nofity.getValue();
            sms.setMtContent(mt);
            sms.setMsisdn(msisdn);
            sms.setType(mtType);
            sms.setHaveMO(false);
            sms.setAction("CMS");
            sms.setSource("CMS");
            // sms.setPackageId(packageId);
            sendMT(sms);
            result.setErrorCode("0");
        } catch (Exception e) {
            logger.error("sendSMSViaCMS: " + e, e);
            result.setErrorCode("-1");
        }
        return result;
    }

    public Object sendMTPromotion(HashMap<String, String> params) {
        logger.info("sendMTPromotion: " + params.get("msisdn"));
        Result result = new Result();
        try {
            SMS sms = new SMS();
            String mt = params.get("mt");
            //int packageId = Integer.parseInt(params.get("packageId"));
            String msisdn = (Helper.formatMobileNumber(params.get("msisdn")));

            mt = Helper.prepaidContent(mt, "", "", "", "", "", "", "", "", "", "", "", "", "");
            int mtType = SMSType.Promotion.getValue();
            sms.setMtContent(mt);
            sms.setMsisdn(msisdn);
            sms.setType(mtType);
            sms.setHaveMO(false);
            sms.setAction("KM5K");
            sms.setSource(params.get("source"));
            // sms.setPackageId(packageId);
            sendMT(sms);
            result.setErrorCode("0");
        } catch (Exception e) {
            logger.error("sendMTPromotion: " + e, e);
            result.setErrorCode("-1");
        }
        return result;
    }

    public void sendSMS1(String msisdn, String mt, String Action, String Source, String subPackageId) {
        int mtType = SMSType.Nofity.getValue();
        SMS sms = new SMS();
        sms.setMtContent(mt);
        sms.setMsisdn(msisdn);
        sms.setType(mtType);
        sms.setHaveMO(false);
        sms.setAction("SEND_FRIEND");
        sms.setSource("IVR");
        sms.setPackageId(Integer.parseInt(subPackageId));
        sendMT(sms);
    }
// lottery

    public Object checkProvinceLottery(HashMap<String, String> params) {
        return lotteryStack.checkProvinceLottery(params);
    }

    public Object getDayOfWeekByDate(HashMap<String, String> params) {
        return lotteryStack.getDayOfWeekByDate(params);
    }

    public Object getLotteryDataByProvince(HashMap<String, String> params) {
        return lotteryStack.getLotteryDataByProvince(params);
    }

    public Object getLotteryRegionOfSub(HashMap<String, String> params) {
        return lotteryStack.getLotteryRegionOfSub(params);
    }

    public Object getProvinceNameOfLottery(HashMap<String, String> params) {
        return lotteryStack.getProvinceNameOfLottery(params);
    }

    public Object getRatingOfLotteryInDateRange(HashMap<String, String> params) {
        return lotteryStack.getRatingOfLotteryInDateRange(params);
    }

    public Object getRatingOfLotteryInDateRangeByNumber(HashMap<String, String> params) {
        return lotteryStack.getRatingOfLotteryInDateRangeByNumber(params);
    }

    public Object getRatingOfLotteryInPublishDateByNumber(HashMap<String, String> params) {
        return lotteryStack.getRatingOfLotteryInPublishDateByNumber(params);
    }

    public Object getStatusLotteryCallout(HashMap<String, String> params) {
        return lotteryStack.getStatusLotteryCallout(params);
    }

    public Object insertLotteryHis(HashMap<String, String> params) {
        return lotteryStack.insertLotteryHis(params);
    }

    public Object registerLotteryCallout(HashMap<String, String> params) {
        return lotteryStack.registerLotteryCallout(params);
    }

    public Object registerLotteryRegion(HashMap<String, String> params) {
        return lotteryStack.registerLotteryRegion(params);
    }
// gift

    public Object donateGift(HashMap<String, String> params) {
        return giftStack.donateGift(params);
    }

    public Object getListGiftContentFromAlbum(HashMap<String, String> params) {
        return giftStack.getListGiftContentFromAlbum(params);
    }

    public Object getListNewGiftContent(HashMap<String, String> params) {
        return giftStack.getListNewGiftContent(params);
    }

    public Object getValidRangeHourForGift(HashMap<String, String> params) {
        return giftStack.getValidRangeHourForGift(params);
    }

    public Object removeGiftContentFromAlbum(HashMap<String, String> params) {
        return giftStack.removeGiftContentFromAlbum(params);
    }

    public Object sendSMSSearchMusicCode(HashMap<String, String> params) {
        return giftStack.sendSMSSearchMusicCode(params);
    }

    public Object updateListenedGiftContent(HashMap<String, String> params) {
        return giftStack.updateListenedGiftContent(params);
    }

    public Object validateCallDate(HashMap<String, String> params) {
        return giftStack.validateCallDate(params);
    }

    public Object validateCallHour(HashMap<String, String> params) {
        return giftStack.validateCallHour(params);
    }

    public Object validateGiftReceiver(HashMap<String, String> params) {
        return giftStack.validateGiftReceiver(params);
    }

// gameStack
    public Object answerHealthGame(HashMap<String, String> params) {
        return gameStack.answerHealthGame(params);
    }

    public Object rejectHealthGame(HashMap<String, String> params) {
        return gameStack.rejectHealthGame(params);
    }
    // weather

    public Object checkRegionListened(HashMap<String, String> params) {
        return weatherStack.checkRegionListened(params);
    }

    public Object checkSetupCallOut(HashMap<String, String> params) {
        return weatherStack.checkSetupCallOut(params);
    }

    public Object getDataWeatherByProvince(HashMap<String, String> params) {
        return weatherStack.getDataWeatherByProvince(params);
    }

    public Object rejectWeather(HashMap<String, String> params) {
        return weatherStack.rejectWeather(params);
    }

    public Object setTimeCallout(HashMap<String, String> params) {
        return weatherStack.setTimeCallout(params);
    }

    public Object updateHistoryContentWeather(HashMap<String, String> params) {
        return weatherStack.updateHistoryContentWeather(params);
    }

    public Object updateStatusCallOutWeather(HashMap<String, String> params) {
        return weatherStack.updateStatusCallOutWeather(params);
    }

    public Object checkProvinceWeather(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> RS checkProvinceWeather" + params.get("sign"));
        return weatherStack.checkProvinceWeather(params);
    }

    /* =============== Idol ================ */
    public Result getActiveIdolCompetition(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getActiveIdolCompetition");
        return idolStack.getActiveIdolCompetition(params);
    }

    public Result getListRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getListRecordIdol");
        return idolStack.getListRecordIdol(params);
    }

    public Result sendRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs sendRecordIdol");
        return idolStack.sendRecordIdol(params);
    }

    public Result removeRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs removeRecordIdol");
        return idolStack.removeRecordIdol(params);
    }

    public Result getListRecordForVoteIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getListRecordForVoteIdol");
        return idolStack.getListRecordForVoteIdol(params);
    }

    public Result addRecordIdolToMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs addRecordIdolToMyCollection");
        return idolStack.addRecordIdolToMyCollection(params);
    }

    public Result removeRecordIdolFromMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs removeRecordIdolFromMyCollection");
        return idolStack.removeRecordIdolFromMyCollection(params);
    }

    public Result voteForRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs voteForRecordIdol");
        return idolStack.voteForRecordIdol(params);
    }

    public Result processApproveRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs processApproveRecordIdol");
        return idolStack.processApproveRecordIdol(params);
    }

    public Result assignTopFisrtRoundRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs assignTopFisrtRoundRecordIdol");
        return idolStack.assignTopFisrtRoundRecordIdol(params);
    }

    public Result assignTopSecondRoundRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs assignTopSecondRoundRecordIdol");
        return idolStack.assignTopSecondRoundRecordIdol(params);
    }

    public Result updateListenHisOfRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs updateListenHisOfRecordIdol");
        return idolStack.updateListenHisOfRecordIdol(params);
    }

    public Result getMyCollectionIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getMyCollectionIdol");
        return idolStack.getMyCollectionIdol(params);
    }

    public Result getInfoOfRecordIdol(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getInfoOfRecordIdol");
        return idolStack.getInfoOfRecordIdol(params);
    }

    public Result getListRecordByID(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getListRecordByID");
        return idolStack.getListRecordByID(params);
    }
    /* =============== Idol ================ */

    public ResultAbstract updateListenToptenHistory(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs updateListenToptenHistory");
        return toptenStack.updateListenHistory(params);
    }

    public ResultAbstract getCountListendInNDays(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getCountListendInNDays");
        return toptenStack.getCountListendInNDays(params);
    }

    public ResultAbstract getListTopten(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getListTopten");
        return toptenStack.getListTopten(params);
    }
    /* =============== Idol ================ */

    public Result getListStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getListStudioRecord");
        return studioStack.getListStudioRecord(params);
    }

    public Result getTopStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getTopStudioRecord");
        return studioStack.getTopStudioRecord(params);
    }

    public Result sendStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs sendStudioRecord");
        return studioStack.sendStudioRecord(params);
    }

    public Result addStudioRecordToMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs addStudioRecordToMyCollection");
        return studioStack.addStudioRecordToMyCollection(params);
    }

    public Result removeStudioRecordFromMyCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs removeStudioRecordFromMyCollection");
        return studioStack.removeStudioRecordFromMyCollection(params);
    }

    public Result voteForStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs voteForStudioRecord");
        return studioStack.voteForStudioRecord(params);
    }

    public Result processApproveStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs processApproveStudioRecord");
        return studioStack.processApproveStudioRecord(params);
    }

    public Result updateListenHisOfStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs updateListenHisOfStudioRecord");
        return studioStack.updateListenHisOfStudioRecord(params);
    }

    public Result getMyStudioCollection(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getMyStudioCollection");
        return studioStack.getMyStudioCollection(params);
    }

    public Result getInfoOfStudioRecord(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getInfoOfStudioRecord");
        return studioStack.getInfoOfStudioRecord(params);
    }

    public Result getStudioRecordByID(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getStudioRecordByID");
        return studioStack.getStudioRecordByID(params);
    }

    public Result checkAbilityRecordStudio(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs checkAbilityRecordStudio");
        return studioStack.checkAbilityRecordStudio(params);
    }

    /* =============== Gamification ================ */
    public Object exchangePointToAward(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs gamification");
        return gamificationStack.exchangePointToAward(params);
    }

    public Object getAwardInfoByKey(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getAwardInfoByKey");
        return gamificationStack.getAwardInfoByKey(params);
    }

    public Object addPointByAction(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs addPointByAction");
        return gamificationStack.addPointByAction(params);
    }

    public void checkingPointEvent() {
        logger.info(" >>>>>>>>>>>>>>> Rs checkingPointEvent");
        gamificationStack.checkingPointEvent();
    }

    public void birthDayPointEvent() {
        logger.info(" >>>>>>>>>>>>>>> Rs birthDayPointEvent");
        gamificationStack.birthDayPointEvent();
    }

    /**
     * Connect Stack
     */
    public Object answerConnectQuestion(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs answerConnectQuestion");
        return connectStack.answerConnectQuestion(params);
    }

    public Object getListContentConnect(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getListContentConnect");
        return connectStack.getListContentConnect(params);
    }

    public Object rejectGameConnect(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs rejectGameConnect");
        return connectStack.rejectGameConnect(params);
    }

    public Object updateHistoryConnect(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs updateHistoryConnect");
        return connectStack.updateHistoryConnect(params);
    }

    /**
     * Game hack não
     */
    public Object getQuestion(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs updateHistoryConnect");
        return gameIntellectualStack.getQuestion(params);
    }

    public Object checkCondition(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs checkCondition");
        return gameIntellectualStack.checkCondition(params);
    }

    public Object getPointUser(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getPointUser");
        return gameIntellectualStack.getPointUser(params);
    }

    public Object saveHistory(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs saveHistory");
        return gameIntellectualStack.saveHistory(params);
    }

    public Object checkConditionResetPoint(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs checkConditionResetPoint");
        return gameIntellectualStack.checkConditionResetPoint(params);
    }

    public Object getNumAnswerCorrect(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs getNumAnswerCorrect");
        return gameIntellectualStack.getNumAnswerCorrect(params);
    }

    public Object chooseStar(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs chooseStar");
        return gameIntellectualStack.chooseStar(params);
    }

    public Object insertOrUpdatePoint(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs insertOrUpdatePoint");
        return gameIntellectualStack.insertOrUpdatePoint(params);
    }

    public Object insertTopup(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs insertTopup ");
        return gameIntellectualStack.insertTopup(params);
    }

    public Object updateStar(HashMap<String, String> params) {
        logger.info(" >>>>>>>>>>>>>>> Rs updateStar ");
        return gameIntellectualStack.updateStar(params);
    }

    /**
     * @return the billing
     */
    public BillingStack getBilling() {
        return billing;
    }

    /**
     * @param billing the billing to set
     */
    public void setBilling(BillingStack billing) {
        this.billing = billing;
    }
}
