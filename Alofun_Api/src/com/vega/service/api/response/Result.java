/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.response;

import com.vega.service.api.common.GiftContentInfo;
import com.vega.service.api.common.SubMessageInfo;
import com.vega.service.api.common.SubProfileInfo;
import com.vega.service.api.object.ConnectInfo;
import com.vega.service.api.object.Content;
import com.vega.service.api.object.IdolCompetitionInfo;
import com.vega.service.api.object.IdolRecordInfo;
import com.vega.service.api.object.LotteryInfo;
import com.vega.service.api.object.StudioRecordInfo;
import java.sql.Timestamp;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 *
 * @author PhongTom
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class Result {

    /*
     * Ma loi ket qua request
     */
    private String errorCode;
    /*
     * Id chu ki goi cuoc
     */
    private String subPackageId;
    /*
     * Id goi cuoc dang dung
     */
    private String packageId;
    /*
     * So dien thoai thue bao
     */
    private String msisdn;
    /*
     * Thoi gian het han, dinh dang: yyyy-MM-dd HH:mm:ss
     */
    private String expireAt;
    /*
     * So phut mien phi con lai
     */
    private String freeMinutes;
    /*
     * Huong khuyen mai dang ky: 1: co dieu kien; 0: khong du dieu kien
     */
    private String promotion;
    private String promtOrd;
    // Content
    private String total;
    private Content[] data;
    private String channelId;
    // Story
    private String contentOrd;
    private int playType;
    private String totalPart;
    private String channelOrd;
    private String lastPartNumber;
    private String summaryPath;
    private String namePath;
    private String contentPath;
    private String duration;
    private String contentType;
    private String listened;
    private String contentId;
    private String packagePrice;
    private String summaryStoryPath;
    private String desc;
    private String promtListen;
    // Charge Gift
    private int chargeFree = 0;
    private int giftFree = 0;
    private String year;
    private String isSub;
    // branch_script
    private String invalikey;
    private String index;
    //friend
    private int hasUnreadVoiceMsg;
    private SubProfileInfo[] friendList;
    private SubProfileInfo profileInfo;
    private SubMessageInfo[] msgList;
    private String count;
    private String name;
    private String thefistID;
    // xo so
    private String region;
    private String region_weather;
    private String region_id;
    private LotteryInfo[] lotteryData;
    private String lotNumber1;
    private String lotNumber2;
    private String lotNumber3;
    private String lotNumber4;
    private String id;
    private String type;
    private String canPlayHealthGame;
    // gift
    private GiftContentInfo[] giftData;
    //Idol
    private IdolCompetitionInfo competInfo;
    private IdolRecordInfo[] idolRecords;
    //Studio
    private StudioRecordInfo[] studioRecords;
    // mini game
    private int totalPoint;
    private int freeMinutesOfAward;
    private int dayUsingOfAward;
    private int minimumPointToAward;
    private int firstCallInDay;
    // KB Connect
    private ConnectInfo[] connectData;
    private int haveHis;
    private Timestamp mt_notify_at;
    private int count_reject;
    private int status;
    public int getFirstCallInDay() {
        return firstCallInDay;
    }

    public void setFirstCallInDay(int firstCallInDay) {
        this.firstCallInDay = firstCallInDay;
    }

    public int getGiftFree() {
        return giftFree;
    }

    public void setGiftFree(int giftFree) {
        this.giftFree = giftFree;
    }

    public String getIsSub() {
        return isSub;
    }

    public void setIsSub(String isSub) {
        this.isSub = isSub;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getChargeFree() {
        return chargeFree;
    }

    public void setChargeFree(int chargeFree) {
        this.chargeFree = chargeFree;
    }

    public String getPromtListen() {
        return promtListen;
    }

    public void setPromtListen(String promtListen) {
        this.promtListen = promtListen;
    }

    public String getSummaryStoryPath() {
        return summaryStoryPath;
    }

    public void setSummaryStoryPath(String summaryStoryPath) {
        this.summaryStoryPath = summaryStoryPath;
    }

    public String getPackagePrice() {
        return packagePrice;
    }

    public void setPackagePrice(String packagePrice) {
        this.packagePrice = packagePrice;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        if (contentId.equals("null")) {
            return;
        }
        this.contentId = contentId;
    }

    public String getListened() {
        return listened;
    }

    public void setListened(String listened) {
        if (listened.equals("null")) {
            return;
        }
        this.listened = listened;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        if (contentType.equals("null")) {
            return;
        }
        this.contentType = contentType;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        if (duration.equals("null")) {
            return;
        }
        this.duration = duration;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        if (contentPath.equals("null")) {
            return;
        }
        this.contentPath = contentPath;
    }

    public String getNamePath() {
        return namePath;
    }

    public void setNamePath(String namePath) {
        if (namePath.equals("null")) {
            return;
        }
        this.namePath = namePath;
    }

    public String getSummaryPath() {
        return summaryPath;
    }

    public void setSummaryPath(String summaryPath) {
        if (summaryPath.equals("null")) {
            return;
        }
        this.summaryPath = summaryPath;
    }

    public String getLastPartNumber() {
        return lastPartNumber;
    }

    public void setLastPartNumber(String lastPartNumber) {
        if (lastPartNumber.equals("null")) {
            return;
        }
        this.lastPartNumber = lastPartNumber;
    }

    public String getChannelOrd() {
        return channelOrd;
    }

    public void setChannelOrd(String channelOrd) {
        if (channelOrd.equals("null")) {
            return;
        }
        this.channelOrd = channelOrd;
    }

    public String getTotalPart() {
        return totalPart;
    }

    public void setTotalPart(String totalPart) {
        if (totalPart.equals("null")) {
            return;
        }
        this.totalPart = totalPart;
    }

    public int getPlayType() {
        return playType;
    }

    public void setPlayType(int playType) {
        this.playType = playType;
    }

    public String getContentOrd() {
        return contentOrd;
    }

    public void setContentOrd(String contentOrd) {
        if (contentOrd.equals("null")) {
            return;
        }
        this.contentOrd = contentOrd;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        if (channelId.equals("null")) {
            return;
        }
        this.channelId = channelId;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        if (total.equals("null")) {
            return;
        }
        this.total = total;
    }

    public Content[] getData() {
        return data;
    }

    public void setData(Content[] data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        if (errorCode.equals("null")) {
            return;
        }
        this.errorCode = errorCode;
    }

    public String getSubPackageId() {
        return subPackageId;
    }

    public void setSubPackageId(String subPackageId) {
        if (subPackageId.equals("null")) {
            return;
        }
        this.subPackageId = subPackageId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        if (packageId.equals("null")) {
            return;
        }
        this.packageId = packageId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        if (msisdn.equals("null")) {
            return;
        }
        this.msisdn = msisdn;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        if (expireAt.equals("null")) {
            return;
        }
        this.expireAt = expireAt;
    }

    public String getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(String freeMinutes) {
        if (freeMinutes.equals("null")) {
            return;
        }
        this.freeMinutes = freeMinutes;
    }

    public String getPromotion() {
        return promotion;
    }

    public void setPromotion(String promotion) {
        if (promotion.equals("null")) {
            return;
        }
        this.promotion = promotion;
    }

    public String getPromtOrd() {
        return promtOrd;
    }

    public void setPromtOrd(String promtOrd) {
        if (promtOrd.equals("null")) {
            return;
        }
        this.promtOrd = promtOrd;
    }

    /**
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * @param desc the desc to set
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * @return the invalikey
     */
    public String getInvalikey() {
        return invalikey;
    }

    /**
     * @param invalikey the invalikey to set
     */
    public void setInvalikey(String invalikey) {
        this.invalikey = invalikey;
    }

    /**
     * @return the hasUnreadVoiceMsg
     */
    public int getHasUnreadVoiceMsg() {
        return hasUnreadVoiceMsg;
    }

    /**
     * @param hasUnreadVoiceMsg the hasUnreadVoiceMsg to set
     */
    public void setHasUnreadVoiceMsg(int hasUnreadVoiceMsg) {
        this.hasUnreadVoiceMsg = hasUnreadVoiceMsg;
    }

    /**
     * @return the friendList
     */
    public SubProfileInfo[] getFriendList() {
        return friendList;
    }

    /**
     * @param friendList the friendList to set
     */
    public void setFriendList(SubProfileInfo[] friendList) {
        this.friendList = friendList;
    }

    /**
     * @return the profileInfo
     */
    public SubProfileInfo getProfileInfo() {
        return profileInfo;
    }

    /**
     * @param profileInfo the profileInfo to set
     */
    public void setProfileInfo(SubProfileInfo profileInfo) {
        this.profileInfo = profileInfo;
    }

    /**
     * @return the msgList
     */
    public SubMessageInfo[] getMsgList() {
        return msgList;
    }

    /**
     * @param msgList the msgList to set
     */
    public void setMsgList(SubMessageInfo[] msgList) {
        this.msgList = msgList;
    }

    /**
     * @return the count
     */
    public String getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(String count) {
        this.count = count;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the index
     */
    public String getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(String index) {
        this.index = index;
    }

    /**
     * @return the thefistID
     */
    public String getThefistID() {
        return thefistID;
    }

    /**
     * @param thefistID the thefistID to set
     */
    public void setThefistID(String thefistID) {
        this.thefistID = thefistID;
    }

    /**
     * @return the region
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return the region
     */
    public String getType() {
        return type;
    }

    /**
     * @param region the region to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the lotteryData
     */
    public LotteryInfo[] getLotteryData() {
        return lotteryData;
    }

    /**
     * @param lotteryData the lotteryData to set
     */
    public void setLotteryData(LotteryInfo[] lotteryData) {
        this.lotteryData = lotteryData;
    }

    /**
     * @return the lotNumber1
     */
    public String getLotNumber1() {
        return lotNumber1;
    }

    /**
     * @param lotNumber1 the lotNumber1 to set
     */
    public void setLotNumber1(String lotNumber1) {
        this.lotNumber1 = lotNumber1;
    }

    /**
     * @return the lotNumber2
     */
    public String getLotNumber2() {
        return lotNumber2;
    }

    /**
     * @param lotNumber2 the lotNumber2 to set
     */
    public void setLotNumber2(String lotNumber2) {
        this.lotNumber2 = lotNumber2;
    }

    /**
     * @return the lotNumber3
     */
    public String getLotNumber3() {
        return lotNumber3;
    }

    /**
     * @param lotNumber3 the lotNumber3 to set
     */
    public void setLotNumber3(String lotNumber3) {
        this.lotNumber3 = lotNumber3;
    }

    /**
     * @return the lotNumber4
     */
    public String getLotNumber4() {
        return lotNumber4;
    }

    /**
     * @param lotNumber4 the lotNumber4 to set
     */
    public void setLotNumber4(String lotNumber4) {
        this.lotNumber4 = lotNumber4;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the canPlayHealthGame
     */
    public String getCanPlayHealthGame() {
        return canPlayHealthGame;
    }

    /**
     * @param canPlayHealthGame the canPlayHealthGame to set
     */
    public void setCanPlayHealthGame(String canPlayHealthGame) {
        this.canPlayHealthGame = canPlayHealthGame;
    }

    /**
     * @return the giftData
     */
    public GiftContentInfo[] getGiftData() {
        return giftData;
    }

    /**
     * @param giftData the giftData to set
     */
    public void setGiftData(GiftContentInfo[] giftData) {
        this.giftData = giftData;
    }

    /**
     * @return the region_weather
     */
    public String getRegion_weather() {
        return region_weather;
    }

    /**
     * @param region_weather the region_weather to set
     */
    public void setRegion_weather(String region_weather) {
        this.region_weather = region_weather;
    }

    /**
     * @return the region_id
     */
    public String getRegion_id() {
        return region_id;
    }

    /**
     * @param region_id the region_id to set
     */
    public void setRegion_id(String region_id) {
        this.region_id = region_id;
    }

    /**
     * @return the competInfo
     */
    public IdolCompetitionInfo getCompetInfo() {
        return competInfo;
    }

    /**
     * @param competInfo the competInfo to set
     */
    public void setCompetInfo(IdolCompetitionInfo competInfo) {
        this.competInfo = competInfo;
    }

    /**
     * @return the idolRecords
     */
    public IdolRecordInfo[] getIdolRecords() {
        return idolRecords;
    }

    /**
     * @param idolRecords the idolRecords to set
     */
    public void setIdolRecords(IdolRecordInfo[] idolRecords) {
        this.idolRecords = idolRecords;
    }

    /**
     * @return the studioRecords
     */
    public StudioRecordInfo[] getStudioRecords() {
        return studioRecords;
    }

    /**
     * @param studioRecords the studioRecords to set
     */
    public void setStudioRecords(StudioRecordInfo[] studioRecords) {
        this.studioRecords = studioRecords;
    }

    /**
     * @return the totalPoint
     */
    public int getTotalPoint() {
        return totalPoint;
    }

    /**
     * @param totalPoint the totalPoint to set
     */
    public void setTotalPoint(int totalPoint) {
        this.totalPoint = totalPoint;
    }

    /**
     * @return the freeMinutesOfAward
     */
    public int getFreeMinutesOfAward() {
        return freeMinutesOfAward;
    }

    /**
     * @param freeMinutesOfAward the freeMinutesOfAward to set
     */
    public void setFreeMinutesOfAward(int freeMinutesOfAward) {
        this.freeMinutesOfAward = freeMinutesOfAward;
    }

    /**
     * @return the dayUsingOfAward
     */
    public int getDayUsingOfAward() {
        return dayUsingOfAward;
    }

    /**
     * @param dayUsingOfAward the dayUsingOfAward to set
     */
    public void setDayUsingOfAward(int dayUsingOfAward) {
        this.dayUsingOfAward = dayUsingOfAward;
    }

    /**
     * @return the minimumPointToAward
     */
    public int getMinimumPointToAward() {
        return minimumPointToAward;
    }

    /**
     * @param minimumPointToAward the minimumPointToAward to set
     */
    public void setMinimumPointToAward(int minimumPointToAward) {
        this.minimumPointToAward = minimumPointToAward;
    }

    /**
     * @return the connectData
     */
    public ConnectInfo[] getConnectData() {
        return connectData;
    }

    /**
     * @param connectData the connectData to set
     */
    public void setConnectData(ConnectInfo[] connectData) {
        this.connectData = connectData;
    }

    /**
     * @return the haveHis
     */
    public int getHaveHis() {
        return haveHis;
    }

    /**
     * @param haveHis the haveHis to set
     */
    public void setHaveHis(int haveHis) {
        this.haveHis = haveHis;
    }

    /**
     * @return the mt_notify_at
     */
    public Timestamp getMt_notify_at() {
        return mt_notify_at;
    }

    /**
     * @param mt_notify_at the mt_notify_at to set
     */
    public void setMt_notify_at(Timestamp mt_notify_at) {
        this.mt_notify_at = mt_notify_at;
    }

    /**
     * @return the count_reject
     */
    public int getCount_reject() {
        return count_reject;
    }

    /**
     * @param count_reject the count_reject to set
     */
    public void setCount_reject(int count_reject) {
        this.count_reject = count_reject;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }
}
