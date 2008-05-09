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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("command")
public class CommandDescriptor implements Comparable<CommandDescriptor> {

    @XNode("@name")
    public String name;

    @XNode("@class")
    public Class<?> klass;

    @XNode("@requireConnection")
    public boolean requireConnection = true;

    @XNode("@interactive")
    public boolean isInteractive = false;

    public String[] aliases;

    @XNode("description")
    public String description;

    @XNode("help")
    public String help;

    @XNodeList(value="options/option", type=CommandOption[].class,
            componentType=CommandOption.class)
    public CommandOption[] options;

    @XNodeList(value="params/param", type=CommandParameter[].class,
            componentType=CommandParameter.class)
    public CommandParameter[] params;

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

    public int compareTo(CommandDescriptor o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof CommandDescriptor)) {
            return false;
        }

        CommandDescriptor that = (CommandDescriptor) o;
        return that.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
