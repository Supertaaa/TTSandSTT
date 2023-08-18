/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.common;

/**
 *
 * @author User
 */
public interface Constants {

    public static String WRONG_PARAM = "-2";
    public static String SYSTEM_ERROR = "-1";
    public static String SUCCESS = "0";
    public static String NO_DATA_FOUND = "1";
    public static final String DATA_EXIST = "1";
    public static String BLACK_LIST = "101";
    public static String BALANCE_NOT_ENOUGH = "401";
    public static String ERROR_CODE = "errorCode";
    public static String ERROR_REJECT = "404";
    // Billing
    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_LOCK = 0;
    /*
     * Charge type locking
     */
    public static final Integer CHARGE_TYPE_SUB = 0;
    public static final Integer CHARGE_TYPE_GIFT = 1;
    /*
     * Friendly
     */
    public static final String MSG_PUSH_KEY = "MSG_PUSH";
    public static final String MSG_UNREAD_VOICE_PREFIX = "msg_unread_";
    public static final int MSG_APPROVED_STATUS = 1;
    public static final int MSG_NOT_APPROVE_STATUS = 0;
    public static final int MSG_UNREAD_STATUS = 0;
    public static final int MSG_READED_STATUS = 1;
    public static final int MSG_VOICE_TYPE = 1;
    public static final int MSG_SMS_TYPE = 2;
    public static final int PROFILE_STATUS_REMOVE = -1;
    public static final int PROFILE_STATUS_WAITING = 0;
    public static final int PROFILE_STATUS_ACTIVE = 1;
    public static final int PROFILE_STATUS_GOOD = 2;
    public static final int PROFILE_STATUS_BAD = 3;
    public static final int PROFILE_STATUS_WEAK = 4;
    public static final int INTERACT_ACCESS_MAIL = 1;
    public static final int INTERACT_SEARCH_FRIEND = 2;
    public static final int STATUS_PUBLIC = 1;
    public static final int PLAY_BY_ORDER = 1;
    public static final int PLAY_BY_HISTORY = 2;
    public static final int TELCO_VINA = 2;
    /*
     * Sub Point
     */
    public static final int POINT_ACTION_DECLARE_PROFILE = 9;
    public static final int INTERACT_SMS_CHAT = 3;
    /*
     * News
     */
    public static final int NEWS_CHANNEL_STATUS_PUBLIC = 1;
    public static final int NEWS_STATUS_PUBLIC = 1;
    public static int NEW_TOPIC_TYPE = 3;
    public static int LOT_STATUS_CALLOUT_REG_ONE_DAY = 2;
    public static int LOT_STATUS_CALLOUT_UNREG = 0;
    /*
     * Minigame
     */
    public static final int GAME_KEY_LUCKY = 1;
    public static final int GAME_KEY_LOVE = 2;
    public static final int GAME_KEY_HEALTH = 3;
    public static final int GAME_KEY_MUSIC = 4;
    public static final int GAME_KEY_CHOICE = 5;
    public static final int GAME_KEY_SPORT = 6;
    public static final int GAME_HIS_RESULT_REJECT = 0;
    public static final int GAME_HIS_RESULT_OK = 1;
    public static final int GAME_HIS_RESULT_FAILED = 2;
    public static final int AWARD_TYPE_POINT = 0;
    public static final int AWARD_TYPE_MINUTE = 1;
    public static final int AWARD_TYPE_CARD = 2;
    /*
     * Sub Point
     */
    public static final int POINT_ACTION_LUCKY_GAME = 12;
    public static final int POINT_ACTION_LOVE_GAME = 13;
    public static final int POINT_ACTION_HEALTH_GAME = 14;
    public static final int POINT_ACTION_MUSIC_GAME = 15;
    public static final int POINT_ACTION_EXCHANGE = 16;
    public static final int POINT_ACTION_IDOL = 17;
    public static final int POINT_ACTION_STUDIO = 18;
    
    
    /**
     * Game intellectual
     */
    public static final int ACTION_ANSWER = 1;
    public static final int ACTION_CHOOSE_START = 2;
    public static final int MAX_QUESTION_BY_USER = 10;
    public static final int GAME_TYPE_INTELLECTUAL = 1;
    public static final int LIMIT_QUESTION = 10;
    public static final int PLUS_POINT = 1;
    public static final int SUB_POINT = 2;
    public static final int RESET_POINT = 3;
    public static final int CORRECT_ANSWER = 1;
    public static final int FAILED_ANSWER = 0;


    /*
     * LIVE_SHOW
     */
    public static String SUCCESS_ONCE_DATA = "0";
    public static String SUCCESS_MORE_DATA = "2";
    public static String SUCCESS_MORE_CALENDAR = "3";
   

    public enum Telco {

        MOBIFONE(0), VIETTEL(1), VINAPHONE(2);
        private int value;

        public int getValue() {
            return value;
        }

        private Telco(int value) {
            this.value = value;
        }

        public static boolean isValid(int val) {
            boolean valid = false;
            for (Telco item : Telco.values()) {
                if (item.getValue() == val) {
                    valid = true;
                    break;
                }
            }
            return valid;
        }
    }
}
