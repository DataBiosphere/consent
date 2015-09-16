/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.genomebridge.consent.http.security;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author dgil
 */
@Provider 
public class ResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext crc, ContainerResponseContext crc1) throws IOException {
        final SecurityContext securityContext = crc.getSecurityContext();
        
        String origin = crc.getHeaderString("Origin");
        
        if (crc.getMethod().equalsIgnoreCase("OPTIONS")) {
            MultivaluedMap<String, String> headers = crc.getHeaders();
            System.out.println("------------------ ResponseFilter REQUEST HEADERS -------------");
            headers.entrySet().forEach((entry) -> {
                System.out.println(entry.getKey() + " : " + entry.getValue().toString());
            });
        } else {
            MultivaluedMap<String, Object> headers = crc1.getHeaders();
            System.out.println("------------------ ResponseFilter RESPONSE HEADERS -------------");
            headers.entrySet().forEach((entry) -> {
                System.out.println(entry.getKey() + " : " + entry.getValue().toString());
            });

        }
    }
}
