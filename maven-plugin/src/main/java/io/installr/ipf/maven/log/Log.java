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
package io.installr.ipf.maven.log;

public final class Log {

    private static final String LOG_PREFIX = "[IPF]";

    private org.apache.maven.plugin.logging.Log log;

    public Log( org.apache.maven.plugin.logging.Log log ) {
        this.log = log;
    }

    public void info( String format, Object ... args ) {
        log.info( format( format, args ) );
    }

    public void info( Throwable t ) {
        log.info( format( t ) );
    }

    public void warn( String format, Object ... args ) {
        log.warn( format( format, args ) );
    }

    public void warn( Throwable t ) {
        log.warn( format( t ) );
    }

    public void error( String format, Object ... args ) {
        log.error( format( format, args ) );
    }

    public void error( Throwable t ) {
        log.error( format( t ) );
    }

    public void debug( String format, Object ... args ) {
        log.debug( format( format, args ) );
    }

    public void debug( Throwable t ) {
        log.debug( format( t ) );
    }

    public void nl() {
        log.info( "" );
    }

    private String format( String format, Object ... args ) {
        return String
            .format( "\033[1m%s\033[0m %s", LOG_PREFIX, args == null ? format : String.format( format, args ) );
    }

    private String format( Throwable t ) {
        return format( t.getMessage() == null ? t.toString() : t.getMessage() );
    }

}
