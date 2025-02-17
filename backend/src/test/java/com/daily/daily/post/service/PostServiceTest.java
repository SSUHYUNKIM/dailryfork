package com.daily.daily.post.service;

import com.daily.daily.common.exception.UnauthorizedAccessException;
import com.daily.daily.common.service.S3StorageService;
import com.daily.daily.member.repository.MemberRepository;
import com.daily.daily.post.domain.Post;
import com.daily.daily.post.dto.PostReadResponseDTO;
import com.daily.daily.post.dto.PostWriteRequestDTO;
import com.daily.daily.post.dto.PostWriteResponseDTO;
import com.daily.daily.post.repository.PostLikeRepository;
import com.daily.daily.post.repository.PostRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static com.daily.daily.member.fixture.MemberFixture.일반회원1;
import static com.daily.daily.member.fixture.MemberFixture.일반회원1_ID;
import static com.daily.daily.post.fixture.PostFixture.POST_ID;
import static com.daily.daily.post.fixture.PostFixture.게시글_요청_DTO;
import static com.daily.daily.post.fixture.PostFixture.다일리_페이지_이미지_파일;
import static com.daily.daily.post.fixture.PostFixture.일반회원1이_작성한_게시글;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostRepository postRepository;
    @Mock
    MemberRepository memberRepository;
    @Mock
    S3StorageService storageService;
    @Mock
    HashtagService hashtagService;
    @Mock
    PostLikeRepository likeRepository;
    @InjectMocks
    PostService postService;

    @Nested
    @DisplayName("create() - 게시글 생성 메서드 테스트")
    class create {
        @Test
        @DisplayName("이미지 파일을 업로드하는 메서드와, 게시글을 저장하는 메서드는 반드시 한 번 이상 호출되어야 한다." +
                "그리고 PostResponse의 페이지 이미지 URL 경로는 storageService.uploadImage() 의 반환값으로 받은 URL 경로와 일치한다.")
        void test1() {
            //given
            PostWriteRequestDTO 게시글_작성_요청_DTO = 게시글_요청_DTO();
            Post 일반회원1이_작성한_게시글 = 일반회원1이_작성한_게시글();

            when(memberRepository.findById(any())).thenReturn(Optional.of(일반회원1()));
            when(storageService.uploadImage(any(), any(), any())).thenReturn("post/3-awef-123-124-wafewe123123_asdf.png");
            when(postRepository.save(any())).thenReturn(일반회원1이_작성한_게시글);

            //when
            PostWriteResponseDTO result = postService.create(2L, 게시글_작성_요청_DTO, 다일리_페이지_이미지_파일());

            //then
            verify(storageService, times(1)).uploadImage(any(), any(), any());
            verify(postRepository, times(1)).save(any());
            verify(hashtagService, times(1)).addHashtagsToPost(일반회원1이_작성한_게시글, 게시글_작성_요청_DTO.getHashtags());
            assertThat(result.getPageImage()).isEqualTo("post/3-awef-123-124-wafewe123123_asdf.png");
            assertThat(new HashSet<>(result.getHashtags())).isEqualTo(게시글_작성_요청_DTO.getHashtags());
        }
    }

    @Nested
    @DisplayName("update() - 게시글 수정 테스트")
    class updatePost {

        @Test
        @DisplayName("게시글 수정 요청에 pageImage파일이 null이면 해당 포스트의 pageImage값은 그대로여야한다.")
        void test1() {
            //given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(일반회원1이_작성한_게시글()));

            //when
            PostWriteResponseDTO updateResult = postService.update(일반회원1_ID, POST_ID, new PostWriteRequestDTO(), null);

            //then
            assertThat(updateResult.getPageImage()).isEqualTo(일반회원1이_작성한_게시글().getPageImage());
        }

        @Test
        @DisplayName("게시글 수정 요청에 pageImage파일이 존재할 때, " +
                "PostResponse의 페이지 이미지 URL 경로는 storageService.uploadImage() 의 반환값으로 받은 URL 경로와 일치한다.")
        void test2() {
            //given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(일반회원1이_작성한_게시글()));
            when(storageService.uploadImage(any(), any(), any())).thenReturn("post/4-awef-1231-xcvsdf-12312_qwf.png");

            //when
            PostWriteResponseDTO updateResult = postService.update(일반회원1_ID, POST_ID, new PostWriteRequestDTO(), 다일리_페이지_이미지_파일());

            //then
            assertThat(updateResult.getPageImage()).isEqualTo("post/4-awef-1231-xcvsdf-12312_qwf.png");
        }

        @Test
        @DisplayName("게시글 작성자가 아닌 다른 사람이 게시글을 수정하려고 하면 UnauthorizedAccessException이 발생한다.")
        void test3() {
            //given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(일반회원1이_작성한_게시글()));

            //when, then
            Assertions.assertThatThrownBy(() -> postService.update(일반회원1_ID + 3, POST_ID, new PostWriteRequestDTO(), 다일리_페이지_이미지_파일()))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("find() - 게시글 단건 조회 테스트")
    class find {

        @Test
        @DisplayName("likeRepository에서 반환한 좋아요 갯수와, 반환하는 DTO의 좋아요 갯수는 같아야한다.")
        void test1() {
            //given
            when(postRepository.findById(any())).thenReturn(Optional.of(일반회원1이_작성한_게시글()));
            when(likeRepository.countByPost(any())).thenReturn(5L);

            //when
            PostReadResponseDTO result = postService.find(POST_ID);

            //then
            assertThat(result.getLikeCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("delete() - 게시글 삭제 테스트")
    class delete {
        @Test
        @DisplayName("게시글 작성자가 아닌 다른 사람이 게시글을 삭제하려고 하면 UnauthorizedAccessException이 발생한다.")
        void test1() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(일반회원1이_작성한_게시글()));

            Assertions.assertThatThrownBy(() -> postService.delete(일반회원1_ID + 3, POST_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }
}