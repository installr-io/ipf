/*
 * Installr.io
 *
 * Copyright (c) 2025-present Dorian Vallant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.installr.ipf.maven;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;

@Mojo( name = "push", requiresProject = false )
public final class PushMojo extends AbstractIpfMojo {

    /**
     * Directory where the final package is stored.
     */
    @Parameter(
        property = "ipf.target.directory",
        required = true,
        defaultValue = "${project.build.directory}"
    )
    private Path targetDirectory;

    /**
     * URL of the repository to which the final package should be pushed.
     */
    @Parameter(
        property = "ipf.repository.url",
        required = true
    )
    private URL repositoryUrl;

    /**
     * Server id in settings.xml that should be used for the credentials to authenticate against the repository server.
     */
    @Parameter(
        property = "ipf.repository.auth.server-id",
        required = false
    )
    private String repositoryAuthServerId;

    /**
     * Skip mojo execution.
     */
    @Parameter(
        property = "ipf.skip.push",
        required = false,
        defaultValue = "false"
    )
    private boolean skipPush;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if ( skipPush ) {
                log().info( "Mojo execution skipped." );
                return;
            }
            log().info( "Pushing package '%s'...", packageId );
            final Path packagePath = targetDirectory.resolve( packageId.toArchiveName() );
            if ( !Files.exists( packagePath ) ) {
                log().warn( "Package path does not exist: %s", packagePath );
                log().warn( "Skipping execution..." );
                return;
            }
            log().nl();
            log().info( "Package path: %s", packagePath );
            log().info( "Repository URL: %s", repositoryUrl );
            log().nl();
            pushPackage( packagePath );
            log().info( "Package '%s' successfully pushed.", packageId );
        }
        catch ( Throwable t ) {
            log().error( t );
            throw new MojoExecutionException( t );
        }
    }

    private void pushPackage( Path packagePath ) throws IOException, MojoFailureException {
        // I give up :(
        // I found no way to use the `uploadPackage? method from the RepositoryClient because I didn't find a
        // dependency which has a provider for `multipart/form-data`...

        // final PackageUpload upload = new PackageUpload();
        // upload.setRepositoryId( "snapshots" );
        // upload.setPackageData( Files.readAllBytes( packagePath ) );
        // final RepositoryClient client = new RepositoryClient( repositoryUrl );
        // client.uploadPackage( upload );

        // Use Apache HttpClient as workaround
        final HttpEntity entity = MultipartEntityBuilder
            .create()
            .setMode( HttpMultipartMode.STRICT )
            .addBinaryBody( "packageData", Files.readAllBytes( packagePath ) )
            .build();

        final HttpPut put = new HttpPut( repositoryUrl.toString() + "/packages" );
        put.setEntity( entity );

        try ( CloseableHttpClient _httpClient = HttpClients.custom().build() ) {
            try ( CloseableHttpResponse response = _httpClient.execute( put, clientContext() ) ) {
                int code = response.getStatusLine().getStatusCode();
                if ( code != 201 ) {
                    throw new IOException( "status code = " + code );
                }
            }
        }
    }

    private HttpClientContext clientContext() throws MojoFailureException {
        final String host = repositoryUrl.getHost();
        final int port = repositoryUrl.getPort() == -1 ? repositoryUrl.getDefaultPort() : repositoryUrl.getPort();
        final String proto = repositoryUrl.getProtocol();

        final HttpHost targetHost = new HttpHost( host, port, proto );
        final AuthCache authCache = new BasicAuthCache();
        authCache.put( targetHost, new BasicScheme() );

        final HttpClientContext context = HttpClientContext.create();
        context.setAuthCache( authCache );

        if ( repositoryAuthServerId != null ) {
            final Server server = mavenServerSettings( repositoryAuthServerId );
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                new AuthScope( host, port ),
                new UsernamePasswordCredentials( server.getUsername(), server.getPassword() )
            );
            context.setCredentialsProvider( credentialsProvider );
        }
        return context;
    }

}
