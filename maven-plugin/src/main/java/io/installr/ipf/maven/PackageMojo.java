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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.installr.lib.pkg.packager.ContentAssignment;
import io.installr.lib.pkg.packager.InstallrPackager;
import io.installr.lib.pkg.packager.PackagerConfig;

@Mojo( name = "package", requiresProject = false )
public final class PackageMojo extends AbstractIpfMojo {

    /**
     * Directory where the source files to package are located.
     */
    @Parameter(
        property = "ipf.source.directory",
        required = true,
        defaultValue = "${project.build.directory}/ipf-sources"
    )
    private Path sourceDirectory;

    /**
     * Directory where the final package should be stored.
     */
    @Parameter(
        property = "ipf.target.directory",
        required = true,
        defaultValue = "${project.build.directory}"
    )
    private Path targetDirectory;

    /**
     * Location of the json file which contains the content assignment configuration.
     */
    @Parameter(
        property = "ipf.ca-config.location",
        required = true,
        defaultValue = "classpath:ipf-ca-config.json"
    )
    private String caConfigLocation;

    /**
     * Set the content assignment in pom.xml (or via command line). This configuration has precedence over
     * caConfigLocation.
     */
    @Parameter(
        property = "ipf.content-assignment"
    )
    private List<ContentAssignment> contentAssignments;

    /**
     * Skip mojo execution.
     */
    @Parameter(
        property = "ipf.skip.package",
        required = false,
        defaultValue = "false"
    )
    private boolean skipPackage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if ( skipPackage ) {
                log().info( "Mojo execution skipped." );
                return;
            }
            log().info( "Starting package generation for '%s'...", packageId );
            if ( !Files.exists( sourceDirectory ) ) {
                log().warn( "Source directory does not exist: %s", sourceDirectory );
                log().warn( "Skipping execution..." );
                return;
            }
            if ( !Files.exists( targetDirectory ) ) {
                log().info( "Target directory does not exist. Creating it..." );
                Files.createDirectories( targetDirectory );
                log().info( "Target directory successfully created." );
            }
            log().nl();
            log().info( "Source directory: %s", sourceDirectory );
            log().info( "Target directory: %s", targetDirectory );
            log().nl();
            final Path archive = makePackage();
            log().info( "Package '%s' successfully generated.", packageId );
            log().info( archive.toString() );
        }
        catch ( Throwable t ) {
            log().error( t );
            throw new MojoExecutionException( t );
        }
    }

    private Path makePackage() throws IOException {
        final PackagerConfig config = new PackagerConfig();
        config.setSource( sourceDirectory );
        config.setTarget( targetDirectory );
        config.setContentAssignments( readContentAssignments() );
        final InstallrPackager packager = new InstallrPackager( packageId, config );
        return packager.makePackage();
    }

    private List<ContentAssignment> readContentAssignments() throws IOException {
        final List<ContentAssignment> effectiveAssignments = new ArrayList<>();
        if ( contentAssignments != null && !contentAssignments.isEmpty() ) {
            log().info( "Reading content assignment configuration from pom.xml..." );
            effectiveAssignments.addAll( contentAssignments );
        }
        if ( caConfigLocation.startsWith( "classpath:" ) ) {
            final URL resource = getClass().getClassLoader().getResource( caConfigLocation.substring( 10 ) );
            final List<ContentAssignment> contentAssignmentsFromClassPath = readContentAssignmentsFromClassPath(
                resource
            );
            if ( contentAssignmentsFromClassPath != null && !contentAssignmentsFromClassPath.isEmpty() ) {
                effectiveAssignments.addAll( contentAssignmentsFromClassPath );
            }
            return effectiveAssignments;
        }
        if ( caConfigLocation.startsWith( "file://" ) ) {
            caConfigLocation = caConfigLocation.substring( 7 );
        }
        final List<ContentAssignment> contentAssignmentsFromFile = readContentAssignmentsFromFile(
            Path.of( caConfigLocation )
        );
        if ( contentAssignmentsFromFile != null && !contentAssignmentsFromFile.isEmpty() ) {
            effectiveAssignments.addAll( contentAssignmentsFromFile );
        }
        return effectiveAssignments;
    }

    private List<ContentAssignment> readContentAssignmentsFromClassPath( URL resource ) throws IOException {
        if ( resource == null ) {
            return null;
        }
        log().info( "Reading content assignment configuration from classpath: '%s'...", caConfigLocation );
        return new ObjectMapper().readValue( resource, new TypeReference<List<ContentAssignment>>() {} );
    }

    private List<ContentAssignment> readContentAssignmentsFromFile( Path location ) throws IOException {
        if ( !Files.exists( location ) ) {
            throw new IllegalArgumentException( "ca-config location not found: " + caConfigLocation );
        }
        log().info( "Reading content assignment configuration from file: '%s'...", location );
        return new ObjectMapper().readValue(
            Files.readAllBytes( location ),
            new TypeReference<List<ContentAssignment>>() {}
        );
    }

}
