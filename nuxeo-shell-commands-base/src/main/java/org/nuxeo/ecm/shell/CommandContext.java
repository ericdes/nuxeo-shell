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

import java.util.Collection;
import java.util.HashMap;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.core.client.RepositoryInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandContext extends HashMap<String, Object> {

    private static final long serialVersionUID = 921391738618179230L;

    private boolean interactive = false;
    private DocumentRef docRef;
    private CommandLine cmdLine;
    private RepositoryInstance repository;

    private Collection<String> candidateHosts;

    private String host;

    private int port;

    private String username;

    private String password;

    public Collection<String> getCandidateHosts() {
        return candidateHosts;
    }

    public void setCandidateHosts(Collection<String> candidateHosts) {
        this.candidateHosts = candidateHosts;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean value) {
        interactive = value;
    }

    public boolean isCurrentDocumentSet() {
        return docRef != null;
    }

    public boolean isCurrentRepositorySet() {
        return repository != null;
    }

    public DocumentRef getCurrentDocument() throws Exception {
        if (docRef == null) {
            docRef = getRepositoryInstance().getRootDocument().getRef();
        }
        return docRef;
    }

    public void setCurrentDocument(DocumentRef docRef) {
        this.docRef = docRef;
    }

    public void setCurrentDocument(DocumentModel doc) {
        docRef = doc.getRef();
    }

    public CommandLine getCommandLine() {
        return cmdLine;
    }

    public void setCommandLine(CommandLine cmdLine) {
        this.cmdLine = cmdLine;
    }

    public void setRepositoryInstance(RepositoryInstance repository) {
        this.repository = repository;
    }

    public DocumentModel fetchDocument() throws Exception {
        return getRepositoryInstance().getDocument(getCurrentDocument());
    }

    public DocumentModel fetchDocument(Path path) throws Exception {
        return getDocumentByPath(getCurrentDocument(), path);
    }

    public RepositoryInstance getRepositoryInstance() throws Exception {
        if (repository == null) {
            String repoName = cmdLine.getOption("repository");
            if (repoName == null) {
                repository = NuxeoClient.getInstance().openRepository();
            } else {
                repository = NuxeoClient.getInstance().openRepository(repoName);
            }
        }
        return repository;
    }

    public DocumentModel getDocumentByPath(DocumentRef base, Path path) throws Exception {
        RepositoryInstance repo = getRepositoryInstance();
        if (!path.isAbsolute()) {
            DocumentModel doc = repo.getDocument(base);
            path = doc.getPath().append(path);
            path = new Path(path.toString());
        }
        return repo.getDocument(new PathRef(path.toString()));
    }

}
