/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.shell.commands;

import java.io.PrintStream;

import jline.ConsoleReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandDescriptor;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.CommandLineService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class InteractiveCommand implements Command {

    private static final Log log = LogFactory.getLog(InteractiveCommand.class);

    private CommandLineService service;
    private ConsoleReader console;

    public ConsoleReader getConsole() {
        return console;
    }

    public CommandLineService getService() {
        return service;
    }

    public void run(CommandLine cmdLine) throws Exception {
        service = Framework.getService(CommandLineService.class);
        console = new ConsoleReader();
        CompositeCompletor completor = new CompositeCompletor(this);
//        ArgumentCompletor completor = new ArgumentCompletor(
//                new Completor[] {new SimpleCompletor(service.getCommandNames()),
//                cc});
//        completor.setStrict(false);
        console.addCompletor(completor);

        while (true) {
            updatePrompt();
            String line = console.readLine();
            if (line == null) {
                // EOF
                break;
            }
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (processInput(line)) {
                break;
            }
        }
    }

    public void printHelp(PrintStream out) {
        out.print("A mode where commands can be run interactively. TODO");
    }

    boolean processInput(String input) {
        String[] args = input.split("[ ]+");
        try {
            CommandLine cmdLine = service.parse(args, true);
            String cmdName = cmdLine.getCommand();
            if ("exit".equals(cmdName) || "quit".equals(cmdName) || "bye".equals(cmdName)) {
                return true;
            }
            runCommand(cmdLine);
        } catch (Throwable e) {
            log.error("Command failed.",e);
        }
        return false;
    }

    void runCommand(CommandLine cmdLine) throws Exception {
        CommandDescriptor cd = service.getCommand(cmdLine.getCommand());
        if (cd == null) {
            log.error("Unknown command: "+cmdLine.getCommand());
        } else {
            service.runCommand(cd, cmdLine);
        }
    }

    void updatePrompt() {
        if (NuxeoClient.getInstance().isConnected()) {
            console.setDefaultPrompt(service.getCommandContext().getHost()+"> ");
        } else {
            console.setDefaultPrompt("|> ");
        }
    }

    /**
     * For now only command auto-completion is available.
     *
     * @param sb
     */
    void autoComplete(StringBuilder sb) {
        // TODO: add auto-completion for File System files and repository documents
        String prefix = sb.toString();
        if (prefix.contains(" ")) {
            return; // only command auto-completion is supported for now
        }
        CommandDescriptor[] cmds = service.getMatchingCommands(prefix);
        if (cmds.length == 0) {
            beep();
        } else if (cmds.length == 1) {
            sb.setLength(0);
            sb.append(cmds[0]);
            updatePrompt();
            System.out.print(cmds[0]);
        } else {
            // show the possible completions
            for (CommandDescriptor cd : cmds) {
                System.out.println(cd.getName());
            }
            updatePrompt();
            System.out.print(prefix);
        }
    }

    static void beep() {
        System.out.print(7);
    }

}
