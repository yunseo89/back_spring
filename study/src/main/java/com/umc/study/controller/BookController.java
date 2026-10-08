package com.umc.study.controller;

import com.umc.study.dto.BookResponse;
import com.umc.study.dto.CreateBookRequest;
import com.umc.study.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // 도서 등록 API (POST /books) -> 201 Created 반환
    @PostMapping
    public ResponseEntity<Long> createBook(@RequestBody @Valid CreateBookRequest request) {
        Long bookId = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookId);
    }

    // 도서 목록 조회 API (GET /books) -> 최신순 반환
    @GetMapping
    public ResponseEntity<List<BookResponse>> getBooks() {
        List<BookResponse> books = bookService.getBooks();
        return ResponseEntity.ok(books);
    }
}