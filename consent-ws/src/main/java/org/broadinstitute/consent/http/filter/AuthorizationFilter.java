package org.broadinstitute.consent.http.filter;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.broadinstitute.consent.http.authentication.BasicAuthenticationAPI;
import org.broadinstitute.consent.http.authentication.GoogleAuthenticationAPI;
import org.broadinstitute.consent.http.enumeration.AuthenticationType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

public class AuthorizationFilter implements Filter {

    private FilterConfig filterConfig;

    private GoogleAuthenticationAPI googleAuthenticationAPI;
    private BasicAuthenticationAPI basicAuthenticationAPI;


    public AuthorizationFilter(GoogleAuthenticationAPI googleAuthenticationAPI, BasicAuthenticationAPI basicAuthenticationAPI) {
        this.googleAuthenticationAPI = googleAuthenticationAPI;
        this.basicAuthenticationAPI = basicAuthenticationAPI;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            if (!httpRequest.getMethod().equals(HttpMethod.OPTIONS)) {
                if (StringUtils.isNotEmpty(authHeader)) {
                    if (authHeader.startsWith(AuthenticationType.BEARER.getValue())) {
                        googleAuthenticationAPI.validateAccessToken(authHeader);
                        chain.doFilter(request, response);
                    } else if (authHeader.startsWith(AuthenticationType.BASIC.getValue())) {
                        basicAuthenticationAPI.validateUser(authHeader);
                        chain.doFilter(request, response);
                    } else {
                        // Header Authentication does not match the mapped ones. Exception -> Unauthorized
                        throw new Exception();
                    }
                }
            } else {
                chain.doFilter(request, response);
            }
        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

    }


}
