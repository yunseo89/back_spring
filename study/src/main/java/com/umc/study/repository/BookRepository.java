package com.umc.study.repository;

import com.umc.study.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // 특정 카테고리 ID로 도서 목록을 조회하는 쿼리 메서드
    List<Book> findByCategoryId(Long categoryId);
}
