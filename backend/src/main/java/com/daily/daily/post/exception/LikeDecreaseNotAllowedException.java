package com.daily.daily.post.exception;

public class LikeDecreaseNotAllowedException extends RuntimeException{
    public LikeDecreaseNotAllowedException() {
        super("좋아요를 취소할 수 없습니다. 좋아요를 누르지 않은 게시글 입니다.");
    }
}
