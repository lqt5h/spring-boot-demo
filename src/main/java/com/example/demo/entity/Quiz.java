package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private boolean isLocked;

    private boolean allowMultipleAttempts;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Quiz -> Question сериализуем
    private List<Question> questions = new ArrayList<>();

    public Quiz() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isIsLocked() {
        return isLocked;
    }

    public void setIsLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isAllowMultipleAttempts() {
        return allowMultipleAttempts;
    }

    public void setAllowMultipleAttempts(boolean allowMultipleAttempts) {
        this.allowMultipleAttempts = allowMultipleAttempts;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    // ВАЖНО: этот метод нужен, чтобы компилировался QuizManagementService
    public void setQuestions(List<Question> questions) {
        this.questions = (questions == null) ? new ArrayList<>() : questions;
    }
}
