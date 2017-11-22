package com.github.onsdigital.search.response;

import org.apache.http.HttpStatus;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class HttpResponse {

    public static Response ok() {
        return Response.status(HttpStatus.SC_OK).build();
    }

    public static Response ok(Object entity) {
        if (entity instanceof Collection) {
            return ok(new ArrayList<>((Collection) entity));
        } else {
            return ok(Arrays.asList(entity));
        }
    }

    public static Response ok(List<Object> entities) {
        return Response.status(HttpStatus.SC_OK).entity(entities).build();
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
