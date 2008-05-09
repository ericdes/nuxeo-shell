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
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandDescriptor;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.CommandLineService;
import org.nuxeo.ecm.shell.CommandOption;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class HelpCommand implements Command {

    private CommandLineService service;

    public void run(CommandLine cmdLine) throws Exception {
        service = Framework.getService(CommandLineService.class);
        String[] elements = cmdLine.getParameters();
        if (elements.length == 0) {
            printGlobalHelp(System.out);
        } else {
            printCommandHelp(elements[0], System.out);
        }
    }

    void printHelp(PrintStream out, String help, String ident) {
        for (String line: trimHelpLines(help)) {
            out.println(ident + line);
        }
    }

    public void printGlobalHelp(PrintStream out) {
        out.println("Syntax: application command [options]");
        out.println();
        out.println("The following commands are supported:");
        for (CommandDescriptor cd : service.getSortedCommands()) {
            out.println(" * " + cd.name + " - "
                    + (cd.description == null ? "N/A" : cd.description));
        }
        out.println();
        out.println("Global options: ");
        for (CommandOption opt : service.getCommandOptions()) {
            if (opt.getCommand() == null) { // ignore local options
                String help = opt.getHelp();
                if (help !=null && help.length() > 0) {
                    String msg = "  --" + opt.getName();
                    if (opt.getShortcut() != null) {
                        msg += " [shortcut: -" + opt.getShortcut() + ']';
                    }
                    out.println(msg);
                    printHelp(out, help, "      ");
                }
            }
        }
        out.println();
        out.println("For more information on a command run \"applicaton help command\".");
        out.println("For auto-completion press TAB key.");
        out.println();
    }

    public void printCommandHelp(String cmd, PrintStream out) {
        CommandDescriptor cd = service.getCommand(cmd);
        if (cd == null) {
            System.err.println("Unknown command: "+cmd);
            return;
        }
        // header
        out.println("Command: "+cd.name+" - "+cd.description);
        out.println();
        // aliases
        out.print("Aliases: ");
        if (cd.aliases == null || cd.aliases.length == 0) {
            out.println("N/A");
        } else {
           out.println(StringUtils.join(cd.aliases, ", "));
        }
        out.println();
        // options
        out.println("Options: ");
        if (cd.options == null || cd.options.length == 0) {
            out.println("N/A");
        } else {
            for (CommandOption opt : cd.options) {
                String help = opt.getHelp();
                if (help !=null && help.length() > 0) {
                    String msg = "  --" + opt.getName();
                    if (opt.getShortcut() != null) {
                        msg += " [shortcut: -" + opt.getShortcut() + ']';
                    }
                    out.println(msg);
                    printHelp(out, help, "      ");
                }
            }
        }
        out.println();
        // help content
        if (cd.help == null || cd.help.length() == 0) {
          out.println("Help not available");
        } else {
            printHelp(out, cd.help, "");
        }
        out.println();
    }

    public static String[] trimHelpLines(String help) {
        String[] rawLines = help.split("\n|(\r\n)|\r");
        List<String> lines = new ArrayList<String>();

        //trim empty lines
        int start = 0;
        for (int i=0; i<rawLines.length; i++) {
            start = i;
            String line = rawLines[i].trim();
            if (line.length() > 0) {
                break;
            }
        }
        int end = 0;
        for (int i=rawLines.length-1; i>=0; i--) {
            end = i;
            String line = rawLines[i].trim();
            if (line.length() > 0) {
                break;
            }
        }

        //trim lines content and add to list
        for (int i=start; i<=end; i++) {
            lines.add(rawLines[i].trim());
        }

        return lines.toArray(new String[lines.size()]);
    }

}
