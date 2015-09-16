/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.genomebridge.consent.http.security;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author dgil
 */
@Provider 
public class RequestFilter implements ContainerRequestFilter {

    /**
     *
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {

        final SecurityContext securityContext = requestContext.getSecurityContext();
        
        String origin = requestContext.getHeaderString("Origin");
        
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            Response resp = Response.status(Response.Status.OK).header("Access-Control-Allow-Origin", origin).build();
            MultivaluedMap<String, Object> headers = resp.getHeaders();
            System.out.println("------------------ AuthorizationRequestFilter REQUEST HEADERS OPTIONS -------------");
            headers.entrySet().forEach((entry) -> {
                System.out.println(entry.getKey() + " : " + entry.getValue().toString());
            });
            requestContext.abortWith(resp);
        } else {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            System.out.println("------------------ AuthorizationRequestFilter REQUEST HEADERS OTHERS -------------");
            headers.entrySet().forEach((entry) -> {
                System.out.println(entry.getKey() + " : " + entry.getValue().toString());
            });
//            headers.add("Access-Control-Allow-Origin", origin);
        }
    }
}
