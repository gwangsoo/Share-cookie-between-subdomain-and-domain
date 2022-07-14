# 도메인과 서브도메인 간 세션 및 쿠키 공유 방법
Share cookie between subdomain and domain

- 도메인과 서브도메인 사이에 Cookie 기반 세션을 공유하는 방법입니다.
- Nginx 로 서브도메인 환경을 만들고 Springboot 으로 서버는 하나만 실행하였습니다.
- Springboot 서버를 여러개 띄우고 세션을 공유하려면 spring-redis-session, spring-jdbc-session 으로 가능합니다.
아니면 JWT 처럼 다른 방법도 있겠습니다.
- 서브도메인 사이에 Cookie 를 공유하는 것이 핵심이므로 굳이 서버를 도메인별로 실행하지 않아도 테스트는 가능합니다.

## 개발환경
- Springboot 2.7.1
- spring-session
- nginx (Docker based)

## Nginx Subdomain 환경 구성
### Nginx 구성
- domain : mysite.localhost
- filepath : nginx/sites-enabled/mysite.conf
    ```
    server {
        listen 80;
        server_name mysite.localhost;
    
        location /
        {
            proxy_pass http://host.docker.internal:8080;
        }
    }
    ```
- subdomain : sub.mysite.localhost
- filepath : nginx/sites-enabled/sub.mysite.conf
    ```
    server {
        listen 80;
        server_name sub.mysite.localhost;
    
        location /
        {
            proxy_pass http://host.docker.internal:8080;
        }
    }
    ```

### Nginx 실행
```
docker-compose up -d
```

## Spring boot

### build.gradle
테스트를 위해 spring-session 의존성을 추가 하였음.
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.session:spring-session-core'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```
### application.properties
Cookie의 Domain에 넣을 값을 상위 도메인으로 해야함.
```yaml
server.servlet.session.cookie.domain=mysite.localhost
```

### RestController
- 세션을 생성하고 확인하기 위한 아주 간단한 소스코드.
- GET /login 으로 세션을 생성하고 세션에 임의의 값을 저장.
- GET / 으로 후 세션이 있으면 세션의 임의의 값을 조회하고 없으면 세션공유실패 메세지 출력함.
- 또한 임의의 Cookie **mycookie** 를 생성 후 서브도메인간 공유되는지 확인 함.
```java
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
```

### 실행
```
./gradlew bootRun

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.7.1)

2022-07-14 17:03:56.646  INFO 39484 --- [           main] c.e.subdomain.SubdomainApplication       : Starting SubdomainApplication using Java 11.0.11 on DESKTOP-17KMBPN with PID 39484 (C:\Workspace\hiphoper\subdomain\build\classes\java\main started by gwang in C:\Workspace\hiphoper\subdomain)
2022-07-14 17:03:56.649  INFO 39484 --- [           main] c.e.subdomain.SubdomainApplication       : No active profile set, falling back to 1 default profile: "default"
2022-07-14 17:03:59.141  INFO 39484 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2022-07-14 17:03:59.150  INFO 39484 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2022-07-14 17:03:59.150  INFO 39484 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.64]
2022-07-14 17:03:59.231  INFO 39484 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2022-07-14 17:03:59.231  INFO 39484 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 2549 ms
2022-07-14 17:03:59.467  INFO 39484 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2022-07-14 17:03:59.474  INFO 39484 --- [           main] c.e.subdomain.SubdomainApplication       : Started SubdomainApplication in 3.175 seconds (JVM running for 3.493)
```

## Test
Nginx와 Springboot 모두 실행 하고 브라우져에서 테스트
### 세션 생성
```
http://mysite.localhost/login
```
### 세션 및 쿠키 확인
```
http://sub.mysite.localhost
```

## 결론
- 생각보다 너무 간단해서 ㅎㅎ
- 당연하게도 도메인이 같은 서브도메인끼리만 가능합니다.
- 도메인이 완전히 달라진다면 그때는 OAuth나 SAML같은 SSO 방법을 사용해야 합니다.
