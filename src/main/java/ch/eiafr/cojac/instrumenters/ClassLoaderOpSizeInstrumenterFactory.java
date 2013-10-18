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

package ch.eiafr.cojac.instrumenters;

import java.util.HashSet;
import java.util.Set;

import ch.eiafr.cojac.Arg;
import ch.eiafr.cojac.Args;
import ch.eiafr.cojac.InstrumentationStats;
import static org.objectweb.asm.Opcodes.*;

public final class ClassLoaderOpSizeInstrumenterFactory implements OpCodeInstrumenterFactory {
    private final OpCodeInstrumenter opCodeInstrumenter;

    private final Args args;
    
    private static final Set<Integer> replaceFloatsOpcodes = new HashSet<>();
    static {
        int [] t = {FRETURN, FCONST_0, FCONST_1, FCONST_2, I2F, L2F, D2F};
        for(int e:t) {
            replaceFloatsOpcodes.add(e);
        }
    }

    public ClassLoaderOpSizeInstrumenterFactory(Args args, InstrumentationStats stats) {
        super();

        this.args = args;
        if (args.isSpecified(Arg.REPLACE_FLOATS))
            opCodeInstrumenter = new ReplaceFloatsInstrumenter(args, stats);
        else
            opCodeInstrumenter = new DirectInstrumenter(args, stats);

    }

    @Override
    public OpCodeInstrumenter getInstrumenter(int opCode, Arg arg) {
        // TODO: a bit tricky...
        if (args.isSpecified(Arg.REPLACE_FLOATS)) {
            if (replaceFloatsOpcodes.contains(opCode))
                return opCodeInstrumenter;
        }
        if (arg==null) return null;
        if (args.isOperationEnabled(arg)) {
            return opCodeInstrumenter;
        }

        return null;
    }
}
