package org.nuxeo.ecm.shell.commands.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.RegistrationInfo;

public class util {

    private util() {}

    public static List<RegistrationInfo> listRegistrationInfos() {
        RuntimeService runtime = Framework.getRuntime();
        ComponentManager cm = runtime.getComponentManager();
        List<RegistrationInfo> ris = new ArrayList<RegistrationInfo>(
                cm.getRegistrations());
        Collections.sort(ris, new Comparator<RegistrationInfo>() {
            public int compare(RegistrationInfo o1, RegistrationInfo o2) {
                return o1.getName().toString().compareTo(o2.getName().toString());
            }
        });
        return ris;
    }

}
