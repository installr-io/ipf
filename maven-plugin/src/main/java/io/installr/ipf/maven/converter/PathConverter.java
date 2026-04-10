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
package io.installr.ipf.maven.converter;

import java.nio.file.Path;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;

@Component( role = ConfigurationConverter.class, hint = "path" )
public class PathConverter extends AbstractBasicConverter {

    @Override
    public boolean canConvert( Class<?> clazz ) {
        return Path.class.isAssignableFrom( clazz );
    }

    @Override
    protected Object fromString( final String str )
        throws ComponentConfigurationException {
        return Path.of( str );
    }

}
