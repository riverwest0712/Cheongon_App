package com.example.cheongon.jobtool;

import java.util.List;

public class InterviewQuestionResponse {
    public List<Question> questions;

    public static class Question {
        public String category;
        public String question;
    }
}
