package com.example.AIrobot.model;

public class AdminSession {

    public enum Step {
        ASK_EMAIL,
        ASK_NAME,
        CONFIRM_DONE
    }

    private Step step;
    private String email;
    private String name;

    public AdminSession() {
        this.step = Step.ASK_EMAIL;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
