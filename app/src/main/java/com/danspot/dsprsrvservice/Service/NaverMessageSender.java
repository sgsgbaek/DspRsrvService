package com.danspot.dsprsrvservice.Service;

import android.util.Base64;
import android.util.Log;

import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class NaverMessageSender implements Callable<String>{
    private static final String TAG = "NaverMessageSender";
    ReservationInfo reservationInfo;
    String type;
    int mMode;

    public NaverMessageSender(String tp, ReservationInfo info, int mode){
        type = tp;
        reservationInfo = info;
        mMode = mode;
    }

    @Override
    public String call() throws Exception {
        return sendSmsMessage(type, reservationInfo);
    }

    private String makeSmsToJson(String type, ReservationInfo info) throws JsonProcessingException {
        JSONObject jobj = new JSONObject();
        JSONArray jarr = new JSONArray();

        try{
            jobj.put("type", type);
            jobj.put("from", info.getFromAddr());
            String content = "[댄스팟예약]\n"+info.getName()+"님, 입금이 확인되면 예약됩니다.\n- 입금계좌 : 신한은행 110 504 015171(우해숙)\n"
                    +info.getPriceInfo()+"\n"+info.getDate();
            jobj.put("content", content);
            JSONObject obj = new JSONObject();
            obj.put("to", info.getToAddr());
            jarr.put(obj);
            jobj.put("messages",jarr);
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jobj.toString();
    }

    private String makeSmsToJsonX(String type, ReservationInfo info) throws JsonProcessingException {
        JSONObject jobj = new JSONObject();
        JSONArray jarr = new JSONArray();

        try{
            jobj.put("type", type);
            jobj.put("from", info.getFromAddr());
            String content = "[댄스팟 강남점 X홀 예약 완료]\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "<입장 안내>\n" +
                    "출입문 비밀번호는\n" +
                    "5602* 입니다 \n" +
                    "\n" +
                    "X홀은 출입문 들어가시자마자 왼편으로 가시면 있습니다!\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "<이용시 주의사항>\n" +
                    "1. 거울, 바닥 및 시설이 파손되지 않게 이용해 주세요!\n" +
                    "2. 반드시 부드러운 연습화만 사용해 주세요! (굽,스파이크 등 바닥을 손상시키는 신발 사용 불가)\n" +
                    "3. 연습 종료 후 에어컨, 앰프 및 조명은 꼭 꺼주세요!\n" +
                    "4. 연습실 내 음식물 취식은 금지되어있습니다.\n" +
                    "(물이나 간단한 음료만 가능)\n" +
                    "5. 와이파이 (iptime)\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "<환불, 시간 변경 규정>\n" +
                    "예약일 3일전 : 전액 환불, 변경 가능\n" +
                    "예약일 2일전 : 50% 환불, 50% 변경 가능\n" +
                    "1일전~당일 : 환불 불가, 변경불가\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "댄스팟강남점 주소 : 강남구 역삼동 792-1 성일빌딩 지하1층";
            jobj.put("content", content);
            JSONObject obj = new JSONObject();
            obj.put("to", info.getToAddr());
            jarr.put(obj);
            jobj.put("messages",jarr);
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jobj.toString();
    }

    private String makeSmsToJsonY(String type, ReservationInfo info) throws JsonProcessingException {
        JSONObject jobj = new JSONObject();
        JSONArray jarr = new JSONArray();

        try{
            jobj.put("type", type);
            jobj.put("from", info.getFromAddr());
            String content = "[댄스팟 강남점 Y홀 예약 안내]\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "<입장 안내>\n" +
                    "출입문 비밀번호는\n" +
                    "5602* 입니다\n" +
                    "\n" +
                    "Y홀은 출입문 들어가셔서 오른쪽으로 가시면 있습니다!\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "<이용시 주의사항>\n" +
                    "1. 거울, 바닥 및 시설이 파손되지 않게 이용해 주세요!\n" +
                    "2. 반드시 부드러운 연습화만 사용해 주세요! (굽,스파이크 등 바닥을 손상시키는 신발 사용 불가)\n" +
                    "3. 연습 종료 후 에어컨, 앰프 및 조명은 꼭 꺼주세요!\n" +
                    "4. 연습실 내 음식물 취식은 금지되어있습니다.\n" +
                    "(물이나 간단한 음료만 가능)\n" +
                    "5. 와이파이 (iptime)\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "<환불, 시간 변경 규정>\n" +
                    "예약일 3일전 : 전액 환불, 변경 가능\n" +
                    "예약일 2일전 : 50% 환불, 50% 변경 가능\n" +
                    "1일전~당일 : 환불 불가, 변경불가\n" +
                    "ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ\n" +
                    "댄스팟강남점 주소 : 강남구 역삼동 792-1 성일빌딩 지하1층";
            jobj.put("content", content);
            JSONObject obj = new JSONObject();
            obj.put("to", info.getToAddr());
            jarr.put(obj);
            jobj.put("messages",jarr);
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jobj.toString();
    }

    public static String makeSignature(String url, String timestamp, String method, String accessKey, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String space = " ";                    // one space
        String newLine = "\n";                 // new line
        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();
        SecretKeySpec signingKey;
        String encodeBase64String;
        try {
            signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
            encodeBase64String = Base64.encodeToString(rawHmac, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            encodeBase64String = e.toString();
        }
        return encodeBase64String;
    }

    public String sendSmsMessage(String type, ReservationInfo info){
        String result = null;
        try{
            Log.d(TAG, "Send SMS start.");
            String hostNameUrl = "https://sens.apigw.ntruss.com";     		// 호스트 URL
            String requestUrl= "/sms/v2/services/";                   		// 요청 URL
            String requestUrlType = "/messages";                      		// 요청 URL
            String accessKey = "HUpgJnTBDXKBgppvWGz9";                     		// 네이버 클라우드 플랫폼 회원에게 발급되는 개인 인증키
            String secretKey = "X9QfCM2SaScKWgkcrjJ3OLEQDJ0habF4YYGSTSkq";		// 2차 인증을 위해 서비스마다 할당되는 service secret
            String serviceId = "ncp:sms:kr:261033467226:danspotreservation";	// 프로젝트에 할당된 SMS 서비스 ID
            String method = "POST";											// 요청 method
            String timestamp = Long.toString(System.currentTimeMillis()); 	// current timestamp (epoch)
            requestUrl += serviceId + requestUrlType;
            String apiUrl = hostNameUrl + requestUrl;
            String body = "";
            if(mMode == 1)
                body = makeSmsToJson(type, info);
            else if(mMode == 2)
                body = makeSmsToJsonX(type, info);
            else if(mMode == 3)
                body = makeSmsToJsonY(type, info);
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("content-type", "application/json");
            conn.setRequestProperty("x-ncp-apigw-timestamp", timestamp);
            conn.setRequestProperty("x-ncp-iam-access-key", accessKey);
            conn.setRequestProperty("x-ncp-apigw-signature-v2", makeSignature(requestUrl, timestamp, method, accessKey, secretKey));
            conn.setRequestMethod(method);
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

            wr.write(body.getBytes());
            wr.flush();
            wr.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br;
            Log.d(TAG, String.valueOf(responseCode));
            if(responseCode==202) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            String resp = response.toString();
            JSONObject jsonObject = new JSONObject(resp);
            String statusCode = jsonObject.getString("statusCode");
            String statusName = jsonObject.getString("statusName");

            result = statusCode + "(" + statusName + ")" + "\n" + info.getHall();
            Log.d(TAG, result);
            Log.d(TAG, "Send SMS end.");
        } catch (Exception e){
            Log.e("REST API", "send SMS failed : " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
