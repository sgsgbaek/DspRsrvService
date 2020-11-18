package com.danspot.dsprsrvservice.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import com.danspot.dsprsrvservice.VO.MmsMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MessageParser {
    private static final String TAG = "Sms/Mms Parser";

    public MmsMessage getMmsInfo(Context context){
        MmsMessage mmsMsg = new MmsMessage();
        List<String> mmsIds = getMmsId(context);
        String mmsId = mmsIds.get(0);
        String mmsAddr = getAddressNumber(context, Integer.parseInt(mmsId));
        String mmsText = messageFromMms(context, mmsId);
        Log.d(TAG, "mmsId: " + mmsId);
        Log.d(TAG, "mmsAddr: " + mmsAddr);
        Log.d(TAG, "mmsText: " + mmsText);
        mmsMsg.setFromAddress(mmsAddr);
        mmsMsg.setMmsText(mmsText);
        return mmsMsg;
    }

    public SmsMessage[] parseSmsMessage(Bundle bundle){
        Object[] objs = (Object[]) bundle.get("pdus");
        SmsMessage[] messages = new SmsMessage[objs.length];

        int smsCount = objs.length;
        for(int i=0; i<smsCount; i++){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                String format = bundle.getString("format");
                messages[i] = SmsMessage.createFromPdu((byte[]) objs[i], format);
            } else {
                messages[i] = SmsMessage.createFromPdu((byte[]) objs[i]);
            }
        }
        return messages;
    }

    private List<String> getMmsId(Context context) {
        try {
            List<String> idList = new ArrayList<String>();
            List<String> numList = new ArrayList<String>();
            List<String> textList = new ArrayList<String>();
            idList.clear();
            numList.clear();
            textList.clear();
            final String[] projection = new String[]{"_id"};
            Uri uri = Uri.parse("content://mms/inbox");
            Cursor query = context.getContentResolver().query(uri, projection, null, null, "date DESC");
            if (query.moveToFirst()) {
                do {
                    String mmsId = query.getString(query.getColumnIndex("_id"));
                    idList.add(mmsId);
                } while (query.moveToNext());

            }
            query.close();
            return idList;
        } catch (Exception e){
            Log.d(TAG, "getMmsId : " + e.toString());
            e.printStackTrace();
        }
        return null;
    }
    public String getAddressNumber(Context context, int id) {
        String selectionAdd = new String("msg_id=" + id);
        String uriStr = MessageFormat.format("content://mms/{0}/addr", Integer.toString(id)); // id를 형변환해주지 않으면, 천단위 넘어가면 콤마가 붙으므로 오류가 나게 된다.
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = context.getContentResolver().query(uriAddress, null, selectionAdd, null, null);
        String name = null;
        if (cAdd.moveToFirst()) {
            do {
                String number = cAdd.getString(cAdd.getColumnIndex("address"));
                if (number != null) {
                    try {
                        Long.parseLong(number.replace("-", ""));
                        name = number;
                    }
                    catch (NumberFormatException nfe) {
                        if (name == null) {
                            name = number;
                        }
                    }
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        return name;
    }
    private String getMmsText(ContentResolver contentResolver, String id) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = contentResolver.openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    // if (sb.length() > 100) break;
                    temp = reader.readLine();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString().trim();
    }

    private String messageFromMms(Context context, String mmsId) {
        String selectionPart = "mid=" + mmsId;
        Uri uriPart = Uri.parse("content://mms/part");
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursorPart = contentResolver.query(uriPart, null, selectionPart, null, null);
        String messageBody = "";
        if (cursorPart.moveToFirst()) {
            do {
                String partId = cursorPart.getString(cursorPart.getColumnIndex("_id"));
                String type = cursorPart.getString(cursorPart.getColumnIndex("ct"));
                if ("text/plain".equals(type)) {
                    String data = cursorPart.getString(cursorPart.getColumnIndex("_data"));
                    if (data != null) { messageBody += "\n" + getMmsText(contentResolver, partId); }
                    else { messageBody += "\n" + cursorPart.getString(cursorPart.getColumnIndex("text")); }
                }
            } while( cursorPart.moveToNext() );
            cursorPart.close();
        }
        if (! TextUtils.isEmpty(messageBody)) messageBody = messageBody.substring(1);
        return messageBody;
    }
}
