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

package org.nuxeo.ecm.shell;

import java.text.ParseException;

import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.core.client.RepositoryInstance;
import org.nuxeo.runtime.api.Framework;

/**
 * Should be used with the nuxeo launcher
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    private Main() {
    }


    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage java <classname> <cmd> [options] ");
            System.exit(1);
        }

        CommandLine cmdLine;
        CommandLineService service = Framework.getRuntime().getService(
                CommandLineService.class);// (CommandLineService.NAME);
        try {
            cmdLine = service.parse(args, true);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        String cmdName = cmdLine.getCommand();
        if (cmdName == null) {
            cmdName = "help";
            cmdLine.addCommand(cmdName);
        }

        CommandContext cmdContext = service.getCommandContext();
        cmdContext.setCommandLine(cmdLine);

        String host = cmdLine.getOption(Options.HOST);
        String port = cmdLine.getOption(Options.PORT);

        // try to find the good @IP
        host= IPHelper.findConnectIP(host, port);

        // this logic would be duplicated in a "connect" command
        cmdContext.setHost(host);
        cmdContext.setPort(port == null ? 0 : Integer.parseInt(port));
        cmdContext.setUsername(cmdLine.getOption(Options.USERNAME));
        cmdContext.setPassword(cmdLine.getOption(Options.PASSWORD));

        CommandDescriptor cd = service.getCommand(cmdName);

        if (cd == null) {
            System.out.println("No such command was registered:  " + cmdName);
            System.exit(1);
        }

        int rcmds = args.length - 1;
        String[] newArgs;
        if (rcmds > 0) {
            newArgs = new String[rcmds];
            System.arraycopy(args, 1, newArgs, 0, rcmds);
        } else {
            newArgs = new String[0];
        }

        try {
            service.runCommand(cd, cmdLine);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            try {
                if (cmdContext.isCurrentRepositorySet()) {
                    RepositoryInstance repo = cmdContext.getRepositoryInstance();
                    if (repo != null) {
                        repo.close();
                    }
                }
                NuxeoClient.getInstance().tryDisconnect();
                System.out.println("Bye.");
            } catch (Exception e) {
                System.err.println("Failed to Disconnect.");
                e.printStackTrace();
            }
        }
    }

}
