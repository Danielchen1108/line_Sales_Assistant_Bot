package com.example.AIrobot.Util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LineMessageUtil {

    // Spring 會自動從 application.properties 或環境變數注入
    @Value("${line.channel.access.token}")
    private String channelAccessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendLineReply(String replyToken, String replyText) {
        try {
            JSONObject message = new JSONObject()
                    .put("type", "text")
                    .put("text", replyText);

            JSONObject body = new JSONObject()
                    .put("replyToken", replyToken)
                    .put("messages", new JSONArray().put(message));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(channelAccessToken);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            restTemplate.postForEntity(
                    "https://api.line.me/v2/bot/message/reply",
                    entity,
                    String.class
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ResponseEntity<String> replyText(String replyToken, String replyText) {
    try {
        JSONObject message = new JSONObject()
                .put("type", "text")
                .put("text", replyText);

        JSONObject body = new JSONObject()
                .put("replyToken", replyToken)
                .put("messages", new JSONArray().put(message));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(channelAccessToken);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        // 將回傳結果交由 handler 繼續傳出去
        return restTemplate.postForEntity(
                "https://api.line.me/v2/bot/message/reply",
                entity,
                String.class
        );
    } catch (Exception ex) {
        ex.printStackTrace();
        // 若錯誤發生，回傳 500 錯誤
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("LINE 傳送失敗：" + ex.getMessage());
    }
}

}
