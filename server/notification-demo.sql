-- 앱 서버와 같은 DB를 선택한 뒤, 아래 0을 테스트 계정의 users_id로 바꿉니다.
-- 서버를 한 번 시작하여 app_notifications 테이블을 만든 후 실행하세요.
-- 0인 상태에서는 데이터가 추가되지 않습니다.
SET @notification_demo_user = 0;

INSERT INTO app_notifications (users_id, category, title, body, event_key)
SELECT users_id, 'POLICY', '[테스트] 추천 정책', '추천 정책 알림 표시를 확인해주세요.',
       CONCAT('demo:', users_id, ':policy')
FROM users WHERE users_id = @notification_demo_user
ON DUPLICATE KEY UPDATE deleted = FALSE, is_read = FALSE, created_at = CURRENT_TIMESTAMP(3);

INSERT INTO app_notifications (users_id, category, title, body, event_key)
SELECT users_id, 'DEADLINE', '[테스트] 마감 안내', '마감 안내 분류를 확인해주세요.',
       CONCAT('demo:', users_id, ':deadline')
FROM users WHERE users_id = @notification_demo_user
ON DUPLICATE KEY UPDATE deleted = FALSE, is_read = FALSE, created_at = CURRENT_TIMESTAMP(3);

INSERT INTO app_notifications (users_id, category, title, body, event_key)
SELECT users_id, 'COMMUNITY', '[테스트] 커뮤니티', '읽음 처리와 X 버튼 삭제를 확인해주세요.',
       CONCAT('demo:', users_id, ':community')
FROM users WHERE users_id = @notification_demo_user
ON DUPLICATE KEY UPDATE deleted = FALSE, is_read = FALSE, created_at = CURRENT_TIMESTAMP(3);

-- 테스트가 끝난 후 이 세 알림만 완전히 정리하려면 아래 줄의 -- 를 제거하고 실행합니다.
-- DELETE FROM app_notifications WHERE users_id = @notification_demo_user AND event_key LIKE CONCAT('demo:', @notification_demo_user, ':%');
