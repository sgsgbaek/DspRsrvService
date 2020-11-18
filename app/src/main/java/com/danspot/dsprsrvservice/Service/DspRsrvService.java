package com.danspot.dsprsrvservice.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.danspot.dsprsrvservice.Constants.DspConstants;
import com.danspot.dsprsrvservice.DAO.DspDatabaseManager;
import com.danspot.dsprsrvservice.Entity.PayInfo;
import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.danspot.dsprsrvservice.MainActivity;
import com.danspot.dsprsrvservice.R;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.FutureTask;

public class DspRsrvService extends Service {
    final String TAG = "DspRsrvService";
    int notinum;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        notinum = 1;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            NotificationChannel channel;
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            channel = new NotificationChannel(DspConstants.NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            if(notificationManager.getNotificationChannel(DspConstants.NOTIFICATION_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "createNotificationChannel: success.");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        try {
            ReservationInfo info = (ReservationInfo) intent.getSerializableExtra("info");
            PayInfo pInfo = (PayInfo)intent.getSerializableExtra("pInfo");
            ReservationInfo completeInfo = (ReservationInfo) intent.getSerializableExtra("completeInfo");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //add to database
            DspDatabaseManager dspDatabaseManager = DspDatabaseManager.getInstance(this);
            if(info != null){
                NaverMessageSender sender = new NaverMessageSender(DspConstants.TYPE_MMS, info, 1);
                FutureTask futureTask = new FutureTask(sender);
                Thread thread = new Thread(futureTask);
                thread.start();
                String result = (String)futureTask.get();

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DspConstants.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.dsp)
                        .setContentTitle("댄스팟 예약 입금문자 발송 완료 : ")// + infoQ.size())
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("이름:"+ info.getName() + "\n번호:"+info.getToAddr() + "\n결과:"+result
                                + "\n금액:"+ info.getPrice() + "\n시간:"+ info.getFmtdDate()+ "|" + info.getFromTime()
                                +"~"+info.getToTime()))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[]{1000, 2000, 1000, 3000, 1000, 4000})
                        .setAutoCancel(true);
                Notification notification = builder.build();
                notificationManager.notify(notinum, notification);
                notinum++;
                if(notinum > 1000) notinum = 1;

                ContentValues contentValues = new ContentValues();
                contentValues.put("resId", info.getResId());
                contentValues.put("name", info.getName());
                contentValues.put("fromAddr", info.getFromAddr());
                contentValues.put("toAddr", info.getToAddr());
                contentValues.put("hall", info.getHall());
                contentValues.put("date", info.getFmtdDate());
                contentValues.put("fromTime", info.getFromTime());
                contentValues.put("toTime", info.getToTime());
                contentValues.put("createDt", info.getCreateDt());
                contentValues.put("price", info.getPrice());
                contentValues.put("status", info.getStatus());

                dspDatabaseManager.insert(DspConstants.TABLE_RESERVATION, contentValues);
            }

            if(pInfo != null){
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DspConstants.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.dsp)
                        .setContentTitle("댄스팟 예약 입금 : ")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("이름:"+ pInfo.getName() + "\n금액:"+pInfo.getPrice()
                                + "\n시간:"+ pInfo.getMonth()+pInfo.getDay()+pInfo.getHour()+pInfo.getMin()))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[]{1000, 2000, 1000, 3000, 1000, 4000})
                        .setAutoCancel(true);
                Notification notification = builder.build();
                notificationManager.notify(notinum, notification);
                notinum++;
                if(notinum > 1000) notinum = 1;
                Date toDay = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String today = dateFormat.format(toDay);
                Log.d(TAG, "current time : " + today);

                ContentValues contentValues = new ContentValues();
                contentValues.put("name", pInfo.getName());
                contentValues.put("payDate", pInfo.getDate());
                contentValues.put("payTime", pInfo.getTime());
                contentValues.put("createDt", today);
                contentValues.put("price", pInfo.getPrice());
                //contentValues.put("paid", pInfo.getPaid());
                dspDatabaseManager.insert(DspConstants.TABLE_PAYMENTS, contentValues);
                String query = "select * from Reservations where name=\""+pInfo.getName()+"\" and price=\""+pInfo.getPrice()+"\" and " +
                        "status=\"notpaid\" and date >= Date(\"" + today + "\")";
                Log.d(TAG, query);
                List<ReservationInfo> resArr = null;
                resArr = dspDatabaseManager.selectReservation(query);
                for(ReservationInfo res : resArr){
                    Log.d(TAG, res.toString());
                }
                Intent resIntent = new Intent(getApplicationContext(), MainActivity.class);
                resIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                resIntent.putExtra("resArr", (Serializable) resArr);
                startActivity(resIntent);
            }

            if(completeInfo != null){
                NaverMessageSender sender = null;
                if(completeInfo.getHall().contains("X홀"))
                    sender = new NaverMessageSender(DspConstants.TYPE_MMS, completeInfo, 2);
                else
                    sender = new NaverMessageSender(DspConstants.TYPE_MMS, completeInfo, 3);
                FutureTask futureTask = new FutureTask(sender);
                Thread thread = new Thread(futureTask);
                thread.start();
                String result = (String)futureTask.get();
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DspConstants.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.dsp)
                        .setContentTitle("댄스팟 예약완료 문자 발송 완료 : ")// + infoQ.size())
                        .setStyle(new NotificationCompat.BigTextStyle().bigText("이름:"+ completeInfo.getName() + "\n번호:"+completeInfo.getToAddr() + "\n결과:"+result
                                + "\n금액:"+ completeInfo.getPrice() + "\n시간:"+ completeInfo.getFmtdDate()+ "|" + completeInfo.getFromTime()
                                +"~"+completeInfo.getToTime()))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setVibrate(new long[]{1000, 2000, 1000, 3000, 1000, 4000})
                        .setAutoCancel(true);
                Notification notification = builder.build();
                notificationManager.notify(notinum, notification);
                notinum++;
                if(notinum > 1000) notinum = 1;
            }
            return START_STICKY;
        } catch (Exception e){
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public DspRsrvService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}