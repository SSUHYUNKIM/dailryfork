package com.daily.daily.auth.service;

import com.daily.daily.auth.exception.LoginFailureException;
import com.daily.daily.auth.exception.TokenExpiredException;
import com.daily.daily.auth.jwt.JwtUtil;
import com.daily.daily.auth.jwt.RefreshToken;
import com.daily.daily.auth.jwt.RefreshTokenRepository;
import com.daily.daily.member.domain.Member;
import com.daily.daily.member.exception.MemberNotFoundException;
import com.daily.daily.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    private static final String ACCESS_TOKEN = "AccessToken";

    public void renewToken(HttpServletResponse response, String accessToken, RefreshToken refreshToken) {
        boolean isAccessTokenExpired = jwtUtil.isExpired(accessToken);
        boolean isRefreshTokenExpired = jwtUtil.isExpired(refreshToken.getRefreshToken());

        if (isAccessTokenExpired && isRefreshTokenExpired) {
            throw new TokenExpiredException();
        }

        if(isAccessTokenExpired) {
            String renewAccessToken = createAccessToken(refreshToken);
            ResponseCookie accessCookie = jwtUtil.createTokenCookie(ACCESS_TOKEN, renewAccessToken);
            response.addHeader(SET_COOKIE, accessCookie.toString());
        }
    }

    public String createAccessToken(final RefreshToken refreshToken) {
        RefreshToken refreshToken1 = refreshTokenRepository.findById(refreshToken.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Member member = memberRepository.findById(refreshToken1.getId())
                .orElseThrow(MemberNotFoundException::new);

        return jwtUtil.generateAccessToken(refreshToken1.getId(), member.getRole());
    }
}
