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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import io.installr.ipf.maven.log.Log;
import io.installr.lib.pkg.PackageId;

public abstract class AbstractIpfMojo extends AbstractMojo {

    @SuppressWarnings( "deprecation" )
    @Component
    private SettingsDecrypter mavenSettingsDecrypter;

    /**
     * Maven settings
     */
    @Parameter(
        defaultValue = "${settings}",
        readonly = true,
        required = true
    )
    private Settings mavenSettings;

    /**
     * Id of the final application package.
     */
    @Parameter(
        property = "ipf.packageId",
        required = true,
        defaultValue = "${project.artifactId}:${project.version}"
    )
    PackageId packageId;

    private Log log;

    Log log() {
        if ( log == null ) {
            log = new Log( getLog() );
        }
        return log;
    }

    Server mavenServerSettings( String serverId ) throws MojoFailureException {
        final Server server = mavenSettings.getServer( serverId );
        if ( server == null ) {
            throw new MojoFailureException(
                String.format( "No server configuration with id '%s' found in settings.xml.", serverId )
            );
        }
        return mavenSettingsDecrypter.decrypt( new DefaultSettingsDecryptionRequest( server ) ).getServer();
    }

}
