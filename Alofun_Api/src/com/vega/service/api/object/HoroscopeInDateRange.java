package com.vega.service.api.object;

import java.util.Calendar;

public class HoroscopeInDateRange {

    private Calendar fromDate;
    private Calendar toDate;
    private int type;

    public boolean checkInDateRange(Calendar time) {
        boolean inDateRange = false;

        time.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        getFromDate().set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        getFromDate().set(Calendar.HOUR_OF_DAY, 0);
        getFromDate().set(Calendar.MINUTE, 0);
        getFromDate().set(Calendar.SECOND, 0);
        getFromDate().set(Calendar.MILLISECOND, 0);

        getToDate().set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        getToDate().set(Calendar.HOUR_OF_DAY, 0);
        getToDate().set(Calendar.MINUTE, 0);
        getToDate().set(Calendar.SECOND, 0);
        getToDate().set(Calendar.MILLISECOND, 0);

        if ((time.after(getFromDate()) || time.equals(getFromDate()))
                && (time.before(getToDate()) || time.equals(getToDate()))) {
            inDateRange = true;
        }

        return inDateRange;
    }

    public Calendar getFromDate() {
        return fromDate;
    }

    public void setFromDate(Calendar fromDate) {
        this.fromDate = fromDate;
    }

    public Calendar getToDate() {
        return toDate;
    }

    public void setToDate(Calendar toDate) {
        this.toDate = toDate;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
