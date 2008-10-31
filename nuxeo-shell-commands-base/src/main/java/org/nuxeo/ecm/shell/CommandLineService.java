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
import java.util.*;

import org.jboss.remoting.CannotConnectException;
import org.nuxeo.ecm.core.client.DefaultLoginHandler;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandLineService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.runtime.client.CommandLineService");

    private Map<String, CommandDescriptor> cmds;
    private Map<String, CommandOption> options;
    private Map<String, CommandOption> shortcuts;

    CommandContext commandContext;


    @Override
    public void activate(ComponentContext context) throws Exception {
        cmds = new Hashtable<String, CommandDescriptor>();
        options = new Hashtable<String, CommandOption>();
        shortcuts = new Hashtable<String, CommandOption>();
        commandContext = new CommandContext();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        cmds.clear();
        options.clear();
        shortcuts.clear();
        cmds = null;
        options = null;
        shortcuts = null;
        commandContext = null;
        //System.out.println("Exiting");
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        super.registerExtension(extension);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("commands")) {
            CommandDescriptor cmd = (CommandDescriptor)contribution;
            cmds.put(cmd.name, cmd);
            CommandOption[] opts = cmd.options;
            if (opts != null) { // register local options
                for (CommandOption opt : opts) {
                    opt.setCommand(cmd.name);
                    addCommandOption(opt);
                }
            }
        } else if (extensionPoint.equals("options")) {
            CommandOption arg = (CommandOption)contribution;
            addCommandOption(arg);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("commands")) {
            CommandDescriptor cmd = (CommandDescriptor)contribution;
            cmds.remove(cmd.name);
            CommandOption[] opts = cmd.options;
            if (opts != null) { // unregister local options
                for (CommandOption opt : opts) {
                    removeCommandOption(opt);
                }
            }
        } else if (extensionPoint.equals("options")) {
            CommandOption arg = (CommandOption)contribution;
            removeCommandOption(arg);
        }
    }

    public CommandDescriptor getCommand(String name) {
        return cmds.get(name);
    }

    public void addCommand(CommandDescriptor cmd) {
        cmds.put(cmd.name, cmd);
    }

    public void removeCommand(String name) {
        cmds.remove(name);
    }

    public CommandDescriptor[] getCommands() {
        return cmds.values().toArray(new CommandDescriptor[cmds.size()]);
    }

    public String[] getCommandNames() {
        return cmds.keySet().toArray(new String[cmds.size()]);
    }

    public CommandDescriptor[] getSortedCommands() {
        CommandDescriptor[] cmds = getCommands();
        Arrays.sort(cmds);
        return cmds;
    }

    public CommandDescriptor[] getMatchingCommands(String prefix) {
        List<CommandDescriptor> result = new ArrayList<CommandDescriptor>();
        for (CommandDescriptor cmd : cmds.values()) {
            if (cmd.name.startsWith(prefix)) {
                result.add(cmd);
            }
        }
        CommandDescriptor[] cmds = result.toArray(new CommandDescriptor[result.size()]);
        Arrays.sort(cmds);
        return cmds;
    }

    public CommandOption getCommandOption(String name) {
        return options.get(name);
    }

    public CommandOption[] getCommandOptions() {
        return options.values().toArray(new CommandOption[options.size()]);
    }

    public void addCommandOption(CommandOption arg) {
        options.put(arg.name, arg);
        if (arg.shortcut != null) {
            shortcuts.put(arg.shortcut, arg);
        }
    }

    public void removeCommandOption(CommandOption arg) {
        options.remove(arg.name);
        if (arg.shortcut != null) {
            shortcuts.remove(arg.shortcut);
        }
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    public CommandLine parse(String[] args, boolean validate) throws ParseException {
        Queue<CommandOption> queue = new LinkedList<CommandOption>();
        CommandLine cmdLine = new CommandLine(this);
        if (args.length == 0) {
            return cmdLine;
        }
        cmdLine.addCommand(args[0]);
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            CommandOption opt;
            if (arg.startsWith("-")) {
                if (arg.startsWith("-", 1)) {
                    arg = arg.substring(2);
                    opt = options.get(arg);
                    if (opt == null) {
                        if (validate) {
                            throw new ParseException("Option is not recognized: "+arg, 0);
                        } else {
                            continue;
                        }
                    }
                    cmdLine.addOption(opt.name);
                    if (!opt.isFlag()) {
                        queue.add(opt);
                    }
                } else {
                    arg = arg.substring(1);
                    char[] chars = arg.toCharArray();
                    for (char c : chars) {
                        opt = shortcuts.get(String.valueOf(c));
                        if (opt == null) {
                            if (validate) {
                                throw new ParseException("Option is not recognized: "+c, 0);
                            } else {
                                continue; // ignore this option
                            }
                        }
                        cmdLine.addOption(opt.name);
                        if (!opt.isFlag()) {
                            queue.add(opt);
                        }
                    }
                }
            } else {
                opt = queue.poll();
                if (opt != null) {
                    cmdLine.addOptionValue(opt.name, arg);
                } else {
                    cmdLine.addParameter(arg);
                }
            }
        }

        // validate cmd args

        if (validate) {
            if (!queue.isEmpty()) {
                System.out.println("Syntax error. The following options had no values:");
                while (!queue.isEmpty()) {
                    System.out.println(" * " + queue.poll());
                }
            }

//          // first check all required options
//          for (CommandOption op :  this.options.values()) {
//              if (op.isRequired && !cmdLine.containsKey(op.name)) {
//                  System.out.println("Required option is missing: "+op.name);
//                  System.exit(2);
//              }
//          }

        } else { // when no validting we need to insert all pending noption into the command line (needed for autcompletion to work)
            if (!queue.isEmpty()) {
                while (!queue.isEmpty()) {
                    CommandOption opt = queue.poll();
                    cmdLine.addOption(opt.name);
                }
            }
        }

        return cmdLine;

    }

    public void initalizeConnection() throws Exception {
        NuxeoClient client = NuxeoClient.getInstance();
        int port = commandContext.getPort();
        String username = commandContext.getUsername();
        String password = commandContext.getPassword();
        // try connecting to all candidate hosts
        Exception exc = null;
        for (String host : commandContext.getCandidateHosts()) {
            if (username != null && !"system".equals(username)) {
                client.setLoginHandler(new DefaultLoginHandler(username, password));
            }
            try {
                System.out.println("Trying to connect to nuxeo server at " +
                        host + ':' + port + " as " +
                        (username == null ? "system user" : username) + "...");
                client.connect(host, port);
                commandContext.setHost(host);
                break;
            } catch (CannotConnectException e) {
                exc = e;
                continue; // try next host
            }
        }
        if (commandContext.getHost() == null) {
            throw new RuntimeException("Could not connect to server", exc);
        }
        System.out.println("Connection established");
    }

    public void runCommand(CommandDescriptor cd, CommandLine cmdLine) throws Exception {
        Command command = (Command)cd.klass.newInstance();
        NuxeoClient client = NuxeoClient.getInstance();
        if (cd.requireConnection && !client.isConnected()) {
            initalizeConnection();
            command.run(cmdLine);
        } else {
            command.run(cmdLine);
        }
    }


}
