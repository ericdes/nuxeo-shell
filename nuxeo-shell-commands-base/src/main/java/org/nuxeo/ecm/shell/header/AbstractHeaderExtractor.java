/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.header;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractHeaderExtractor implements HeaderExtractor {

    public final static String CRLF = System.getProperty("line.separator");

    public CommandHeader extractHeader(Reader r) throws IOException,  ParseException {
        BufferedReader reader  = null;
        if (reader instanceof BufferedReader) {
            reader = (BufferedReader)r;
        } else {
            reader = new BufferedReader(r);
        }
        CommandHeader header = null;
        String line = reader.readLine();
        StringBuilder buf = null;
        while (line != null) {
            line = trimLine(line);
            if (line.isEmpty()) {
                if (buf != null) {
                    buf.append(CRLF);
                } else {
                    line = reader.readLine();
                    continue;
                }
            }
            if (isHeaderBoundary(line)) {
                if (header == null) {
                    header = new CommandHeader();
                } else {
                    if (buf != null) {
                        header.help = buf.toString();
                    } else {
                        header.help = "N/A";
                    }
                    return header;
                }
            } else {
                if (header == null) {
                    return null;
                }
                if (header.description == null) {
                    header.description = line;
                } else if (header.pattern == null) {
                    header.pattern = CommandPattern.parsePattern(line);
                } else {
                    if (buf == null) {
                        buf = new StringBuilder();
                    }
                    buf.append(line).append(CRLF);
                }
            }
            line = reader.readLine();
        }
        return header;
    }

    protected String trimLine(String line) {
        return line.trim();
    }

    protected abstract boolean isHeaderBoundary(String line);

}
