package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {
    @Mock MemberRepository members;
    @Mock RatingRepository ratings;
    @InjectMocks RatingService service;

    @Test
    @DisplayName("같은 영화에 평점을 두 번 등록하면 409 RATING409_1이고 저장하지 않는다")
    void duplicateRatingIsRejected() {
        when(members.findById(1L)).thenReturn(Optional.of(mock(Member.class)));
        when(ratings.existsByMember_MemberIdAndMovieId(1L, 550L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class,
                () -> service.create(1L, new CreateRatingRequest(550L, 5, null)));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.getCode()).isEqualTo("RATING409_1");
        verify(ratings, never()).save(any());
    }

    @Test
    @DisplayName("남의 평점을 수정하면 403 RATING403_1이고 기존 별점과 코멘트가 유지된다")
    void anotherMembersRatingCannotBeEdited() {
        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(1L);
        Rating rating = Rating.builder().member(owner).movieId(550L).score(3).comment("원래 후기").build();
        when(ratings.findById(9L)).thenReturn(Optional.of(rating));

        ApiException error = assertThrows(ApiException.class,
                () -> service.update(2L, 9L, new UpdateRatingRequest(5, "수정 시도")));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.getCode()).isEqualTo("RATING403_1");
        assertThat(rating.getScore()).isEqualTo(3);
        assertThat(rating.getComment()).isEqualTo("원래 후기");
        verify(ratings, never()).save(any());
    }
}
