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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;



/**
 * Ex:
 * cp [--recurse|-r] [--depth|-d:int?1] [--version|-v] file [doc]
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandPattern {

    public String name;
    public List<Option> options = new ArrayList<Option>();
    public List<Argument> args = new ArrayList<Argument>();

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(name).append(" ");
        for (Option opt : options) {
            buf.append(opt.toString()).append(" ");
        }
        for (Argument arg : args) {
            buf.append(arg.toString()).append(" ");
        }
        return buf.toString();
    }

    public static class Argument {
        String type;
        boolean isOptional;
        @Override
        public String toString() {
            return isOptional ? "[" +type+"]" : type;
        }
    }

    public static class Option {
        public String[] names;
        public String type;
        public boolean isRequired;
        public String defaultValue;
        @Override
        public String toString() {
            String str = StringUtils.join(names, '|');
            if (type != null) {
                str += ":" + type;
            }
            if (defaultValue != null) {
                str += "?" + defaultValue;
            }
            return !isRequired ? "[" +str+"]" : str;
        }
    }

    public static CommandPattern parsePattern(String input) throws ParseException {
        CommandPattern cmd = new CommandPattern();
        int len = input.length();
        StringBuilder buf = new StringBuilder();
        Option opt = null;
        Argument arg = null;
        for (int i=0; i<len; i++) {
            char c = input.charAt(i);
            switch (c) {
            case ' ':
            case '\t':
                continue;
            case '[':
                buf.setLength(0);
                i = collectUntil(']', buf, input, i+1);
                if (i >= len) {
                    throw new ParseException("no matching ']' found", i);
                }
                if (buf.length() == 0) {
                    throw new ParseException("Emtpy options [] found", i);
                }
                if (buf.charAt(0) == '-') {
                    opt = parseOption(buf.toString());
                    opt.isRequired = false;
                    cmd.options.add(opt);
                } else {
                    arg = new Argument();
                    arg.type = buf.toString();
                    arg.isOptional = true;
                    cmd.args.add(arg);
                }
                break;
            case '-':
                i = collectUntil(' ', buf, input, i);
                opt = parseOption(buf.toString());
                opt.isRequired = true;
                cmd.options.add(opt);
                break;
            default:
                i = collectUntil(' ', buf, input, i);
                if (cmd.name == null) {
                    cmd.name = buf.toString();
                } else {
                    arg = new Argument();
                    arg.type = buf.toString();
                    cmd.args.add(arg);
                }
                break;
            }
            buf.setLength(0);
        }
        return cmd;
    }

    public static int collectUntil(char sep, StringBuilder buf, String input, int offset) {
        char c;
        int len = input.length();
        for (int i=offset; i<len; i++) {
            c = input.charAt(i);
            if (c == sep) {
                return i;
            }
            buf.append(c);
        }
        return len;
    }

    private static Option parseOption(String input) {
        Option opt = new Option();
        int p = input.indexOf(':');
        if (p > -1) {
            opt.names = StringUtils.split(input.substring(0, p), '|', true);
            opt.type = input.substring(p+1);
            p = opt.type.indexOf('?');
            if (p > -1) {
                opt.defaultValue = opt.type.substring(p+1);
                opt.type = opt.type.substring(0, p);
            }
        } else {
            opt.names = StringUtils.split(input, '|', true);
        }
        return opt;
    }


    public static void main(String[] args) throws Exception {
        String input = "cp [--recurse|-r] [--depth|-d:int?1] [--version|-v] file [doc]";
        System.out.println(input);
        CommandPattern cmd = parsePattern(input);
        System.out.println(cmd);
    }

}
