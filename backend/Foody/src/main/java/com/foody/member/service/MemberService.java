package com.foody.member.service;

import com.foody.global.exception.ErrorCode;
import com.foody.security.util.JwtProvider;
import com.foody.member.dto.request.MemberSignupRequest;
import com.foody.member.dto.response.TokenResponse;
import com.foody.member.entity.Member;
import com.foody.member.exception.MemberException;
import com.foody.member.repository.MemberRepository;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    @Value("${jwt.token.secret}")
    private String secretKey;

    @Transactional(readOnly = true)
    public void isNicknameDuplicated(String nickname) {
        boolean memberExists = memberRepository.existsByNickname(nickname);

        if(memberExists) {
            throw new MemberException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    @Transactional(readOnly = true)
    public void isEmailDuplicated(String email) {
        boolean memberExists = memberRepository.existsByEmail(email);

        if(memberExists) {
            throw new MemberException(ErrorCode.EMAIL_DUPLICATED);
        }
    }

    @Transactional
    public TokenResponse loginMember(HttpServletResponse response, Authentication authentication, OAuth2User oAuth2User)
        throws IOException {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        log.info("{} member request login",email);

        // 가입된 이메일 없음 -> findByEmail에서 처리
        Member member = findByEmail(email);

        String accessToken = jwtProvider.createAccessToken(member.getId(), email, secretKey, true);
        String refreshToken = jwtProvider.createRefreshToken(email, secretKey);

        TokenResponse tokenResponse = new TokenResponse(member.getId(), accessToken, refreshToken);

        // 토큰 반환
        return tokenResponse;
    }

    @Transactional
    public TokenResponse signUpMember(HttpServletResponse response, Authentication authentication,
        OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String profileImg = (String) attributes.get("picture");

        log.info("{} signup request", email);
        log.info("{} ",profileImg);
        // 이메일 중복검사
        isEmailDuplicated(email);
        MemberSignupRequest memberSignupRequest = new MemberSignupRequest(email, profileImg);

        Member member = Member.signupMember(memberSignupRequest);

        memberRepository.save(member);

        String accessToken = jwtProvider.createAccessToken(member.getId(), email, secretKey, false);
        String refreshToken = jwtProvider.createRefreshToken(email, secretKey);

        TokenResponse tokenResponse = new TokenResponse(member.getId(), accessToken, refreshToken);

        // 토큰 반환
        return tokenResponse;
    }

    @Transactional(readOnly = true)
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                               .orElseThrow(() -> new MemberException(ErrorCode.EMAIL_NOT_FOUND));
    }


}