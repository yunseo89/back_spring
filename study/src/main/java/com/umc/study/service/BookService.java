package com.umc.study.service;

import com.umc.study.domain.Book;
import com.umc.study.domain.Category;
import com.umc.study.dto.BookResponse;
import com.umc.study.dto.CreateBookRequest;
import com.umc.study.exception.CategoryNotFoundException;
import com.umc.study.repository.BookRepository;
import com.umc.study.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    // 도서 등록 (POST)
    @Transactional
    public Long createBook(CreateBookRequest request) {
        // 1. 카테고리 존재 여부 확인 (없으면 커스텀 예외 발생)
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("존재하지 않는 카테고리 ID입니다: " + request.categoryId()));

        // 2. Book 엔티티 생성 및 저장
        Book book = new Book(category, request.title(), request.description());
        Book savedBook = bookRepository.save(book);

        return savedBook.getBookId();
    }

    // 도서 목록 조회 - 최신순 (GET)
    public List<BookResponse> getBooks() {
        // bookId 기준 내림차순(최신순) 정렬 조회
        List<Book> books = bookRepository.findAll(Sort.by(Sort.Direction.DESC, "bookId"));

        return books.stream()
                .map(BookResponse::from)
                .collect(Collectors.toList());
    }
}
