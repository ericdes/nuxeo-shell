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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * Command to show details information about resgistered components.
 *
 * @author <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
 */
public class ServiceInfoCommand extends AbstractCommand {

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        String[] elements = cmdLine.getParameters();
        if (elements.length > 1) {
            System.err.println(cmdLine.getCommand()
                    + " takes zero or one parameter: the component name");
        }

        RuntimeService runtime = Framework.getRuntime();
        ComponentManager cm = runtime.getComponentManager();

        if (elements.length == 0) {
            List<RegistrationInfo> ris = util.listRegistrationInfos();
            for (RegistrationInfo ri : ris) {
                System.out.print(show(ri, 0));
                System.out.println("-------------------------------------");
            }
        } else {
            String name = elements[0];
            RegistrationInfo ri = cm.getRegistrationInfo(new ComponentName(name));
            System.out.println(show(ri, 1));
        }
    }

    public static String show(RegistrationInfo ri, int verbosity) {
        RuntimeService runtime = Framework.getRuntime();
        ComponentManager cm = runtime.getComponentManager();
        ComponentInstance component = ri.getComponent();
        Object instance = component.getInstance();

        StringBuilder sb = new StringBuilder();

        sb.append("Component Name: ").append(component.getName()).append("\n");
        sb.append("Impl. Class Name: ").append(instance.getClass().getName()).append("\n");
        sb.append("\n");

        String[] serviceNames = component.getProvidedServiceNames();
        if (serviceNames.length > 0) {
            sb.append("Provides:\n");
            for (String serviceName : serviceNames) {
                sb.append("- " + serviceName + "\n");
            }
            sb.append("\n");
        }

        String[] propertyNames = component.getPropertyNames();
        if (propertyNames.length > 0) {
            sb.append("Properties:\n");
            for (String propertyName : propertyNames) {
                sb.append("- " + propertyName + " : "
                        + component.getProperty(propertyName).getValue() + "\n");
            }
            sb.append("\n");
        }

        ExtensionPoint[] extensionPoints = ri.getExtensionPoints();
        if (extensionPoints.length > 0) {
            sb.append("Extension Points:\n");
            for (ExtensionPoint xp : extensionPoints) {
                sb.append("- " + xp.getName() + "\n");
            }
            sb.append("\n");
        }

        if (verbosity > 0) {
            sb.append("documentation:\n");
            sb.append(ri.getDocumentation());
        }

        // More tests need to be added here if more detail() methods are added.
        // Reflection could be used if it becomes unpractical.
        if (instance instanceof TypeService) {
            sb.append(detail((TypeService) instance));
        }

        return sb.toString();
    }

    /**
     * Detailed dump of the TypeService component.
     *
     * Note: this code could be moved to the class being dumped.
     */
    public static String detail(TypeService instance) {
        StringBuilder sb = new StringBuilder();

        SchemaManager sm = instance.getTypeManager();

        DocumentType[] doctypes = sm.getDocumentTypes();
        if (doctypes.length > 0) {
            sb.append("Document Types:\n");
            for (DocumentType doctype : doctypes) {
                sb.append("- " + doctype.getName() + "\n");

                Collection<Schema> schemas = doctype.getSchemas();
                if (schemas.size() > 0) {
                    sb.append("  Schemas: ");
                    for (Schema schema : schemas) {
                        sb.append(schema.getName()).append(" ");
                    }
                    sb.append("\n");
                }

                Set<String> facets = doctype.getFacets();
                if (facets.size() > 0) {
                    sb.append("  Facets: ");
                    for (String facet : facets) {
                        sb.append(facet).append(" ");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        Schema[] schemas = sm.getSchemas();
        if (schemas.length > 0) {
            sb.append("Schemas:\n");
            for (Schema schema : schemas) {
                sb.append("- " + schema.getName() + "\n");
            }
            sb.append("\n");
        }

        Type[] types = sm.getTypes();
        if (types.length > 0) {
            sb.append("Types:\n");
            for (Type type : types) {
                sb.append("- " + type.getName() + "\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

}
