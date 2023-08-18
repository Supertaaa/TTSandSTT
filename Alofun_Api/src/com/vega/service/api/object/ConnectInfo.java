package com.vega.service.api.object;

import java.sql.Timestamp;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlRootElement(name = "ConnectInfo")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class ConnectInfo {

    private int connect_content_id;
    private String content_path;
    private String content_question;
    private String list_key;
    private int key_right;
    private Timestamp start_date_question;
    private Timestamp end_date_question;
    private int connect_status;
    private Timestamp created_at;
    private Timestamp updated_at;
    private int answerQuestion = -1;
    /** answerQuestion
     * 0 chưa trl ,
     * 1 là đã trl ,
     * 2 là k đc trl do hết hạn trl,
     * 3 khách hàng từ chối tham gia trl
     */   
    /**
     * @return the connect_content_id
     */
    public int getConnect_content_id() {
        return connect_content_id;
    }

    /**
     * @param connect_content_id the connect_content_id to set
     */
    public void setConnect_content_id(int connect_content_id) {
        this.connect_content_id = connect_content_id;
    }

    /**
     * @return the content_path
     */
    public String getContent_path() {
        return content_path;
    }

    /**
     * @param content_path the content_path to set
     */
    public void setContent_path(String content_path) {
        this.content_path = content_path;
    }

    /**
     * @return the content_question
     */
    public String getContent_question() {
        return content_question;
    }

    /**
     * @param content_question the content_question to set
     */
    public void setContent_question(String content_question) {
        this.content_question = content_question;
    }

    /**
     * @return the list_key
     */
    public String getList_key() {
        return list_key;
    }

    /**
     * @param list_key the list_key to set
     */
    public void setList_key(String list_key) {
        this.list_key = list_key;
    }

    /**
     * @return the key_right
     */
    public int getKey_right() {
        return key_right;
    }

    /**
     * @param key_right the key_right to set
     */
    public void setKey_right(int key_right) {
        this.key_right = key_right;
    }

    /**
     * @return the start_date_question
     */
    public Timestamp getStart_date_question() {
        return start_date_question;
    }

    /**
     * @param start_date_question the start_date_question to set
     */
    public void setStart_date_question(Timestamp start_date_question) {
        this.start_date_question = start_date_question;
    }

    /**
     * @return the end_date_question
     */
    public Timestamp getEnd_date_question() {
        return end_date_question;
    }

    /**
     * @param end_date_question the end_date_question to set
     */
    public void setEnd_date_question(Timestamp end_date_question) {
        this.end_date_question = end_date_question;
    }

    /**
     * @return the connect_status
     */
    public int getConnect_status() {
        return connect_status;
    }

    /**
     * @param connect_status the connect_status to set
     */
    public void setConnect_status(int connect_status) {
        this.connect_status = connect_status;
    }

    /**
     * @return the created_at
     */
    public Timestamp getCreated_at() {
        return created_at;
    }

    /**
     * @param created_at the created_at to set
     */
    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    /**
     * @return the updated_at
     */
    public Timestamp getUpdated_at() {
        return updated_at;
    }

    /**
     * @param updated_at the updated_at to set
     */
    public void setUpdated_at(Timestamp updated_at) {
        this.updated_at = updated_at;
    }

    /**
     * @return the answerQuestion
     */
    public int getAnswerQuestion() {
        return answerQuestion;
    }

    /**
     * @param answerQuestion the answerQuestion to set
     */
    public void setAnswerQuestion(int answerQuestion) {
        this.answerQuestion = answerQuestion;
    }
 
}
