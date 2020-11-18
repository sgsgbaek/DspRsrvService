package com.danspot.dsprsrvservice;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import com.amitshekhar.DebugDB;
import com.danspot.dsprsrvservice.Constants.DspConstants;
import com.danspot.dsprsrvservice.DAO.DspDatabaseManager;
import com.danspot.dsprsrvservice.Entity.PayInfo;
import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.danspot.dsprsrvservice.Recycler.DspRecyclerAdapter;
import com.danspot.dsprsrvservice.Recycler.RecyclerDecoration;
import com.danspot.dsprsrvservice.Service.DspRsrvService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AutoPermissionsListener {
    GoogleAccountCredential mCredential;
    private  int mID = 0;
    ProgressDialog mProgress;
    RecyclerView mRecyclerView;
    DspRecyclerAdapter mAdapter;
    List<ReservationInfo> mList;

    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoPermissions.Companion.loadAllPermissions(this, 101);
        Intent intent = new Intent(getApplicationContext(), DspRsrvService.class);
        intent.putExtra("name", "DanspotReservation");
        startForegroundService(intent);
        DebugDB.getAddressLog();

        // Google Calendar API 호출중에 표시되는 ProgressDialog
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Google Calendar API 호출 중입니다.");

        Toast.makeText(MainActivity.this, "APK install Success.. ", Toast.LENGTH_SHORT).show();
        // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
        // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(DspConstants.SCOPES)
        ).setBackOff(new ExponentialBackOff()); // I/O 예외 상황을 대비해서 백오프 정책 사용

        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
        if(mRecyclerView == null)
            mRecyclerView = findViewById(R.id.recyclerView);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setBackgroundColor(R.color.white);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this)) ;
        RecyclerDecoration spaceDecoration = new RecyclerDecoration(50);
        mRecyclerView.addItemDecoration(spaceDecoration);
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(mRecyclerView.getContext(),new LinearLayoutManager(this).getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        // 리사이클러뷰에 표시할 데이터 리스트 생성.
        if(mList == null)
            mList = new ArrayList<ReservationInfo>();

        if(mAdapter == null)
            mAdapter = new DspRecyclerAdapter(this, mList, mCredential, mProgress, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DspDatabaseManager dspDatabaseManager = DspDatabaseManager.getInstance(this);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE,-1);
        String yesterDay = dateFormat.format(cal.getTime());
        Log.d("", "yesterDay : " + yesterDay);
        String paymentQuery = "select * from Payments where payDate >= Date(\"" + yesterDay + "\")"; //하루전꺼 처리안된것 모두쿼리
        List<PayInfo> payArr = null;
        payArr = dspDatabaseManager.selectPayments(paymentQuery);
        int size = mList.size();
        mList.clear();
        mAdapter.notifyItemRangeRemoved(0, size);
        Date toDay = new Date();
        String today = dateFormat.format(toDay);
        for(PayInfo p : payArr){
            String query = "select * from Reservations where name=\""+p.getName()+"\" and price=\""+p.getPrice()+"\" and " +
            "status=\"notpaid\" and date >= Date(\"" + today + "\")";
            Log.d("TAG", query);
            List<ReservationInfo> resArr = null;
            resArr = dspDatabaseManager.selectReservation(query);
            if (resArr != null) {
                for (ReservationInfo res : resArr) {
                    if(!mList.contains(res)) {
                        mList.add(0, res);
                        mAdapter.notifyItemInserted(0);
                    }
                }
            }
        }
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
    private String getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) { // Google Play Services를 사용할 수 없는 경우
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) { // 유효한 Google 계정이 선택되어 있지 않은 경우
            chooseAccount();
        } else if (!isDeviceOnline()) {    // 인터넷을 사용할 수 없는 경우
            Toast.makeText(MainActivity.this, "사용 가능한 인터넷 연결이 없습니다."
                    , Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    /**
     * 안드로이드 디바이스에 최신 버전의 Google Play Services가 설치되어 있는지 확인
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            ArrayList<ReservationInfo> resArr = (ArrayList<ReservationInfo>) intent.getSerializableExtra("resArr");
            if (resArr != null) {
                for (ReservationInfo res : resArr) {
                    mList.add(0, res);
                    mAdapter.notifyItemInserted(0);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();;
        }
    }

    /*
     * Google Play Services 업데이트로 해결가능하다면 사용자가 최신 버전으로 업데이트하도록 유도하기위해
     * 대화상자를 보여줌.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }
    /*
     * 안드로이드 디바이스에 Google Play Services가 설치 안되어 있거나 오래된 버전인 경우 보여주는 대화상자
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                DspConstants.REQUEST_GOOGLE_PLAY_SERVICES
        );
        dialog.show();
    }
    private void chooseAccount() {
        // GET_ACCOUNTS 권한을 가지고 있다면
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED){
            // 사용자가 구글 계정을 선택할 수 있는 다이얼로그를 보여준다.
            startActivityForResult(
                    mCredential.newChooseAccountIntent(),
                    DspConstants.REQUEST_ACCOUNT_PICKER);
        } else {
            // 사용자에게 GET_ACCOUNTS 권한을 요구하는 다이얼로그를 보여준다.(주소록 권한 요청함)
            AutoPermissions.Companion.loadSelectedPermission(this, 101, Manifest.permission.GET_ACCOUNTS);
            chooseAccount();
        }
    }

    /*
     * 안드로이드 디바이스가 인터넷 연결되어 있는지 확인한다. 연결되어 있다면 True 리턴, 아니면 False 리턴
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DspConstants.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(MainActivity.this, " 앱을 실행시키려면 구글 플레이 서비스가 필요합니다."
                            + " 구글 플레이 서비스를 설치 후 다시 실행하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case DspConstants.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(DspConstants.PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case DspConstants.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int requestCode, String[] permissions) {
        Toast.makeText(this, "permission denied : " + permissions.length, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGranted(int requestCode, String[] permissions) {
        Toast.makeText(this, "permission granted : " + permissions.length, Toast.LENGTH_LONG).show();
    }

}