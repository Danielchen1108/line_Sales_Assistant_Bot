package com.example.AIrobot.Service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.AIrobot.Entity.Customer;

import java.util.Collections;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // 1. 客戶成交潛力分析
    public String analyzeCustomerPotential(Customer customer) {
        String prompt = String.format("""
        請根據以下條件分數標準進行評分：
        1~4分：財務壓力大，無明顯需求，近期生活重心不在保險理財
        5~7分：生活安定，有潛在需求或亮點、近期無明顯重大事件，但家庭/個人有未來可能規劃
        8~10分：近期有明顯大事件（如結婚、生子、買房、升遷等）或已表現出強烈購買/規劃意願

        分數須根據顧客資料真實狀況評估，勿僅依範例分數判斷，請保持分數分布合理。

        你是一位保險業的客戶成交潛力分析專員，請根據以下顧客資料，以「財務經濟壓力」為最主要評估依據，結合顧客的個人親身經歷與潛在需求，綜合「年齡」、「職業」、「已購買產品」、「地區」、「狀態」等資訊，判斷顧客未來是否有機會產生保險或理財需求，並評估「成交機會」1~10分。

        若顧客短期或中長期內需求不明顯、成交機會較小，請給予較低分數（1~4分），並在評價中直接說明原因，例如財務壓力較大、現階段生活重心非在保險或理財規劃上等。若顧客雖無明顯需求但具有潛在變化，請在「後續建議」欄位具體說明可追蹤、可培養的行動，並在「標籤」欄位標註「可追蹤」或「可培養」。

        若顧客具有未來潛力，請給予中高分（5~10分），並具體指出可談方向（如健康險、意外險、退休規劃、家庭保障、理財商品等），同時推薦最適合該顧客的保險產品類型或具體商品名稱（如定期壽險、實支實付醫療險、還本型年金、癌症險等），並於「後續建議」欄位簡述應主動積極推進或培養策略，於「標籤」欄位標註「高潛力」「主動推進」或「立即跟進」等。

        分數評定須以顧客的財務經濟壓力為主，輔以個人經歷（如近期重大事件、生活型態變化、家人需求等）與其潛在需求，並真實反映實際成交機會，避免過度樂觀或悲觀。

        請於「標籤」欄位僅填寫下列關鍵字之一或多個：「高潛力」「可追蹤」「可培養」「需關懷」「主動推進」「立即跟進」

        請只回傳下列格式的 JSON，不要加任何說明、標點或文字：
        {
            "成交機會": "6分",
            "評價": "大胖年齡31歲，已購買多種保險產品，目前工作穩定，生活重心以家庭為主。近期無重大生活事件，經濟壓力適中，對未來保障與子女教育有一定需求。",
            "建議產品": "還本型年金、子女教育險、重大疾病險",
            "後續建議": "可列為中長期追蹤名單，建議定期關心其生活狀況，若有結婚、生子、工作升遷等變化時主動跟進。",
            "標籤": "可追蹤"
        }

        顧客資料如下：
        姓名：%s
        年齡：%s
        職業：%s
        已購買產品：%s
        地區：%s
        狀態：%s

        """,
                customer.getName(),
                customer.getAge() != null ? customer.getAge() : "未提供",
                customer.getJob() != null ? customer.getJob() : "未提供",
                customer.getProductsOwned() != null ? customer.getProductsOwned() : "未提供",
                customer.getRegion(),
                customer.getStatus()
        );

        JSONObject requestBody = new JSONObject()
            .put("model", "gpt-4o")
            .put("messages", Collections.singletonList(
                new JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
            ))
            .put("temperature", 0.5);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            request,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            String responseBody = response.getBody();
            JSONObject json = new JSONObject(responseBody);
            String content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

            // Log content
            System.out.println("OpenAI content: " + content);

            // 正則抓出最外層 JSON
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{[\\s\\S]*?\\}");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String onlyJson = matcher.group();
                System.out.println("純 JSON: " + onlyJson);
                return onlyJson;
            } else {
                // 找不到就直接回傳原始
                return content;
            }
        } else {
            throw new RuntimeException("OpenAI 回傳錯誤：" + response.getStatusCode());
        }
    }

    // 2. 顧問建議功能
    public String advisorSuggest(String question, Customer customer) {
        String prompt = String.format("""
        你是一位有豐富經驗的保險顧問，熟悉保單設計與客戶關係經營。  
        請依據下列客戶資料，並針對業務員隨時提出的任何問題，綜合判斷、分析，並用**真誠、專業又親切的語氣**給出具體建議。  
        建議內容可以包含：
        - 購買意願評估
        - 銷售時的重點、提醒
        - 開場話題、溝通技巧
        - 產品需求切入點
        - 後續追蹤方式
        - …只要對業務員有幫助都可自由發揮

        請**每次都回應真實且務實的專業建議，不要使用模板或重複內容**，回答 200 字內。

        客戶資料如下：
        姓名：%s
        年齡：%s
        職業：%s
        已購產品：%s
        地區：%s
        狀態：%s
        成交機會：%s
        評價：%s
        後續建議：%s
        標籤：%s

        業務員問題：%s

            """,
                customer.getName(),
                customer.getAge() != null ? customer.getAge() : "未提供",
                customer.getJob() != null ? customer.getJob() : "未提供",
                customer.getProductsOwned() != null ? customer.getProductsOwned() : "未提供",
                customer.getRegion(),
                customer.getStatus(),
                customer.getPotentialLevel(),
                customer.getAiComment(),
                customer.getAiProductAdvice(),
                customer.getAiFollowUp(),
                question
        );

        JSONObject requestBody = new JSONObject()
            .put("model", "gpt-4o")
            .put("messages", Collections.singletonList(
                new JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
            ))
            .put("temperature", 0.6);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
            OPENAI_API_URL,
            HttpMethod.POST,
            request,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            String responseBody = response.getBody();
            JSONObject json = new JSONObject(responseBody);
            String content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

            // Log advisor content
            System.out.println("OpenAI 顧問服務內容: " + content);

            return content.trim();
        } else {
            throw new RuntimeException("OpenAI 回傳錯誤：" + response.getStatusCode());
        }
    }
}
