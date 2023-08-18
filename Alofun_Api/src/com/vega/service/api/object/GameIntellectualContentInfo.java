/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.object;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Nora
 */
public class GameIntellectualContentInfo {

    private int index;
    private int id_game;
    private String question_name;
    private String path_question;
    private String answer_correct;
    private int status;
    private String created_date;
    private String updated_date;
    
    private List<String> listAnswer;
    

    public GameIntellectualContentInfo(int index, int id_game, String question_name, String path_question, List<String> listAnswer , String answer_correct) {
        this.index = index;
        this.id_game = id_game;
        this.path_question = path_question;
        this.listAnswer = listAnswer;
        this.answer_correct = answer_correct;
        this.question_name = question_name;
    }

    public int getId_game() {
        return id_game;
    }

    public void setId_game(int id_game) {
        this.id_game = id_game;
    }

    public String getQuestion_name() {
        return question_name;
    }

    public void setQuestion_name(String question_name) {
        this.question_name = question_name;
    }

    public String getPath_question() {
        return path_question;
    }

    public void setPath_question(String path_question) {
        this.path_question = path_question;
    }

    public List<String> getListAnswer() {
        return listAnswer;
    }

    public void setListAnswer(List<String> listAnswer) {
        this.listAnswer = listAnswer;
    }


    public String getAnswer_correct() {
        return answer_correct;
    }

    public void setAnswer_correct(String answer_correct) {
        this.answer_correct = answer_correct;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreated_date() {
        return created_date;
    }

    public void setCreated_date(String created_date) {
        this.created_date = created_date;
    }

    public String getUpdated_date() {
        return updated_date;
    }

    public void setUpdated_date(String updated_date) {
        this.updated_date = updated_date;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    
    

}
