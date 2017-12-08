package com.github.onsdigital.search.response;

import org.apache.http.HttpStatus;

import javax.ws.rs.core.Response;

public class HttpResponse {

    public static Response ok() {
        return Response.status(HttpStatus.SC_OK).build();
    }

    public static Response ok(Object entity) {
        return Response.status(HttpStatus.SC_OK).entity(entity).build();
    }

    public static Response created() { return Response.status(HttpStatus.SC_CREATED).build(); }

    public static Response created(Object entity) {
        return Response.status(HttpStatus.SC_CREATED).entity(entity).build();
    }

    public static Response internalServerError() {
        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
    }

    public static Response internalServerError(Exception e) {
        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }

}
