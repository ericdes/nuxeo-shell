package org.nuxeo.ecm.shell.commands.repository;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.shell.CommandLine;

public class MkDirCommand extends AbstractCommand {

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        if (elements.length == 0) {
            System.out.println(
                    "SYNTAX ERROR: the mkdir command must take at least one argument: mkdir path_or_name [type]");
            return;
        }
        DocumentModel parent = null;
        String dirName = null;
        if (elements.length >= 1) {
            Path path = new Path(elements[0]);
            path = path.removeTrailingSeparator();

            if (path.isAbsolute()) {
                // abs path
                Path parentPath = path.removeLastSegments(1);
                parent = context.fetchDocument(parentPath);
                dirName = path.lastSegment();
            } else {
                // relative path
                DocumentRef currentDocRef = context.getCurrentDocument();
                parent = context.getRepositoryInstance().getSession().getDocument(
                        currentDocRef);
                dirName = path.toString();
            }
        }
        String dirType;
        if (elements.length == 2) {
            dirType = elements[1];
        } else {
            dirType = "Folder";
        }

        createDir(parent, dirName, dirType);
    }

    private DocumentModel createDir(DocumentModel parent, String name,
            String docType) throws Exception {
        CoreSession session = context.getRepositoryInstance().getSession();
        DocumentModel doc = session.createDocumentModel(docType);
        doc.setPathInfo(parent.getPathAsString(), name);
        doc.setProperty("dublincore", "title", name);

        doc = session.createDocument(doc);
        session.save();
        return doc;
    }

}
