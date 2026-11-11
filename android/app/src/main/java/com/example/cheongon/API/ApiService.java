package com.example.cheongon.API;

import com.example.cheongon.API.ApiService;
import com.example.cheongon.community.Post;
import com.example.cheongon.community.Comment;
import com.example.cheongon.recommendation.Bookmark;
import com.example.cheongon.recommendation.Recommendation;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/interview/questions")
    Call<com.example.cheongon.jobtool.InterviewQuestionResponse> generateInterviewQuestions(
            @retrofit2.http.Header("Authorization") String authorization,
            @Body com.example.cheongon.jobtool.InterviewQuestionRequest request
    );

    @GET("api/jobtool/access")
    Call<Map<String, Object>> getJobtoolAccess(
            @retrofit2.http.Header("Authorization") String authorization
    );

    @POST("api/jobtool/payment-order")
    Call<Map<String, Object>> createJobtoolPaymentOrder(
            @retrofit2.http.Header("Authorization") String authorization
    );

    @GET("api/policy")
    Call<List<Map<String, Object>>> getPolicies();

    // 정책 추천 결과
    @GET("api/recommendations/{users_id}")
    Call<List<Recommendation>> getRecommendations(
            @Path("users_id") int usersId
    );

    // 회원가입
    @POST("signup")
    Call<Map<String, Object>> signup(
            @Body Map<String, Object> user
    );

    // 아이디 자동 중복 확인
    // DB의 기존 name 컬럼에서 중복 확인
    @POST("api/check-id")
    Call<Map<String, Object>> checkIdDuplicate(
            @Body Map<String, String> request
    );

    // 아이디 변경
    @PUT("api/user/change-id")
    Call<Map<String, Object>> changeUserId(
            @Body Map<String, Object> request
    );

    // 비밀번호 변경
    @PUT("api/user/change-password")
    Call<Map<String, Object>> changePassword(
            @Body Map<String, Object> request
    );

    // 이메일 자동 중복 확인
    @POST("api/check-email")
    Call<Map<String, Object>> checkEmailDuplicate(
            @Body Map<String, String> request
    );

    // 로그인
    @POST("login")
    Call<Map<String, Object>> login(
            @Body Map<String, String> user
    );


    // 회원탈퇴
    @POST("api/user/delete")
    Call<Map<String, Object>> deleteUser(
            @Body Map<String, Object> request
    );

    // 제공자 토큰은 서버에서 각 제공자 API로 검증한 뒤 기존 회원 로그인 또는 신규 가입 처리
    @POST("api/auth/social")
    Call<Map<String, Object>> socialLogin(
            @Body Map<String, String> credential
    );

    @GET("api/auth/signup-terms")
    Call<Map<String, String>> getSignupTerms();

    @POST("api/auth/social/complete")
    Call<Map<String, Object>> completeSocialSignup(@Body Map<String, Object> request);

    @POST("api/auth/mock")
    Call<Map<String, Object>> mockSocialLogin(
            @Body Map<String, String> request
    );

    // 조건입력
    @POST("api/user-conditions")
    Call<Map<String, Object>> saveUserConditions(
            @Body Map<String, Object> conditions
    );

    // 찜 추가
    @POST("api/bookmark")
    Call<Map<String, Object>> addBookmark(
            @Body Map<String, Object> request
    );
    // 찜 목록 조회
    @GET("api/bookmark/{users_id}")
    Call<List<Bookmark>> getBookmarks(
            @Path("users_id") int usersId
    );

    // 찜 삭제
    @DELETE("api/bookmark/{bookmark_id}")
    Call<Map<String, Object>> deleteBookmark(
            @Path("bookmark_id") int bookmarkId
    );

    // 정책 찜(추천) 토글 - 커뮤니티 좋아요 토글과 동일 방식
    @POST("api/bookmark/toggle")
    Call<Map<String, Object>> toggleBookmark(
            @Body Map<String, Object> body
    );

    // 정책 찜(추천) 개수 + 내 찜 여부 조회
    @GET("api/bookmark/status")
    Call<Map<String, Object>> getBookmarkStatus(
            @Query("policy_id") String policyId,
            @Query("users_id") int usersId
    );

    // 챗봇 메시지 전송
    @POST("api/chat")
    Call<Map<String, Object>> sendChatMessage(
            @retrofit2.http.Header("Authorization") String authorization,
            @Body Map<String, Object> request
    );


    // 전체 게시글 목록 조회 (로그인한 유저의 좋아요 상태를 함께 받기 위해 users_id 추가)
    @GET("api/posts")
    Call<List<Post>> getPosts(
            @Query("users_id") int usersId
    );

    // 게시글 작성
    @POST("api/posts")
    Call<Map<String, Object>> createPost(
            @Body Map<String, Object> postData
    );

    // 내가 쓴 게시글 조회
    @GET("api/posts/my/{users_id}")
    Call<List<Post>> getMyPosts(
            @Path("users_id") int usersId
    );

    // 좋아요 누른 게시물 조회
    @GET("api/posts/liked/{users_id}")
    Call<List<Post>> getLikedPosts(
            @Path("users_id") int usersId
    );

    // 댓글 목록 조회
    @GET("api/posts/{post_id}/comments")
    Call<List<Comment>> getComments(
            @Path("post_id") int postId
    );

    // 댓글 등록
    @POST("api/comments")
    Call<Map<String, Object>> createComment(
            @Body Map<String, Object> commentData
    );

    // 게시글 삭제
    @DELETE("/api/posts/{post_id}")
    Call<Map<String, Object>> deletePost(
            @Path("post_id") int postId);

    // 게시글 수정
    @PUT("/api/posts/{post_id}")
    Call<Map<String, Object>> updatePost(
            @Path("post_id") int postId,
            @Body Map<String, Object> updateData
    );

    // 게시글 단건 조회
    @GET("/api/posts/{post_id}")
    Call<Post> getPostDetail(@Path("post_id") int postId);

    // 내가 쓴 댓글 조회
    @GET("api/comments/user/{users_id}")
    Call<List<Comment>> getMyComments(
            @Path("users_id") int userId
    );

    // 댓글 수정
    @PUT("api/comments/{comment_id}")
    Call<Map<String, Object>> updateComment(
            @Path("comment_id") int commentId,
            @Body Map<String, Object> updateData
    );

    // 댓글 삭제
    @DELETE("api/comments/{comment_id}")
    Call<Map<String, Object>> deleteComment(
            @Path("comment_id") int commentId
    );

    // 좋아요 누르기 / 취소하기 (토글 방식)
    @POST("api/posts/like")
    Call<Map<String, Object>> toggleLike(
            @Body Map<String, Object> likeData
    );

    // (좋아요 상태 및 개수 조회)
    @GET("api/posts/like/status")
    Call<Map<String, Object>> getLikeStatus(
            @Query("post_id") int postId,
            @Query("users_id") int userId
    );

    //상세 정보
    @GET("api/policy/{policy_id}")
    Call<Map<String, Object>> getPolicyDetail(
            @Path("policy_id") String policyId,
            @Query("users_id") int usersId
    );

    //자소서
    @POST("/api/cover-letter/review")
    Call<Map<String, Object>> requestAiCoverLetterReview(
            @retrofit2.http.Header("Authorization") String authorization,
            @Body Map<String, String> body
    );
}
