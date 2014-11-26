/**
 * Written by Fedor Burdun of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Fedor Burdun
 */
package org.iohiccup;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class IOHiccupPremain {
    
    
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        commonmain(agentArgument, instrumentation);

        try {
            IOHiccup.premain0(agentArgument, instrumentation);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        //instrumentation.addTransformer(new IOHiccupTransformer(IOHiccup.premain0(agentArgument, instrumentation)));
    }

    private static void commonmain(String arguments, Instrumentation instrumentation) {
        // Exclude CLI option Xbootclasspath
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(
                    new JarFile(IOHiccupPremain.class.getProtectionDomain().
                            getCodeSource().getLocation().getPath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        
        commonmain(agentArgument, instrumentation);
        
        try {
            IOHiccupAttachable.premain0(agentArgument, instrumentation);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        //TODO: Exclude CLI option Xbootclasspath        
        
        boolean needHelp = false;
        String pid = null;
        String agentArguments = "";
        
        for (String s : args) {
            if (s.startsWith("-pid")) {
                String[] p = s.split("=");
                if (p.length==2) {
                    pid = p[1];
                } else {
                    needHelp = true;
                }
            } else if (s.startsWith("-agentargs")) {
                String[] p = s.split("=", 2);
                if (p.length==2) {
                    agentArguments = p[1];
                } else {
                    needHelp = true;
                }
            } else if (s.startsWith("-h") || s.startsWith("--help") || s.startsWith("-help")) {
                needHelp = true;
            } else {
                needHelp = true;
            }
        }
        
        //validate agent arguments
        //print help message and exit if something is wrong
        {
            (new IOHiccup()).parseArguments(agentArguments);
        }
        
        if (needHelp || null == pid) {
            System.err.println("please, to attach ioHiccup to already running application rerun it in next manner:\n\n"
                    + "\tjava -jar ioHiccup.jar -pid=<PID of java VM> -agentargs='<args>' \n\n");
            IOHiccup.printHelpParameters();
            System.exit(1);
        }
        
        try {
            
            VirtualMachine vm = VirtualMachine.attach(pid);
            
            vm.loadAgent(IOHiccupPremain.class.getProtectionDomain().
                    
                            getCodeSource().getLocation().getPath(), agentArguments);
            vm.detach();
            System.exit(0);
        
        } catch (IOException e) {
            System.err.println("Seems like java process with pid="+pid+" doesn't exist or not permit to instrument. \nPlease ensure that pid is correct.");
        } catch (AgentInitializationException e) {
            System.err.println("Failed to initialize agent: " + e);
        } catch (AgentLoadException e) {
            System.err.println("Failed to load agent: " + e);
        } catch (AttachNotSupportedException e) {
            System.err.println("Seems like attach isn't supported: " + e);
        }
    }
}
