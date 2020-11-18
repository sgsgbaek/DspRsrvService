package com.danspot.dsprsrvservice.Entity;

import java.io.Serializable;
import lombok.Data;

public @Data
class ReservationInfo implements Serializable {
    String resId;
    String name;
    String fromAddr;
    String toAddr;
    String hall;
    String date;        //sentence include date
    String fmtdDate;    //YY-MM-DD
    String fromTime;    //HH:MM:SS
    String toTime;      //HH:MM:SS
    String createDt;
    String year;
    String month;
    String day;
    String fromHour;
    String fromMin;
    String toHour;
    String toMin;
    String priceInfo;
    String price;
    String url;
    String status;
}
