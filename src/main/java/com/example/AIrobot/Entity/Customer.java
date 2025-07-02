package com.example.AIrobot.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String region;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String status;

    private String createdBy;
    private Integer age;              
    private String job;               
    private String productsOwned;     

    // 新增欄位
    @Column(unique = true)
    private String idNumber;        // 身分證字號

    private LocalDate birthday;     // 出生年月日

    // AI 分析欄位
    private String potentialLevel;   // 成交機會/分數

    @Lob
    private String aiComment;        // 評價（AI說明）

    @Lob
    private String aiProductAdvice;  // 建議產品（AI回覆）

    @Lob
    private String aiFollowUp;       // 後續建議（AI回覆）

    // AI 建議標籤（逗號分隔文字，不產生新表）
    @Column(name = "ai_tags", columnDefinition = "TEXT")
    private String aiTags; // 例如： "年輕族群, 醫療需求, 高意願"

    // 備註
    @ElementCollection
    @CollectionTable(name = "customer_notes", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "note")
    private List<String> notes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Constructors ---
    public Customer() {}

    public Customer(
        String name,
        String phone,
        String region,
        String status,
        List<String> notes,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer age,
        String job,
        String productsOwned,
        String idNumber,            // 新增
        LocalDate birthday,         // 新增
        String potentialLevel,
        String aiComment,
        String aiProductAdvice,
        String aiFollowUp,
        String aiTags
    ) {
        this.name = name;
        this.phone = phone;
        this.region = region;
        this.status = status;
        this.notes = notes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.age = age;
        this.job = job;
        this.productsOwned = productsOwned;
        this.idNumber = idNumber;      // 新增
        this.birthday = birthday;      // 新增
        this.potentialLevel = potentialLevel;
        this.aiComment = aiComment;
        this.aiProductAdvice = aiProductAdvice;
        this.aiFollowUp = aiFollowUp;
        this.aiTags = aiTags;
    }

    public Customer(String name) {
        this.name = name;
    }

    // --- Getter & Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
    public void addNote(String note) { notes.add(note); }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public String getProductsOwned() { return productsOwned; }
    public void setProductsOwned(String productsOwned) { this.productsOwned = productsOwned; }

    // 新增 Getter/Setter
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public String getPotentialLevel() { return potentialLevel; }
    public void setPotentialLevel(String potentialLevel) { this.potentialLevel = potentialLevel; }

    public String getAiComment() { return aiComment; }
    public void setAiComment(String aiComment) { this.aiComment = aiComment; }

    public String getAiProductAdvice() { return aiProductAdvice; }
    public void setAiProductAdvice(String aiProductAdvice) { this.aiProductAdvice = aiProductAdvice; }

    public String getAiFollowUp() { return aiFollowUp; }
    public void setAiFollowUp(String aiFollowUp) { this.aiFollowUp = aiFollowUp; }

    public String getAiTags() { return aiTags; }
    public void setAiTags(String aiTags) { this.aiTags = aiTags; }
}
