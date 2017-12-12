package com.github.onsdigital.search.mongo.models;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/*
Simple immutable class to compute the difference in time between two Date objects
 */
public final class Duration {

    private final Date date1;
    private final Date date2;

    public Duration(final Date date1, final Date date2) {
        // date2 should be the later date
        this.date1 = date1;
        this.date2 = date2;
    }

    public long getDuration() {
        return getDuration(TimeUnit.MILLISECONDS);
    }

    public long getDuration(TimeUnit timeUnit) {
        long duration = this.date2.getTime() - this.date1.getTime();
        return  timeUnit.convert(duration, TimeUnit.MILLISECONDS);
    }

}
