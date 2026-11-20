package com.umc.movie.controller;

import com.umc.movie.dto.*;
import com.umc.movie.interceptor.AuthInterceptor;
import com.umc.movie.service.FavoriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> create(
            @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
            @Valid @RequestBody CreateFavoriteRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("즐겨찾기를 등록했습니다.", favoriteService.create(memberId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getMyFavorites(
            @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok("내 즐겨찾기 목록을 조회했습니다.", favoriteService.getMyFavorites(memberId)));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<FavoriteStatusResponse>> getStatus(
            @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
            @Positive(message = "영화 ID는 양수여야 합니다.") @PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.ok("즐겨찾기 여부를 조회했습니다.", favoriteService.getStatus(memberId, movieId)));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
            @Positive(message = "영화 ID는 양수여야 합니다.") @PathVariable Long movieId) {
        favoriteService.delete(memberId, movieId);
        return ResponseEntity.ok(ApiResponse.ok("즐겨찾기를 해제했습니다.", null));
    }
}
