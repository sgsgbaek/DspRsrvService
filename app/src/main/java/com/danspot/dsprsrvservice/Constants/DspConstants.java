package com.danspot.dsprsrvservice.Constants;

import com.google.api.services.calendar.CalendarScopes;

public class DspConstants {
    public static final String DSP_DB = "Danspot.db";   //DB이름
    public static final String TABLE_RESERVATION = "Reservations"; //Table 이름
    public static final String TABLE_PAYMENTS = "Payments";
    public static final int DB_VERSION = 1;			//DB 버전
    public static final String NOTIFICATION_CHANNEL_ID = "DanspotRes";
    public static final String TYPE_MMS = "MMS";
    public static final String TYPE_SMS = "SMS";
    public static final String APPLICATION_NAME = "com.danspot.dsprsrvservice";

    public static final String[] SCOPES = {CalendarScopes.CALENDAR};
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final String PREF_ACCOUNT_NAME = "accountName";
}
