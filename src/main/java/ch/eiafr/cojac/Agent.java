/*
 * *
 *    Copyright 2011 Baptiste Wicht & Frédéric Bapst
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package ch.eiafr.cojac;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public final class Agent implements ClassFileTransformer {
    private final CojacReferences references;
    private final ClassInstrumenter instrumenter;

    public Agent(final CojacReferences references) {
        try {
            this.references = references;
            this.instrumenter = new ClassLoaderInstrumenter(references); 
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (!references.hasToBeInstrumented(className)) {
                return classfileBuffer;
            }
            if (references.getArgs().isSpecified(Arg.VERBOSE))
                System.out.println("Agent instrumenting "+className +" under "+loader);
            return instrumenter.instrument(classfileBuffer);
        } catch (RuntimeException e) {
            e.printStackTrace();  // Otherwise it'll be hidden!
            throw e;
        }
    }
}
