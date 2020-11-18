package com.danspot.dsprsrvservice.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.danspot.dsprsrvservice.Constants.DspConstants;
import com.danspot.dsprsrvservice.Entity.PayInfo;
import com.danspot.dsprsrvservice.Entity.ReservationInfo;

import java.util.ArrayList;
import java.util.List;

public class DspDatabaseManager {
    Context myContext = null;
    private static DspDatabaseManager dspDBManager = null;
    private SQLiteDatabase dspDatabase = null;

    //MovieDatabaseManager 싱글톤 패턴으로 구현
    public static DspDatabaseManager getInstance(Context context)
    {
        if(dspDBManager == null)
            dspDBManager = new DspDatabaseManager(context);
        return dspDBManager;
    }

    private DspDatabaseManager(Context context) {
        myContext = context;
        //DB Open
        dspDatabase = context.openOrCreateDatabase(DspConstants.DSP_DB, context.MODE_PRIVATE,null);
        //Table 생성
        dspDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + DspConstants.TABLE_RESERVATION +
                "(" + "resId TEXT PRIMARY KEY," +
                "name TEXT," +
                "fromAddr TEXT," +
                "toAddr TEXT," +
                "hall TEXT," +
                "date TEXT," +
                "fromTime TEXT," +
                "toTime TEXT," +
                "createDt TEXT," +
                "price TEXT," +
                "status TEXT);");
        //Table 생성
        dspDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + DspConstants.TABLE_PAYMENTS +
                "(" + "paymentId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "payDate TEXT," +
                "payTime TEXT," +
                "createDt TEXT," +
                "price TEXT);");
    }

    public long insert(String table, ContentValues addRowValue) {
        return dspDatabase.insert(table, null, addRowValue);
    }

    public Cursor query(String table, String[] colums, String selection, String[] selectionArgs, String groupBy,
                        String having, String orderby) {
        return dspDatabase.query(table, colums, selection, selectionArgs, groupBy, having, orderby);
    }

    public List<ReservationInfo> selectReservation(String sqlQuery) {
        List<ReservationInfo> resArr = new ArrayList<ReservationInfo>();
        Cursor c = dspDatabase.rawQuery(sqlQuery,null);
        while(c.moveToNext()){
            ReservationInfo info = new ReservationInfo();
            int colIdx = c.getColumnIndex("resId");
            info.setResId(c.getString(colIdx));
            colIdx = c.getColumnIndex("name");
            info.setName(c.getString(colIdx));
            colIdx = c.getColumnIndex("fromAddr");
            info.setFromAddr(c.getString(colIdx));
            colIdx = c.getColumnIndex("toAddr");
            info.setToAddr(c.getString(colIdx));
            colIdx = c.getColumnIndex("hall");
            info.setHall(c.getString(colIdx));
            colIdx = c.getColumnIndex("date");
            info.setFmtdDate(c.getString(colIdx));
            colIdx = c.getColumnIndex("fromTime");
            info.setFromTime(c.getString(colIdx));
            colIdx = c.getColumnIndex("toTime");
            info.setToTime(c.getString(colIdx));
            colIdx = c.getColumnIndex("createDt");
            info.setCreateDt(c.getString(colIdx));
            colIdx = c.getColumnIndex("price");
            info.setPrice(c.getString(colIdx));
            colIdx = c.getColumnIndex("status");
            info.setStatus(c.getString(colIdx));
            resArr.add(info);
        }
        return resArr;
    }

    public List<PayInfo> selectPayments(String sqlQuery){
        List<PayInfo> resArr = new ArrayList<PayInfo>();
        Cursor c = dspDatabase.rawQuery(sqlQuery,null);
        while(c.moveToNext()){
            PayInfo info = new PayInfo();
            int colIdx = c.getColumnIndex("paymentId");
            info.setPaymentId(c.getInt(colIdx));
            colIdx = c.getColumnIndex("name");
            info.setName(c.getString(colIdx));
            colIdx = c.getColumnIndex("payDate");
            info.setDate(c.getString(colIdx));
            colIdx = c.getColumnIndex("payTime");
            info.setTime(c.getString(colIdx));
            colIdx = c.getColumnIndex("createDt");
            info.setCreateDt(c.getString(colIdx));
            colIdx = c.getColumnIndex("price");
            info.setPrice(c.getString(colIdx));
            resArr.add(info);
        }
        return resArr;
    }

    public int update(String table, ContentValues updateRowValue, String whereClause, String[] whereArgs) {
        return dspDatabase.update(table,
                updateRowValue,
                whereClause,
                whereArgs);
    }

    public int delete(String whereClause, String[] whereArgs) {
        return dspDatabase.delete(DspConstants.TABLE_RESERVATION,
                whereClause,
                whereArgs);
    }
}
