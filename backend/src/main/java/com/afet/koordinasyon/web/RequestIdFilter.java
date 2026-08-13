package com.afet.koordinasyon.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Her istek için bir correlation/request ID üretir (veya istemcinin gönderdiği güvenli formattaki
 * değeri kabul eder), MDC'ye ve response header'ına yazar, ve istek tamamlandığında tek satırlık
 * bir özet log kaydı bırakır. Spring Security filtre zincirinden ÖNCE çalışır (HIGHEST_PRECEDENCE)
 * — böylece /actuator/health gibi permitAll uçlar da dahil TÜM istekler kapsanır.
 *
 * userId MDC anahtarı burada değil, kimlik doğrulama başarılı olduğunda JwtAuthenticationFilter
 * tarafından doldurulur (aynı thread, aynı MDC context'i) — bu filtre yalnızca okur.
 *
 * Loglanmaz: Authorization header, cookie, token, request/response body, query string (hassas
 * parametre — örn. e-posta doğrulama token'ı — içerebileceği için körlemesine loglanmaz, yalnızca
 * path loglanır).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger requestLog = LoggerFactory.getLogger("com.afet.koordinasyon.RequestLog");
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[a-zA-Z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        String requestId = (StringUtils.hasText(incoming) && SAFE_REQUEST_ID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            requestLog.info("requestId={} method={} path={} status={} durationMs={} userId={}",
                    requestId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                    durationMs, MDC.get(MDC_USER_ID));
            MDC.clear();
        }
    }
}
