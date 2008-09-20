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
package org.nuxeo.ecm.shell.commands.system;

import java.util.List;

import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
 *
 */
public class ServicesCommand extends AbstractCommand {

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        RuntimeService runtime = Framework.getRuntime();
        System.out.println(runtime.toString());

        List<RegistrationInfo> ris = util.listRegistrationInfos();

        System.out.println("Registered components (+ state)");
        for (RegistrationInfo ri : ris) {
            System.out.println(ri.getName() + ": " + ri.getState());
        }
    }

}

