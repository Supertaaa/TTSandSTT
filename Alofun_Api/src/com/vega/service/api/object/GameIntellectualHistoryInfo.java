/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.object;

/**
 *
 * @author Nora
 */
public class GameIntellectualHistoryInfo {

    private int id;
    private int question_id;
    private String msisdn;
    private String key_answer;
    private String time_answer;
    private String result_correct;
    private int iscorrect;
    private int order_number_answer;
    private int action;
    private int game_type;
    private String created_at;
    private String updated_at;

     public GameIntellectualHistoryInfo() {
       
    }
     
      public GameIntellectualHistoryInfo(String msisdn, int action) {
        this.msisdn = msisdn;
        this.action = action;
    }
    
    public GameIntellectualHistoryInfo(int question_id, String msisdn, String key_answer, String result_correct, int iscorrect, int order_number_answer, int action) {
        this.question_id = question_id;
        this.msisdn = msisdn;
        this.key_answer = key_answer;
        this.result_correct = result_correct;
        this.iscorrect = iscorrect;
        this.order_number_answer = order_number_answer;
        this.action = action;
    }

    
    
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestion_id() {
        return question_id;
    }

    public void setQuestion_id(int question_id) {
        this.question_id = question_id;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getKey_answer() {
        return key_answer;
    }

    public void setKey_answer(String key_answer) {
        this.key_answer = key_answer;
    }

    public String getTime_answer() {
        return time_answer;
    }

    public void setTime_answer(String time_answer) {
        this.time_answer = time_answer;
    }

    public String getResult_correct() {
        return result_correct;
    }

    public void setResult_correct(String result_correct) {
        this.result_correct = result_correct;
    }

    public int getIscorrect() {
        return iscorrect;
    }

    public void setIscorrect(int iscorrect) {
        this.iscorrect = iscorrect;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getGame_type() {
        return game_type;
    }

    public void setGame_type(int game_type) {
        this.game_type = game_type;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public int getOrder_number_answer() {
        return order_number_answer;
    }

    public void setOrder_number_answer(int order_number_answer) {
        this.order_number_answer = order_number_answer;
    }
    
}
