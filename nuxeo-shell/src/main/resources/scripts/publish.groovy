/*

Publish the current document into the target section

publish|pub [section:doc]


This command publish the current document into the given section. 
Relative or absolute paths may be used to specify the target section.
*/

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.common.utils.*;

def params = cmdLine.getParameters();
if (params.length != 2) {
  out.println("Syntax Error: the publish command take exactly one parameter - the target section path");
  return;
}

String docPath = params[0];
String sectionPath = params[1];
doc = ctx.fetchDocument();

out.println("Publishing ${doc.getPathAsString()} into ${sectionPath}");

if (!doc.hasFacet("Versionable")) {
  out.println("Error: Cannot publish the document. It is not Versionable");
  return;  
}

DocumentModel section = session.getDocument(new PathRef(sectionPath));
if (!"Section".equals(section.getType())) {
  out.println("Error: Cannot publish the document. The destination is not a section");
  return;  
}

session.publishDocument(doc, section);
out.println("Done");
