package com.example.cheongon.jobtool;

public class InterviewQuestionRequest {
    public final String jobRole;
    public final String careerLevel;
    public final String requiredCompetencies;
    public final String coverLetter;
    public final String companyIndustry;

    public InterviewQuestionRequest(String jobRole, String careerLevel,
                                    String requiredCompetencies, String coverLetter,
                                    String companyIndustry) {
        this.jobRole = jobRole;
        this.careerLevel = careerLevel;
        this.requiredCompetencies = requiredCompetencies;
        this.coverLetter = coverLetter;
        this.companyIndustry = companyIndustry;
    }
}
