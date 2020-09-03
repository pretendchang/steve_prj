package com.steve.test.prototype1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.steve.util.ResourceFileReader;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class javabeantry {
	public static void main(final String[] args) throws Exception {
        //final ScriptEngine e = new NashornScriptEngineFactory().getScriptEngine();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine e = manager.getEngineByName("JavaScript");
        // Get original load function
        final JSObject loadFn = (JSObject)e.get("load");
        // Get global. Not really necessary as we could use null too, just for
        // completeness.
        final JSObject thiz = (JSObject)e.eval("(function() { return this; })()");

        // Define a new "load" function
        final Function<Object, Object> newLoad = (source) -> {
            if (source instanceof String) {
                final String strSource = (String)source;
                if (strSource.startsWith("myurlscheme:")) {
                    // handle "myurlscheme:"
                    return loadFn.call(thiz, createCustomSource(strSource));
                }
                else if (strSource.startsWith("cmpdata:")) {
                    // handle "cmpdata:"
                	String []ss = strSource.split(":");
                    return loadFn.call(thiz, loadCmpdataInclude(ss[1]));
                }
            }
            // Fall back to original load for everything else
            return loadFn.call(thiz, source);
        };

        // Replace built-in load with our load
        e.put("load", newLoad);
        // Load a dynamically generated script
        e.eval("load('myurlscheme:boo')");
        e.eval("load('cmpdata:include.js')");
        e.eval("load('nashorn:mozilla_compat.js')");
    }

    public static Object createCustomSource(final String source) {
        final Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("name", source);
        sourceMap.put("script", "print('Hello from " + source + "');");
        return sourceMap;
    }
    
    public static Object loadCmpdataInclude(final String source)
    {
    	String str = ResourceFileReader.readResourceFile("com.cmpdata.scripting.CheckScript",source);
		
		final Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("name", "cmpdatainclude");
        sourceMap.put("script", str);
        return sourceMap;
    }
    
}
