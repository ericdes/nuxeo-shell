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

package org.nuxeo.ecm.shell.commands.repository;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.ColorHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ListCommand extends AbstractCommand {

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        RepositoryInstance repo = context.getRepositoryInstance();
        DocumentRef docRef = context.getCurrentDocument();
        long t0 = System.currentTimeMillis();
        DocumentModelList docs = repo.getChildren(docRef);


        for (DocumentModel doc : docs) {
            System.out.println(ColorHelper.decorateName(doc, doc.getName()));
        }

        System.out.println(docs.size() + " docs listed in " + (System.currentTimeMillis()-t0) + "ms");
    }

}
