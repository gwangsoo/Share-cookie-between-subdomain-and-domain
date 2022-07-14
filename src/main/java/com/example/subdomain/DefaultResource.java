package com.example.subdomain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.util.UUID;

@RestController
public class DefaultResource {

    @Value("${server.servlet.session.cookie.domain}")
    private String domain;

    // 세션생성
    @GetMapping("/login")
    public ResponseEntity login() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        // - Session Check
        HttpSession session = request.getSession(true);
        session.setAttribute("random", UUID.randomUUID().toString());

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("http://sub.mysite.localhost")).build();
    }

    // 세션공유 확인
    @GetMapping("/")
    public ResponseEntity<String> index() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        /**
         * 쿠키의 생성에 필요한 항목 참고
         * {@link org.springframework.boot.web.server.Cookie}
          */
        HttpHeaders headers = new HttpHeaders();

        // - Session Check
        HttpSession session = request.getSession(false);
        if(session != null) {
            headers.add("Set-Cookie","mycookie=success; domain="+domain+"; Path=/");
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body("세션공유 성공 : " + session.getAttribute("random").toString());
        }
        else {
            headers.add("Set-Cookie","mycookie=fail; domain="+domain+"; Path=/");
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body("세션공유 실패!!");
        }
    }
}
