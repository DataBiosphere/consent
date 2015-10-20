package org.genomebridge.consent.http.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlets.CrossOriginFilter;

public class CORSFilter extends CrossOriginFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        handleCORS((HttpServletRequest) request, (HttpServletResponse) response, chain);
        super.doFilter(request, response, chain);
    }

    private void handleCORS(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        response.setHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
    }
}
