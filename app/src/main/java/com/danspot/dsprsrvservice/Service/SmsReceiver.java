package com.danspot.dsprsrvservice.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import com.danspot.dsprsrvservice.Entity.PayInfo;
import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.danspot.dsprsrvservice.Service.DspRsrvService;
import com.danspot.dsprsrvservice.Util.MessageParser;
import com.danspot.dsprsrvservice.Util.RsrvInfoParser;
import com.danspot.dsprsrvservice.VO.MmsMessage;
import java.util.LinkedList;
import java.util.Queue;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";

    MessageParser messageParser;
    RsrvInfoParser rsrvInfoParser;
    Queue<ReservationInfo> rsvQueue;
    Context ctx;
    public SmsReceiver() {
        super();
        if(rsvQueue == null){
            rsvQueue = new LinkedList<ReservationInfo>();
        }
        if(rsrvInfoParser == null){
            rsrvInfoParser = new RsrvInfoParser();
            Log.d(TAG, "Create RsrvInfoParser success");
        }
        if(messageParser == null){
            messageParser = new MessageParser();
            Log.d(TAG, "Create SMS/MMS Parser success");
        }
        Log.d(TAG, "SmsReceiver initialized");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.d(TAG, "onReceiver sms() called.");
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Toast.makeText(context, "BOOT_COMPLETED", Toast.LENGTH_SHORT).show();
        }
        else if(intent.getAction().equals(SMS_RECEIVED)) {
            Log.d(TAG, "onReceiver sms received.");
            Bundle bundle = intent.getExtras();
            SmsMessage[] messages = messageParser.parseSmsMessage(bundle);
            for(SmsMessage m : messages){
                String origin = m.getOriginatingAddress();
                Log.d(TAG, "Sms text : " + origin);
                Log.d(TAG, "Sms text : " + m.getMessageBody());
                if("15778000".equals(m.getOriginatingAddress())){  //입금문자일 경우
                    PayInfo pInfo = rsrvInfoParser.parsePayInfo(m.getMessageBody());
                    Intent itt = new Intent(context, DspRsrvService.class);
                    itt.putExtra("pInfo", pInfo);
                    context.startForegroundService(itt);
                }
            }
        } else if(intent.getAction().equals(MMS_RECEIVED)) {
            ctx = context;
            final ReservationInfo[] info = new ReservationInfo[1];
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    info[0] = parseMMS();       //run on main thread
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent itnt = new Intent(context, DspRsrvService.class);
            itnt.putExtra("info", info[0]);
            context.startService(itnt);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private ReservationInfo parseMMS() {
        try {
            Log.d(TAG, "onReceiver mms received.");
            MmsMessage mmsMsg = messageParser.getMmsInfo(ctx);
            Log.d(TAG, "mmsMsg.fromAddr : " + mmsMsg.getFromAddress());
            Log.d(TAG, "mmsMsg.mmsText : " + mmsMsg.getMmsText());
            ReservationInfo reservationInfo = rsrvInfoParser.parseRsrvInfo(mmsMsg);
            if (reservationInfo != null) {
                return reservationInfo;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private PayInfo parseSMS(String smsText){
        Log.d(TAG, "onReceiver sms received.");
        PayInfo info = rsrvInfoParser.parsePayInfo(smsText);
        return info;
    }

}