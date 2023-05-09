package org.broadinstitute.consent.http.util;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpClientUtil implements ConsentLogger {

    public record SimpleResponse(int code, String entity) {
    }

    private final ServicesConfiguration configuration;

    private final LoadingCache<URI, SimpleResponse> cache;

    public HttpClientUtil(ServicesConfiguration configuration) {
        this.configuration = configuration;
        CacheLoader<URI, SimpleResponse> loader = new CacheLoader<>() {
            @Override
            public SimpleResponse load(URI uri) throws Exception {
                return getHttpResponse(new HttpGet(uri));
            }
        };
        this.cache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(configuration.getCacheExpireMinutes(), TimeUnit.MINUTES)
                .build(loader);
    }

    /**
     * This returns the result of previously executed GET requests from a cache or executes them if
     * they are not yet cached.
     *
     * @param request The HttpGet request
     * @return SimpleResponse
     * @throws IOException The exception
     */
    public SimpleResponse getCachedResponse(HttpGet request) throws IOException {
        try {
            return cache.get(request.getUri());
        } catch (Exception e) {
            // Something went wrong with the cached version, log for followup
            logWarn(e.getMessage());
            return getHttpResponse(request);
        }
    }

    /**
     * This handles GET requests that could be subject to connection timeout errors by
     * limiting the request to a configured default number of seconds.
     *
     * @param request The HttpGet request
     * @return SimpleResponse
     * @throws IOException The exception
     */
    public SimpleResponse getHttpResponse(HttpGet request) throws IOException {
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            final ScheduledExecutorService executor = Executors.newScheduledThreadPool(configuration.getPoolSize());
            executor.schedule(request::cancel, configuration.getTimeoutSeconds(), TimeUnit.SECONDS);
            return httpclient.execute(request, httpResponse ->
                    new SimpleResponse(
                            httpResponse.getCode(),
                            IOUtils.toString(httpResponse.getEntity().getContent(), Charset.defaultCharset()))
            );
        }
    }

    public HttpRequest buildGetRequest(GenericUrl genericUrl, AuthUser authUser) throws Exception {
        HttpTransport transport = new NetHttpTransport();
        HttpRequest request = transport.createRequestFactory().buildGetRequest(genericUrl);
        request.setHeaders(buildHeaders(authUser));
        return request;
    }

    public HttpRequest buildUnAuthedGetRequest(GenericUrl genericUrl) throws Exception {
        HttpTransport transport = new NetHttpTransport();
        HttpRequest request = transport.createRequestFactory().buildGetRequest(genericUrl);
        request.setHeaders(new HttpHeaders().set("X-App-ID", "DUOS"));
        return request;
    }

    public HttpRequest buildPostRequest(GenericUrl genericUrl, HttpContent content, AuthUser authUser)
            throws Exception {
        HttpTransport transport = new NetHttpTransport();
        HttpRequest request = transport.createRequestFactory().buildPostRequest(genericUrl, content);
        request.setHeaders(buildHeaders(authUser));
        return request;
    }

    public HttpRequest buildUnAuthedPostRequest(GenericUrl genericUrl, HttpContent content)
            throws Exception {
        HttpTransport transport = new NetHttpTransport();
        HttpRequest request = transport.createRequestFactory().buildPostRequest(genericUrl, content);
        request.setHeaders(new HttpHeaders().set("X-App-ID", "DUOS"));
        return request;
    }

    public HttpRequest buildDeleteRequest(GenericUrl genericUrl, AuthUser authUser)
            throws Exception {
        HttpTransport transport = new NetHttpTransport();
        HttpRequest request = transport.createRequestFactory().buildDeleteRequest(genericUrl);
        request.setHeaders(buildHeaders(authUser));
        return request;
    }

    public HttpResponse handleHttpRequest(HttpRequest request) {
        try {
            request.setThrowExceptionOnExecuteError(false);
            HttpResponse response = request.execute();
            if (Objects.nonNull(response)) {
                return switch (response.getStatusCode()) {
                    case HttpStatusCodes.STATUS_CODE_BAD_REQUEST ->
                            throw new BadRequestException(response.getStatusMessage());
                    case HttpStatusCodes.STATUS_CODE_UNAUTHORIZED ->
                            throw new NotAuthorizedException(response.getStatusMessage());
                    case HttpStatusCodes.STATUS_CODE_FORBIDDEN ->
                            throw new ForbiddenException(response.getStatusMessage());
                    case HttpStatusCodes.STATUS_CODE_NOT_FOUND ->
                            throw new NotFoundException(response.getStatusMessage());
                    case HttpStatusCodes.STATUS_CODE_CONFLICT ->
                            throw new ConsentConflictException(response.getStatusMessage());
                    default -> response;
                };
            }
        } catch (IOException e) {
            throw new ServerErrorException("Server Error", HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        }
        throw new ServerErrorException("Server Error", HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    }

    private HttpHeaders buildHeaders(AuthUser authUser) {
        return new HttpHeaders()
                .setAuthorization("Bearer " + authUser.getAuthToken())
                .setAccept(MediaType.APPLICATION_JSON)
                .set("X-App-ID", "DUOS");
    }
}
