package com.atlassian.ecohelp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class EcohelpResource {

    public EcohelpResource() {

    }

    /*
        curl --verbose --silent --user admin:admin --request GET --url 'http://localhost:7990/bitbucket/rest/ecohelp/1.0/hello' --header 'Accept: application/json' --header 'X-Atlassian-Token: no-check'
     */

    @GET
    @Path("/hello")
    @Produces(MediaType.APPLICATION_JSON)
    public Response doGet() throws Exception {
        return Response.ok()
                .entity("{\"hello\":\"world\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
