-- 기존 member/rating 데이터는 변경하지 않습니다. MySQL Workbench에서 먼저 선택한 DB를 확인하세요.
USE umc_movie;

-- 5주차에 favorite를 만들었다면 이 문장은 기존 테이블을 변경하지 않습니다.
CREATE TABLE IF NOT EXISTS favorite (
    favorite_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL COMMENT 'TMDB movie ID, 외래 키 아님',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (favorite_id),
    CONSTRAINT uk_favorite_member_movie UNIQUE (member_id, movie_id),
    CONSTRAINT fk_favorite_member FOREIGN KEY (member_id) REFERENCES member(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 테이블에서도 시간 기본값, 외래 키, (member_id, movie_id) UNIQUE를 꼭 확인합니다.
SHOW CREATE TABLE favorite;
SHOW INDEX FROM favorite;
SHOW CREATE TABLE rating;

-- 아래는 검사 결과 복합 UNIQUE가 정말 없을 때만 수동으로 실행합니다.
-- 먼저 중복 데이터가 있는지 확인하고, 있다면 보존할 데이터를 결정하세요. 자동 삭제하지 않습니다.
-- SELECT member_id, movie_id, COUNT(*) FROM favorite GROUP BY member_id, movie_id HAVING COUNT(*) > 1;
-- ALTER TABLE favorite ADD CONSTRAINT uk_favorite_member_movie UNIQUE (member_id, movie_id);
-- 동일한 복합 UNIQUE가 다른 이름이면 새로 추가하는 대신 이름만 맞출 수 있습니다.
-- ALTER TABLE favorite RENAME INDEX 기존_인덱스_이름 TO uk_favorite_member_movie;
-- rating도 uk_rating_member_movie 이름의 (member_id, movie_id) UNIQUE가 필요합니다.
-- member.member_id가 UNSIGNED라면 새 favorite.member_id도 같은 자료형으로 맞춘 뒤 생성하세요.
