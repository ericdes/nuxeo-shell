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

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * A command descriptor. This describe command arguments and help.
 * <p>
 * A command descriptor may be lazy this means when you need to be sure command definition is loaded
 * we must call {@link #load()} before accessing command definition.
 * <p>
 * When instantiating a command using {@link #newInstance()} the definition
 * will be automatically loaded if needed.
 * <p>
 * Laziness avoid compiling script commands at startup.
 *
 * TODO: support for "bundle:" scripts
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("command")
public class DefaultCommandDescriptor implements CommandDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<?> klass;

    protected String[] aliases;

    @XNode("description")
    protected String description = "N/A";

    @XNode("help")
    protected String help = "N/A";

    @XNodeList(value="options/option", type=CommandOption[].class,
            componentType=CommandOption.class)
    public CommandOption[] options;

    @XNodeList(value="params/param", type=CommandParameter[].class,
            componentType=CommandParameter.class)
    public CommandParameter[] params;


    /**
     *
     */
    public DefaultCommandDescriptor() {
        // TODO Auto-generated constructor stub
    }

    public DefaultCommandDescriptor(String name, Class<?> klass) {
        this.name = name;
        this.klass = klass;
    }


    public boolean isDynamicScript() {
        return false;
    }

    public boolean hasOptions() {
        return options != null && options.length > 0;
    }

    public boolean hasArguments() {
        return params != null && params.length > 0;
    }

    @Override
    public String toString() {
        return name + " [" + klass + ']';
    }

    /**
     * Set alternate names
     */
    @XNode("@alt")
    void setAlt(String alt) {
        aliases = StringUtils.split(alt, ',', true);
    }

    /**
     * @return the aliases.
     */
    public String[] getAliases() {
        return aliases;
    }

    /**
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the help.
     */
    public String getHelp() {
        return help;
    }

    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the options.
     */
    public CommandOption[] getOptions() {
        return options;
    }

    /**
     * @return the params.
     */
    public CommandParameter[] getArguments() {
        return params;
    }

    /**
     * @param params the params to set.
     */
    public void setParams(CommandParameter[] params) {
        this.params = params;
    }

    /**
     * @param help the help to set.
     */
    public void setHelp(String help) {
        this.help = help;
    }

    /**
     * @param description the description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param aliases the aliases to set.
     */
    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    /**
     * @param options the options to set.
     */
    public void setOptions(CommandOption[] options) {
        this.options = options;
    }

    public int compareTo(CommandDescriptor o) {
        return name.compareTo(o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DefaultCommandDescriptor)) {
            return false;
        }

        DefaultCommandDescriptor that = (DefaultCommandDescriptor) o;
        return that.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public Command newInstance() throws Exception {
        if (klass != null) {
            return (Command)klass.newInstance();
        } else {
            throw new IllegalStateException("Command implementation not defined : "+name);
        }
    }

}
