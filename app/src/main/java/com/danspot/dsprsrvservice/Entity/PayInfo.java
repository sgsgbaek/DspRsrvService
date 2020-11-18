package com.danspot.dsprsrvservice.Entity;

import java.io.Serializable;
import lombok.Data;

public @Data
class PayInfo implements Serializable {
    int paymentId;
    String name;
    String price;
    String month;
    String day;
    String hour;
    String min;
    //String paid;    //false/true
    String date;    //yyyy-MM-DD
    String time;    //hh:mm:ss
    String createDt;
}
