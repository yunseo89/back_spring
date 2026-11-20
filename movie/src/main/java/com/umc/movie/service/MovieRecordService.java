package com.umc.movie.service;

import com.umc.movie.dto.*;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieRecordService {
    private final MemberRepository memberRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;

    // 두 INSERT를 묶는 경계입니다. RuntimeException이 밖으로 나가면 먼저 저장한 평점도 롤백됩니다.
    @Transactional
    public MovieRecordResponse create(Long memberId, CreateMovieRecordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER404_1", "해당 회원을 찾을 수 없습니다."));
        if (ratingRepository.existsByMember_MemberIdAndMovieId(memberId, request.movieId())) {
            throw new ApiException(HttpStatus.CONFLICT, "RATING409_1", "이미 이 영화에 평점을 남기셨습니다.");
        }
        if (request.addToFavorites()
                && favoriteRepository.existsByMember_MemberIdAndMovieId(memberId, request.movieId())) {
            throw new ApiException(HttpStatus.CONFLICT, "FAVORITE409_1", "이미 이 영화를 즐겨찾기하셨습니다.");
        }
        Rating rating = ratingRepository.saveAndFlush(Rating.builder()
                .member(member).movieId(request.movieId()).score(request.score()).comment(request.comment()).build());
        FavoriteResponse favoriteResponse = null;
        if (request.addToFavorites()) {
            Favorite favorite = favoriteRepository.saveAndFlush(Favorite.builder()
                    .member(member).movieId(request.movieId()).build());
            favoriteResponse = FavoriteResponse.from(favorite);
        }
        return new MovieRecordResponse(RatingResponse.from(rating), favoriteResponse);
    }
}
