package org.nuxeo.ecm.shell.commands.repository;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.runtime.api.Framework;

public class AuditSync extends AbstractCommand {

    private void printHelp() {
        System.out.println("");
        System.out.println("Synthax: auditsync remote_path [clientResursionLevel] ");
        System.out.println(" remote_path : document path from where audit sync must be launched");
        System.out.println(" subTrans (optionnal, default=0): defines what part of the recursion is done on the client side");
        System.out.println("  (usefull for big DB to avoid EJB3 socket timeout)");
    }

    private Logs auditService;

    private long t0;

    @Override
    public void run(CommandLine cmdLine) throws Exception {

        String[] elements = cmdLine.getParameters();

        if (elements.length == 0) {
            System.out.println(
                    "SYNTAX ERROR: the audit command must take at least one argument: auditsync remote_path");
            printHelp();
            return;
        }
        if ("help".equals(elements[0])) {
            printHelp();
            return;
        }
        Path path = new Path(elements[0]);
        DocumentModel root;
        try {
            root = context.fetchDocument(path);
        } catch (Exception e) {
            System.err.println("Failed to retrieve the given folder");
            return;
        }

        Integer clientRecurseLevel = 0;
        if (elements.length >= 2) {
            try {
                clientRecurseLevel = Integer.parseInt(elements[1]);
            } catch (Throwable t) {
                System.err.println("Failed to parse clientRecurseLevel parameter");
                return;
            }
        }

        auditService = Framework.getService(Logs.class);

        t0 = System.currentTimeMillis();
        long nbEntries;
        if (clientRecurseLevel == 0) {
            // full server side recusion
            nbEntries = auditService.syncLogCreationEntries(
                    context.getRepositoryInstance().getRepositoryName(),
                    root.getPathAsString(), true);
        } else {
            // client side partial recursion
            nbEntries = recurseSyncLog(
                    context.getRepositoryInstance().getRepositoryName(), root,
                    0, clientRecurseLevel);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(nbEntries + " audit entries synched in " + (t1 - t0) + "ms");
        System.out.println(1000 * (float) nbEntries / (t1 - t0) + " doc/s");
    }

    private long recurseSyncLog(String repo, DocumentModel root, int level,
            int maxLevel) throws Exception {

        if (level >= maxLevel) {
            long ti1 = System.currentTimeMillis();
            long nbEntries = auditService.syncLogCreationEntries(repo,
                    root.getPathAsString(), true);
            long ti2 = System.currentTimeMillis();
            System.out.println("Level " + level + " : " + nbEntries
                    + " audit entries synched in " + (ti2 - ti1) + "ms");
            return nbEntries;
        } else {
            long nbEntries = auditService.syncLogCreationEntries(repo,
                    root.getPathAsString(), false);

            for (DocumentModel child : context.getRepositoryInstance().getChildren(
                    root.getRef())) {
                if (child.isFolder()) {
                    nbEntries += recurseSyncLog(repo, child, level + 1,
                            maxLevel);
                }
            }
            return nbEntries;
        }
    }

}
