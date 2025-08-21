package com.creator.pangpanglive.util

import android.content.*
import android.util.Log
import com.creator.pangpanglive.models.Image
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by skcrackers on 26/12/2017.
 */
object UploadUtil {
    private const val LINE_END = "\r\n"
    private const val TWOHYPEN = "--"
    private const val boundary = "----WebKitFormBoundaryAndroidForWavayo"
    @Throws(Exception::class)
    fun upload(
        context: Context?,
        uploadUrl: String,
        images: ArrayList<Image>,
        paramMap: HashMap<String, String?>
    ): String {
        var result = ""

        // 데이터 경계선
        val delimiter = LINE_END + TWOHYPEN + boundary + LINE_END;
        val postDataBuilder = StringBuffer()
        postDataBuilder.append(TWOHYPEN + boundary + LINE_END)

        // 추가하고 싶은 Key & Value 추가
        // key & value를 추가한 후 꼭 경계선을 삽입해줘야 데이터를 구분할 수 있다.
        val keys: Iterator<String> = paramMap.keys.iterator()
        while (keys.hasNext()) {
            val key = keys.next()
            postDataBuilder.append(setValue(key, paramMap[key]))
            postDataBuilder.append(delimiter)
        }

        // 커넥션 생성 및 설정
        val conn = URL(uploadUrl).openConnection() as HttpURLConnection
        conn.doInput = true
        conn.doOutput = true
        conn.useCaches = false
        conn.requestMethod = "POST"
        conn.setRequestProperty("Connection", "Keep-Alive")
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary)
        var filInputStream: FileInputStream? = null
        var dataOutputStream: DataOutputStream? = null

        // 전송 작업 시작
        dataOutputStream = DataOutputStream(BufferedOutputStream(conn.outputStream))
        // 위에서 작성한 메타데이터를 먼저 전송한다. (한글이 포함되어 있으므로 UTF-8 메소드 사용)
        dataOutputStream.writeBytes(postDataBuilder.toString())
        for (idx in images.indices) {
            Log.d("SeongKwon", "uploadUtil Name = " + images[idx].name)
            dataOutputStream.writeBytes(setFile("imgFile", images[idx].name))
            dataOutputStream.writeBytes(LINE_END)

            // 전송 작업 시작
            Log.d("SeongKwon", "uploadUtil Path = " + images[idx].path)
            filInputStream = FileInputStream(images[idx].path)

            // 파일 복사 작업 시작
            val maxBufferSize = 4096
            var bufferSize = Math.min(filInputStream.available(), maxBufferSize)
            val buffer = ByteArray(bufferSize)

            // 버퍼 크기만큼 파일로부터 바이트 데이터를 읽는다.
            var byteRead = filInputStream.read(buffer, 0, bufferSize)

            // 전송
            while (byteRead > 0) {
                dataOutputStream.write(buffer)
                bufferSize = Math.min(filInputStream.available(), maxBufferSize)
                byteRead = filInputStream.read(buffer, 0, bufferSize)
            }

            // content wrapper종료
            if(idx == images.size - 1) {
                dataOutputStream.writeBytes(LINE_END + TWOHYPEN + boundary + "--" + LINE_END); // 반드시 작성해야 한다.
            } else {
                dataOutputStream.writeBytes(LINE_END + TWOHYPEN + boundary + LINE_END); // 반드시 작성해야 한다.
            }
        }
        dataOutputStream.flush()
        dataOutputStream.close()
        filInputStream?.close()

        // 결과 반환 (HTTP RES CODE)
        if (conn.responseCode == 200) {
            result = conn.responseMessage
            Log.e("SeongKwon", "result1 = $result")
        } else {
            Log.e("SeongKwon", "result2 = " + conn.responseCode + "//" + result)
        }

        // 무조건 서버응답 bypass
        val baos = ByteArrayOutputStream()
        val byteBuffer = ByteArray(2048)
        var byteData: ByteArray? = null
        var nLength = 0
        while (conn.inputStream.read(byteBuffer, 0, byteBuffer.size).also { nLength = it } != -1) {
            baos.write(byteBuffer, 0, nLength)
        }
        byteData = baos.toByteArray()
        result = String(byteData)
        conn.disconnect()
        return result
    }

    /**
     * Map 형식으로 Key와 Value를 셋팅한다.
     *
     * @param key   : 서버에서 사용할 변수명
     * @param value : 변수명에 해당하는 실제 값
     * @return
     */
    fun setValue(key: String, value: String?): String {
        return "Content-Disposition: form-data; name=\"$key\"\r\n\r\n$value"
    }

    /**
     * 업로드할 파일에 대한 메타 데이터를 설정한다.
     *
     * @param key      : 서버에서 사용할 파일 변수명
     * @param fileName : 서버에서 저장될 파일명
     * @return
     */
    fun setFile(key: String, fileName: String?): String {
        return "Content-Disposition: form-data; name=\"$key\"; filename=\"$fileName\"\r\nContent-Type: image/jpeg\r\n"
    }

    private fun readInputStreamToString(connection: HttpURLConnection): String? {
        var result: String? = null
        val sb = StringBuffer()
        var `is`: InputStream? = null
        try {
            `is` = BufferedInputStream(connection.inputStream)
            val br = BufferedReader(InputStreamReader(`is`))
            var inputLine: String? = ""
            while (br.readLine().also { inputLine = it } != null) {
                sb.append(inputLine)
            }
            result = sb.toString()
        } catch (e: Exception) {
            Log.i("SeongKwon", "Error reading InputStream")
            result = null
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    Log.d("SeongKwon", "Error closing InputStream")
                }
            }
        }
        return result
    }
}