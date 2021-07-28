package com.creator.allshoppingliveapp.util;

import android.content.Context;
import android.util.Log;

import com.creator.allshoppingliveapp.models.Image;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by skcrackers on 26/12/2017.
 */

public class UploadUtil {
    private static final String LINE_END = "\r\n";
    private static final String TWOHYPEN = "--";
    private static final String boundary = "----WebKitFormBoundaryAndroidForWavayo";

    public static String upload(Context context, String uploadUrl, ArrayList<Image> images, HashMap<String, String> paramMap) throws Exception {
        String result = "";
        String url = uploadUrl;

        // 데이터 경계선
        String delimiter = "\r\n" + TWOHYPEN + boundary + "\r\n";

        StringBuffer postDataBuilder = new StringBuffer();

        postDataBuilder.append(TWOHYPEN + boundary + LINE_END);

        // 추가하고 싶은 Key & Value 추가
        // key & value를 추가한 후 꼭 경계선을 삽입해줘야 데이터를 구분할 수 있다.
        Iterator<String> keys = paramMap.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            postDataBuilder.append(setValue(key, paramMap.get(key)));
            postDataBuilder.append(delimiter);
        }

        // 커넥션 생성 및 설정
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        FileInputStream in = null;
        DataOutputStream out = null;

        // 전송 작업 시작

        out = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
        // 위에서 작성한 메타데이터를 먼저 전송한다. (한글이 포함되어 있으므로 UTF-8 메소드 사용)
        out.writeBytes(postDataBuilder.toString());

        for(int idx = 0; idx < images.size(); idx++) {
            Log.d("SeongKwon", "uploadUtil Name = " + images.get(idx).name);
            out.writeBytes(setFile("imgFile", images.get(idx).name));
            out.writeBytes("\r\n");

            // 전송 작업 시작
            Log.d("SeongKwon", "uploadUtil Path = " + images.get(idx).path);
            in = new FileInputStream(images.get(idx).path);

            // 파일 복사 작업 시작
            int maxBufferSize = 4096;
            int bufferSize = Math.min(in.available(), maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            // 버퍼 크기만큼 파일로부터 바이트 데이터를 읽는다.
            int byteRead = in.read(buffer, 0, bufferSize);

            // 전송
            while (byteRead > 0) {
                out.write(buffer);
                bufferSize = Math.min(in.available(), maxBufferSize);
                byteRead = in.read(buffer, 0, bufferSize);
            }

            // content wrapper종료
            if(idx == images.size() - 1) {
                out.writeBytes("\r\n" + TWOHYPEN + boundary + "--\r\n"); // 반드시 작성해야 한다.
            } else {
                out.writeBytes("\r\n" + TWOHYPEN + boundary + "\r\n"); // 반드시 작성해야 한다.
            }
        }

        out.flush();
        out.close();
        in.close();

        // 결과 반환 (HTTP RES CODE)
        if(conn.getResponseCode() == 200) {
            result = conn.getResponseMessage();
            Log.e("SeongKwon", "result1 = " + result);
        } else {
            Log.e("SeongKwon", "result2 = " + conn.getResponseCode() + "//" + result);
        }

        // 무조건 서버응답 bypass
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] byteBuffer = new byte[2048];
        byte[] byteData = null;
        int nLength = 0;
        while((nLength = conn.getInputStream().read(byteBuffer, 0, byteBuffer.length)) != -1) {
            baos.write(byteBuffer, 0, nLength);
        }
        byteData = baos.toByteArray();
        result = new String(byteData);

        conn.disconnect();

        return result;
    }

    /**
     * Map 형식으로 Key와 Value를 셋팅한다.
     *
     * @param key   : 서버에서 사용할 변수명
     * @param value : 변수명에 해당하는 실제 값
     * @return
     */
    public static String setValue(String key, String value) {
        return "Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n" + value;
    }

    /**
     * 업로드할 파일에 대한 메타 데이터를 설정한다.
     *
     * @param key      : 서버에서 사용할 파일 변수명
     * @param fileName : 서버에서 저장될 파일명
     * @return
     */
    public static String setFile(String key, String fileName) {
        return "Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"\r\nContent-Type: image/jpeg" + "\r\n";
    }

    private static String readInputStreamToString(HttpURLConnection connection) {
        String result = null;
        StringBuffer sb = new StringBuffer();
        InputStream is = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        }
        catch (Exception e) {
            Log.i("SeongKwon", "Error reading InputStream");
            result = null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    Log.d("SeongKwon", "Error closing InputStream");
                }
            }
        }

        return result;
    }
}
