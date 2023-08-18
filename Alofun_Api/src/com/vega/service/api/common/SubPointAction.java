/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vega.service.api.common;

/**
 *
 * @author HaoNM
 */
public enum SubPointAction {        
	FirstRegisterFromGamingStarted(1),//1: Lan dau tao acc sub_point (do Dang ky lan dau hoac lan dau tao acc);
	DailyChargeSuccess(2),
	WeeklyChargeSuccess(3),
	MonthlyChargeSuccess(4),
	FirstCallInDay(5),
	CallDuration(6),
	DayUsing(7),
	ExchangeAward(8),
	DeclareProfile(9),
	InviteFriend(10),
        BirthDay(11);
        /*
	 * Sub Point
	 */	
	int value;
	
	public int getValue() {
        return value;
    }

    private SubPointAction(int val) {
        this.value = val;
    }
}
