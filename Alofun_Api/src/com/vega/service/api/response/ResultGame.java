/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vega.service.api.response;

import com.vega.service.api.object.GameIntellectualContentInfo;
import java.util.List;

/**
 *
 * @author Nora
 */
public class ResultGame {

    /*
     * Ma loi ket qua request
     */
    private String errorCode;

    private String desc;

    private int point;

    private int actionStar;

    private List<GameIntellectualContentInfo> data;

    private List<Integer> dataAnswerCorrect;

    private List<Integer> dataAnswerFailed;

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public List<GameIntellectualContentInfo> getData() {
        return data;
    }

    public void setData(List<GameIntellectualContentInfo> data) {
        this.data = data;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public List<Integer> getDataAnswerCorrect() {
        return dataAnswerCorrect;
    }

    public void setDataAnswerCorrect(List<Integer> dataAnswerCorrect) {
        this.dataAnswerCorrect = dataAnswerCorrect;
    }

    public List<Integer> getDataAnswerFailed() {
        return dataAnswerFailed;
    }

    public void setDataAnswerFailed(List<Integer> dataAnswerFailed) {
        this.dataAnswerFailed = dataAnswerFailed;
    }

    public int getActionStar() {
        return actionStar;
    }

    public void setActionStar(int actionStar) {
        this.actionStar = actionStar;
    }

}
