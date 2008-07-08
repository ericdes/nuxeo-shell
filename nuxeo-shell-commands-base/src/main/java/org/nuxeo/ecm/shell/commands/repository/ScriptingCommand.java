/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.beans.SimpleBeanInfo;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.nuxeo.ecm.shell.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptingCommand extends AbstractCommand {

    protected static ScriptEngineManager scriptMgr = new ScriptEngineManager();


    protected CompiledScript script;
    protected File file;
    protected long lastModified = 0;

    public ScriptingCommand(File file) throws ScriptException {
        ScriptEngine engine = scriptMgr.getEngineByExtension(getExtension(file));
        this.script = compileScript(engine, file);
        this.lastModified = file.lastModified();
        this.file = file;
    }

    public void run(CommandLine cmdLine) throws Exception {
        if (file.lastModified() > lastModified) {
            ScriptEngine engine = scriptMgr.getEngineByExtension(getExtension(file));
            this.script = compileScript(engine, file);
        }
        Bindings ctx = new SimpleBindings();
        ctx.put("cmd", cmdLine);
        ctx.put("ctx", context);
        ctx.put("client", client);
        ctx.put("service", cmdService);
        try {
            script.eval(ctx);
        } finally {
            System.out.flush();
        }
    }

    public static CompiledScript compileScript(ScriptEngine engine, File file) throws ScriptException {
        if (engine instanceof Compilable) {
            Compilable comp = (Compilable)engine;
            try {
                Reader reader = new FileReader(file);
                try {
                    return comp.compile(reader);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } else {
            return null;
        }
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p > -1) {
            return name.substring(p+1);
        }
        return "";
    }

}
