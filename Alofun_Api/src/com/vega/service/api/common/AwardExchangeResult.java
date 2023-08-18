package com.vega.service.api.common;

import com.vega.service.api.object.BillingErrorCode;

public class AwardExchangeResult {

    private int errorCode;
    private int totalPoint;

    public int getTotalPoint() {
        return totalPoint;
    }

    public void setTotalPoint(int totalPoint) {
        this.totalPoint = totalPoint;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
