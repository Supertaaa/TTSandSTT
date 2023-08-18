package com.vega.service.api.object;

public enum SMSFunType {

    Genral(0),
    Guide(1),
    WrongSyntax(2),
    NotEnoughMoney(3),
    RegDailyPromotionOK(4),
    RegDailyOK(5),
    RegDailyReActiveOK(6),
    RegDailyNotEnoughMoney(7),
    RegDailyRegistered(8),
    ConfirmSwitchDailyToWeekly(9),
    RegDailyConfirmedOK(10),
    RegWeeklyPromotionOK(11),
    RegWeeklyOK(12),
    RegWeeklyReActiveOK(13),
    RegWeeklyNotEnoughMoney(14),
    RegWeeklyRegistered(15),
    ConfirmSwitchWeeklyToDaily(16),
    RegWeeklyConfirmedOK(17),
    CancelDailyOK(18),
    CancelWeeklyOK(19),
    CancelWhenNotReg(20),
    RegDailyPromotionNotEnoughCondition(21),
    GetPasswordWhenNotReg(22),
    GetPasswordOK(23),
    RenewCancelDailyPackage(24),
    RenewCancelWeeklyPackage(25),
    RegDailyNotEnoughMoneySuggest(26),
    RegWeeklyNotEnoughMoneySuggest(27),
    RegWeeklyPromotionNotEnoughCondition(28),
    checkDailyPackage(29),
    checkWeeklyPackage(30),
    checkNotPackage(31),
    RegMonthyPromotionOK(32),
    RegMonthyReActiveOK(33),
    RegDaily2000PromotionOK(34),
    RegDaily2000ReActiveOK(35),
    RegDaily2000OK(36),
    RegWeekly7000PromotionOK(37),
    RegWeekly7000ReActiveOK(38),
    RegWeekly7000OK(39),
    RegMonthyOK(40),
    RegDailyReNoPromotionOK(41),
    RegWeeklyReNoPromotionOK(42),
    RegMonthyReNoPromotionOK(43),
    checkMonthyPackage(44),
    checkDaily2000Package(45),
    checkWeekly7000Package(46),
    sendAnswer(47);
    int value;

    public int getValue() {
        return value;
    }

    private SMSFunType(int val) {
        this.value = val;
    }
}
