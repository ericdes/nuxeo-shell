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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands.system;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;

public class LogCommand extends AbstractCommand {
    private static final String DEFAULT_CATEGORY = "org.nuxeo.ecm.shell";

    private static final Log log = LogFactory.getLog(LogCommand.class);

    private static Map<String, Logger> loggers = new HashMap<String, Logger>();

    private static String PATTERN_LAYOUT = "%d{HH:mm:ss,SSS} %-5p [%C{1}] %m%n";

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: log filename [log level [package or class]]");
        System.out.println("        log off [package or class]");
        System.out.println(" filename : logging file destination (writes in append mode)");
        System.out.println(" log level : (optionnal, default=INFO): available values are TRACE, DEBUG, INFO, WARN, ERROR, FATAL");
        System.out.println(" package or class (optionnal, default=\"org.nuxeo.ecm.shell\"): category logged (see Log4J doc. for more details)");
        System.out.println("");
        System.out.println("\"log off\" command stop given logger or all custom loggers if none is specified");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        if (elements.length == 0) {
            log.error("SYNTAX ERROR: the log command must take at least one argument: log filename");
            printHelp();
            return;
        } else if ("off".equals(elements[0])) {
            removeLogger(elements);
        } else {
            try {
                setLogger(elements);
            } catch (FileNotFoundException e) {
                log.error("Couldn't create or open " + elements[0], e);
            }
        }
    }

    private void removeLogger(String[] elements) {
        if (elements.length > 1) {
            loggers.get(elements[1]).removeAllAppenders();
        } else {
            loggers.get(DEFAULT_CATEGORY).removeAllAppenders();
        }
    }

    private void setLogger(String[] elements) throws FileNotFoundException {
        Logger logger;
        if (elements.length > 2) {
            logger = Logger.getLogger(elements[2]);
        } else {
            logger = Logger.getLogger(DEFAULT_CATEGORY);
        }
        OutputStream out = new FileOutputStream(elements[0]);
        Appender appender = new WriterAppender(
                new PatternLayout(PATTERN_LAYOUT), out);
        logger.addAppender(appender);

        if (elements.length > 1) {
            logger.setLevel(Level.toLevel(elements[1]));
        } else {
            logger.setLevel(Level.INFO);
        }
        loggers.put(logger.getName(), logger);
    }

}
