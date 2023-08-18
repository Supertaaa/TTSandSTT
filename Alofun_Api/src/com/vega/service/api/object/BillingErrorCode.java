package com.vega.service.api.object;

public enum BillingErrorCode {

    Success(0),
    NotFoundData(1),
    NotEnoughBalance(5),
    RegisterSamePackage(3),
    CancelPackage(4),
    ChargingSubProcessing(-100),
    BlackList(101),
    SystemError(-1),
    WrongParams(-2),
    NotPromotionBecauseRegistedNotExpired(5),
    NotPromotionBecauseRegistedButExpired(6),
    NotPromotion(7),
    Registed(8);
    int value;

    public int getValue() {
        return value;
    }

    private BillingErrorCode(int val) {
        this.value = val;
    }
}
