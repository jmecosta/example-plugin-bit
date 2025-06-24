package com.atlassian.ecohelp.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.bitbucket.rest.v2.api.resolver.RepositoryResolver;
import com.atlassian.bitbucket.rest.v2.api.util.ResourcePatterns;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.bitbucket.i18n.I18nService;

import com.atlassian.bitbucket.rest.v2.api.resolver.RepositoryResolver;


import javax.inject.Inject;

@Path(ResourcePatterns.REPOSITORY_URI)
public class TeamcityResource {

    /**
     * Creates Rest resource for testing the Jenkins configuration
     *
     * @param i18nService i18n Service
     */
    //@Inject
    //public TeamcityResource(final TeamcityConnectionSettings connectionSettings) {
    //    this.connectionSettings = connectionSettings;
    //}



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
          
  @GET
  @Path(value = "/loadhtml")
  @Produces(MediaType.TEXT_HTML)
  public Response loadhtml(@QueryParam("page") final String page) {

    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    
    return Response.ok()
            .entity(file)
            .build();
  }    

  @GET
  @Path(value = "loadjs")
  @Produces("text/javascript")
  public String loadjs(@QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }  

  @GET
  @Path(value = "loadimg")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response loadimg(@QueryParam("img") final String img) {
    return Response.ok(getResourceAsFile("public/" + img), MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename=\"" + img + "\"").build();
  }  

  @GET
  @Path(value = "loadcss")
  @Produces("text/css")
  public String loadcss(@QueryParam("page") final String page) {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    final InputStream is = classloader.getResourceAsStream("public/" + page);
    final String file = convertStreamToString(is);
    return file;
  }

  /**
   * Trigger a build on the Teamcity instance using vcs root
   *
   * @param repositoryResolver - {@link Repository}
   * @param url - url to TeamCity server
   * @param username - TeamCity user name
   * @param password - TeamCity user password
   * @return "OK" if it worked. Otherwise, an error message.
   */
  @GET
  @Path(value = "testconnection")
  @Produces("text/plain; charset=UTF-8")
  public Response testconnection(
          @BeanParam final RepositoryResolver repositoryResolver,
          @QueryParam("url") final String url,
          @QueryParam("username") final String username,
          @QueryParam("password") final String password,
          @QueryParam("debugon") final String isDebugOn) {

        return Response.ok()
                .entity(repositoryResolver.getRepository())
                .type(MediaType.APPLICATION_JSON)
                .build();
  }  


static String convertStreamToString(final java.io.InputStream is) {
    final java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

public static File getResourceAsFile(final String resourcePath) {
    try {
      final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      final InputStream in = classloader.getResourceAsStream(resourcePath);

      if (in == null) {
        return null;
      }

      final File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
      tempFile.deleteOnExit();

      try (FileOutputStream out = new FileOutputStream(tempFile)) {
        // copy stream
        final byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
      }
      return tempFile;
    } catch (final IOException e) {
      return null;
    }
  }  

}
