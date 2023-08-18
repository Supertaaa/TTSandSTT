package com.vega.service.api.object;

import java.util.ArrayList;
import java.util.Calendar;

public class PromtInTimeRange {

    private int hourFrom;
    private int minuteFrom;
    private int hourTo;
    private int minuteTo;
    private ArrayList<Integer> listPromtNo = new ArrayList<Integer>();

    public int getHourFrom() {
        return hourFrom;
    }

    public void setHourFrom(int hourFrom) {
        this.hourFrom = hourFrom;
    }

    public int getMinuteFrom() {
        return minuteFrom;
    }

    public void setMinuteFrom(int minuteFrom) {
        this.minuteFrom = minuteFrom;
    }

    public int getHourTo() {
        return hourTo;
    }

    public void setHourTo(int hourTo) {
        this.hourTo = hourTo;
    }

    public int getMinuteTo() {
        return minuteTo;
    }

    public void setMinuteTo(int minuteTo) {
        this.minuteTo = minuteTo;
    }

    public ArrayList<Integer> getListPromtNo() {
        return listPromtNo;
    }

    public void setListPromtNo(ArrayList<Integer> listPromtNo) {
        this.listPromtNo = listPromtNo;
    }

    public int getNextPromtNo(Calendar time, Integer lastPromtNo) {
        int promtNo = 0;

        Calendar timeFrom = Calendar.getInstance();
        timeFrom.set(Calendar.HOUR_OF_DAY, hourFrom);
        timeFrom.set(Calendar.MINUTE, minuteFrom);
        timeFrom.set(Calendar.SECOND, 0);

        Calendar timeTo = Calendar.getInstance();
        timeTo.set(Calendar.HOUR_OF_DAY, hourTo);
        timeTo.set(Calendar.MINUTE, minuteTo);
        timeTo.set(Calendar.SECOND, 0);

        if (hourFrom > hourTo) {
            timeTo.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (time.after(timeFrom) && time.before(timeTo)) {
            int idx = listPromtNo.indexOf(lastPromtNo);
            if (idx >= 0) {
                idx = (idx == listPromtNo.size() - 1) ? 0 : idx + 1;
                promtNo = listPromtNo.get(idx);
            } else if (listPromtNo.size() > 0) {
                promtNo = listPromtNo.get(0);
            }
        }

        return promtNo;
    }
}
