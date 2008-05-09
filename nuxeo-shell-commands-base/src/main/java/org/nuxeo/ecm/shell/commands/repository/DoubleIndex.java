package org.nuxeo.ecm.shell.commands.repository;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.shell.CommandLine;

public class DoubleIndex extends AbstractCommand {

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: DoubleIndex doc_path [nb] ");
        System.out.println(" doc_path path of the doc to index");
        System.out.println(
                " nb number of sync indexing request inside transaction");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String docPath = null;
        int nb = 2;
        int batchSize;
        String[] elements = cmdLine.getParameters();

        if (elements.length >= 1) {
            if ("help".equals(elements[0])) {
                printHelp();
                return;
            }
            docPath = elements[0];
        }

        if (elements.length >= 2) {
            try {
                nb = Integer.parseInt(elements[1]);
            } catch (Throwable t) {
                System.err.println("Failed to parse nb");
                printHelp();
                return;
            }
        } else {
            printHelp();
            return;
        }

        index(docPath, nb);
    }

    public void index(String path, int nb) throws Exception {
        DocumentModel dm = context.getRepositoryInstance().getDocument(
                new PathRef(path));
        String title = (String) dm.getProperty("dublincode", "title");
        for (int i = 0; i < nb - 1; i++) {
            dm.setProperty("dublincore", "title", title + i);
            context.getRepositoryInstance().saveDocument(dm);
        }

        dm.setProperty("dublincore", "title", title);
        context.getRepositoryInstance().saveDocument(dm);
        context.getRepositoryInstance().save();
    }

}
