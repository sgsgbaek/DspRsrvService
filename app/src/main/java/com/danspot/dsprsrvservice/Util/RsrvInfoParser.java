package com.danspot.dsprsrvservice.Util;

import android.util.Log;

import com.danspot.dsprsrvservice.Entity.PayInfo;
import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.danspot.dsprsrvservice.VO.MmsMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class RsrvInfoParser {
    final String TAG = "RsrvInfoParser";
    public ReservationInfo parseRsrvInfo(MmsMessage mmsMsg){
        String fromAddr = mmsMsg.getFromAddress();
        String info = mmsMsg.getMmsText();
        if(info.contains("댄스팟 연습실 강남점") && info.contains("예약이 확정")) {
            ReservationInfo rsrvInfo = new ReservationInfo();
            rsrvInfo.setFromAddr(fromAddr);
            rsrvInfo.setStatus("notpaid");
            String[] line = info.split("\\n");
            String[] strArr;
            for (String str : line) {
                if(str.contains("예약번호")){
                    strArr = str.split(": ");
                    rsrvInfo.setResId(strArr[1]);
                }else if(str.contains("예약자명")){
                    strArr = str.split(": ");
                    rsrvInfo.setName(strArr[1]);
                }else if(str.contains("전화번호")){
                    strArr = str.split(": ");
                    rsrvInfo.setToAddr(strArr[1]);
                }else if(str.contains("예약상품")){
                    rsrvInfo.setHall(str);
                }else if(str.contains("이용기간")){
                    parseDate(str, rsrvInfo);
                    rsrvInfo.setDate(str);
                }else if(str.contains("결제금액")){
                    parsePrice(str, rsrvInfo);
                    rsrvInfo.setPriceInfo(str);
                }else if(str.contains("예약 내역 자세히 보기")){  //link url
                    strArr = str.split(": ");
                    rsrvInfo.setUrl(strArr[1]);
                }
            }
            return rsrvInfo;
        }
        return null;
    }

    private void parseDate(String strInfo, ReservationInfo rsrvInfo){
        String formattedDate = "";
        String fromTime = "";
        String toTime = "";
        String[] strArr = strInfo.split("\\s+| |\\.|-|~|:|\\(|\\)");
        ArrayList<String> arrayList = new ArrayList<>();
        for(String s : strArr){
            if(!s.equals("")){
                arrayList.add(s);
            }
        }
        rsrvInfo.setYear(arrayList.get(1));
        formattedDate += (arrayList.get(1)+"-");
        rsrvInfo.setMonth(arrayList.get(2));
        formattedDate += (arrayList.get(2)+"-");
        rsrvInfo.setDay(arrayList.get(3));
        formattedDate += arrayList.get(3);
        rsrvInfo.setFmtdDate(formattedDate);
        String fromDayNight = arrayList.get(5);
        String fromHour = arrayList.get(6);
        int fromHourInt = Integer.parseInt(fromHour);
        if("오전".equals(fromDayNight) && "12".equals(fromHour)){
            fromHourInt = 0;
        }
        if("오후".equals(fromDayNight) && !"12".equals(fromHour)){
            fromHourInt +=12;
        }
        rsrvInfo.setFromHour(String.format("%02d", fromHourInt));
        rsrvInfo.setFromMin(arrayList.get(7));
        fromTime = rsrvInfo.getFromHour() + ":" + rsrvInfo.getFromMin() + ":00";
        rsrvInfo.setFromTime(fromTime);

        String toDayNight = arrayList.get(8);
        String toHour = arrayList.get(9);
        int toHourInt = Integer.parseInt(toHour);
        if("오전".equals(toDayNight) && "12".equals(toHour)){
            toHourInt = 0;
        }
        if("오후".equals(toDayNight) && !"12".equals(toHour)){
            toHourInt += 12;
        }
        rsrvInfo.setToHour(String.format("%02d", toHourInt));
        rsrvInfo.setToMin(arrayList.get(10));
        toTime = rsrvInfo.getToHour() + ":" + rsrvInfo.getToMin() + ":00";
        rsrvInfo.setToTime(toTime);

        SimpleDateFormat format1 = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
        Calendar time = Calendar.getInstance();
        String now_time = format1.format(time.getTime());
        rsrvInfo.setCreateDt(now_time);
        Log.d(TAG, "parseRsrvDate: " + strArr);
    }

    private void parsePrice(String priceInfo, ReservationInfo rsrvInfo){
        String[] strArr = priceInfo.split("\\s+| ");
        ArrayList<String> arrayList = new ArrayList<>();
        for(String s : strArr){
            if(!s.equals("")){
                arrayList.add(s);
            }
        }
        String s = arrayList.get(arrayList.size()-1);
        String price = s.substring(0, s.length()-1);
        rsrvInfo.setPrice(price);
    }

    public PayInfo parsePayInfo(String smsMsg){
        PayInfo info = new PayInfo();
        String[] strArr = smsMsg.split("\\s+| |/|:|\\n");
        info.setMonth(strArr[1].substring(2));
        info.setDay(strArr[2]);
        info.setHour(strArr[3]);
        info.setMin(strArr[4]);
        info.setPrice(strArr[7]);
        info.setName(strArr[strArr.length-1]);  //맨뒤가 이름
        Date toDay = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy");
        String paidDate = df.format(toDay) + "-" + info.getMonth() + "-" + info.getDay();
        String paidTime = info.getHour() + ":" + info.getMin() + ":00";
        info.setDate(paidDate);
        info.setTime(paidTime);
        //info.setPaid("false");
        return info;
    }
}
