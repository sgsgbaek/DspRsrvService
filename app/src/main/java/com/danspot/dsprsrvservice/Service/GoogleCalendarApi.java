package com.danspot.dsprsrvservice.Service;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.danspot.dsprsrvservice.Constants.DspConstants;
import com.danspot.dsprsrvservice.DAO.DspDatabaseManager;
import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.danspot.dsprsrvservice.MainActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.pedro.library.AutoPermissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoogleCalendarApi extends AsyncTask<Void, Void, String> /*implements Callable<String>*/ {
    private Exception mLastError = null;
    private MainActivity mActivity;
    List<String> eventStrings = new ArrayList<String>();
    Calendar mService;
    ReservationInfo mInfo;
    int mID;
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    Context mContext;

    public GoogleCalendarApi(MainActivity mainActivity, Context context, GoogleAccountCredential credential, ReservationInfo info, int flag, ProgressDialog progress){
        mCredential = credential;
        mInfo = info;
        mActivity = mainActivity;
        mID = flag;
        mProgress = progress;
        mContext = context;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar
                .Builder(transport, jsonFactory, mCredential)
                .setApplicationName(DspConstants.APPLICATION_NAME)
                .build();
    }

    /**
     * 다음 사전 조건을 모두 만족해야 Google Calendar API를 사용할 수 있다.
     *
     * 사전 조건
     *     - Google Play Services 설치
     *     - 유효한 구글 계정 선택
     *     - 안드로이드 디바이스에서 인터넷 사용 가능
     *
     * 하나라도 만족하지 않으면 해당 사항을 사용자에게 알림.
     */
    public String getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) { // Google Play Services를 사용할 수 없는 경우
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) { // 유효한 Google 계정이 선택되어 있지 않은 경우
            chooseAccount();
        } else if (!isDeviceOnline()) {    // 인터넷을 사용할 수 없는 경우
            Toast.makeText(mContext, "사용 가능한 인터넷 연결이 없습니다."
                    , Toast.LENGTH_SHORT).show();
        }
        else {
            // Google Calendar API 호출
            //new MakeRequestTask(this, mCredential).execute();
            //callable 은 Calendar API 동작 안함..
            new GoogleCalendarApi(mActivity, mContext, mCredential, mInfo, 2, mProgress).execute();
        }
        return null;
    }
    /**
     * 안드로이드 디바이스에 최신 버전의 Google Play Services가 설치되어 있는지 확인
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(mContext);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }
    /*
     * Google Play Services 업데이트로 해결가능하다면 사용자가 최신 버전으로 업데이트하도록 유도하기위해
     * 대화상자를 보여줌.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(mContext);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    private void chooseAccount() {
        // GET_ACCOUNTS 권한을 가지고 있다면
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED){
            mActivity.startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    DspConstants.REQUEST_ACCOUNT_PICKER);
        } else {
            // 사용자에게 GET_ACCOUNTS 권한을 요구하는 다이얼로그를 보여준다.(주소록 권한 요청함)
            AutoPermissions.Companion.loadSelectedPermission(mActivity, 101, Manifest.permission.GET_ACCOUNTS);
            chooseAccount();
        }
    }

    /*
     * 안드로이드 디바이스가 인터넷 연결되어 있는지 확인한다. 연결되어 있다면 True 리턴, 아니면 False 리턴
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /*
     * CalendarTitle 이름의 캘린더에서 10개의 이벤트를 가져와 리턴
     */
    private String getEvent() throws Exception {
        Events events = null;
        String ret = null;
        try {
            String min = "2020-11-02T13:00:00";
            String max = "2020-11-02T15:00:00";
            DateTime minTime = new DateTime(min);
            DateTime maxTime = new DateTime(max);
            String calendarID = getCalendarID("X홀");
            if (calendarID == null) {
                return "캘린더를 먼저 생성하세요.";
            }
            events = mService.events().list(calendarID)//"primary")
                    .setMaxResults(10)
                    .setTimeZone("Asia/Seoul")
                    .setTimeMin(minTime)
                    .setTimeMax(maxTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    // 모든 이벤트가 시작 시간을 갖고 있지는 않다. 그런 경우 시작 날짜만 사용
                    start = event.getStart().getDate();
                }
                eventStrings.add(String.format("%s \n (%s)", event.getSummary(), start));
            }
            ret = eventStrings.size() + "개의 데이터를 가져왔습니다.";
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    /*
     * 캘린더 이름에 대응하는 캘린더 ID를 리턴
     */
    private String getCalendarID(String calendarTitle){
        String id = null;

        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            CalendarList calendarList = null;
            try {
                calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            } catch (UserRecoverableAuthIOException e) {
                mActivity.startActivityForResult(e.getIntent(), DspConstants.REQUEST_AUTHORIZATION);
            }catch (IOException e) {
                e.printStackTrace();
            }
            List<CalendarListEntry> items = calendarList.getItems();
            for (CalendarListEntry calendarListEntry : items) {
                if ( calendarListEntry.getSummary().toString().equals(calendarTitle)) {
                    id = calendarListEntry.getId().toString();
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return id;
    }

    private String parseCalendarName(String hall){
        String[] strArr = hall.split("\\s+| |:|\\(");
        return strArr[3];
    }

    private String addEvent() {
        String hall = parseCalendarName(mInfo.getHall());
        String calendarID = getCalendarID(hall);
        if ( calendarID == null ){
            return "캘린더를 먼저 생성하세요.";
        }
        Event event = new Event().setSummary("["+hall.charAt(0)+"] " + mInfo.getName() + "님");
        //simpledateformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ", Locale.KOREA);
        // Z에 대응하여 +0900이 입력되어 문제 생겨 수작업으로 입력
        String fromDate = mInfo.getFmtdDate()+"T"+mInfo.getFromTime()+"+09:00";
        DateTime startDateTime = new DateTime(fromDate);
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime);
        event.setStart(start);
        Log.d( "@@@", startDateTime.toString() );
        String toDate = mInfo.getFmtdDate()+"T"+mInfo.getToTime()+"+09:00";
        DateTime endDateTime = new DateTime(toDate);
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime);
        event.setEnd(end);
        try {
            event = mService.events().insert(calendarID, event).execute();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", "Exception : " + e.toString());
            return null;
        }
        System.out.printf("Event created: %s\n", event.getHtmlLink());
        Log.e("Event", "created : " + event.getHtmlLink());
        String eventStrings = "created : " + event.getHtmlLink();
        setReservationPaid(mInfo.getResId(), DspConstants.TABLE_RESERVATION);
        return eventStrings;
    }

    void setReservationPaid(String resId, String table){
        DspDatabaseManager dspDatabaseManager = DspDatabaseManager.getInstance(mContext);
        ContentValues cValues = new ContentValues();
        cValues.put("status", "paid");
        dspDatabaseManager.update(table, cValues, "resId=?", new String[]{resId});
    }

    @Override
    protected void onPreExecute() {
        // mStatusText.setText("");
        mProgress.show();
    }

    @Override
    protected void onPostExecute(String output) {
        mProgress.hide();
        if ( mID == 3 )   Toast.makeText(mActivity, TextUtils.join("\n\n", eventStrings), Toast.LENGTH_SHORT).show();
    }

    /*
     * 안드로이드 디바이스에 Google Play Services가 설치 안되어 있거나 오래된 버전인 경우 보여주는 대화상자
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode
    ) {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        Dialog dialog = apiAvailability.getErrorDialog(
                mActivity,
                connectionStatusCode,
                DspConstants.REQUEST_GOOGLE_PLAY_SERVICES
        );
        dialog.show();
    }

    @Override
    protected void onCancelled() {
        mProgress.hide();
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                mActivity.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        DspConstants.REQUEST_AUTHORIZATION);
            } else {
                Toast.makeText(mActivity, "MakeRequestTask The following error occurred:\n" + mLastError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mActivity, "요청 취소됨.", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected String doInBackground(Void... voids) {
        try {
            if ( mID == 1) {
                //return createCalendar();
            }else if (mID == 2) {
                String ret = addEvent();
                Intent itt = new Intent(mContext, DspRsrvService.class);
                itt.putExtra("completeInfo", mInfo);
                mContext.startForegroundService(itt);
                return ret;
            }
            else if (mID == 3) {
                return getEvent();
            }
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
        return null;
    }
}
