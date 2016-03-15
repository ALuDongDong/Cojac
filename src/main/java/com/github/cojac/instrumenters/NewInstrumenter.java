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

package com.github.cojac.instrumenters;

import org.objectweb.asm.MethodVisitor;

import com.github.cojac.Arg;
import com.github.cojac.Args;
import com.github.cojac.InstrumentationStats;
import com.github.cojac.models.NewDoubles;
import com.github.cojac.models.Operation;
import com.github.cojac.models.Operations;
import com.github.cojac.models.ReactionType;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;


final class NewInstrumenter implements IOpcodeInstrumenter {
    private final ReactionType reaction;
    private final InstrumentationStats stats;
    private final String logFileName;
    private final BitSet implementedMethods = new BitSet(256);
    private final Map<Integer, InvokableMethod> invocations = new HashMap<Integer, InvokableMethod>(50);

    
    private static final String NEW_DOUBLES = "com/github/cojac/models/NewDoubles";

    NewInstrumenter(Args args, InstrumentationStats stats) {
        super();

        this.stats = stats;

        reaction = args.getReactionType();


        if (args.isSpecified(Arg.CALL_BACK))
            logFileName = args.getValue(Arg.CALL_BACK); // No, I'm not proud of that trick...
        else
            logFileName = args.getValue(Arg.LOG_FILE);

        fillMethods();
    }
    private void fillMethods() {
        for(Operation op: Operations.OPERATIONS){
            
            try {
                NewDoubles.class.getMethod(op.opCodeName, op.parameters);
                System.out.println("method \""+op.opCodeName+"\" modified.");
                invocations.put(op.opCodeVal, new InvokableMethod(NEW_DOUBLES, op.opCodeName, op.signature));
                implementedMethods.set(op.opCodeVal);
            } catch (NoSuchMethodException e) {
                //Method not implemented, no problem.
            }catch(Exception e){
                e.printStackTrace();
            }
        }//*/
        
        //invocations.put(IADD, new InvokableMethod(CHECKED_INTS, "checkedIADD", Signatures.RAW_INTEGER_BINARY));
        //invocations.put(ISUB, new InvokableMethod(CHECKED_INTS, "checkedISUB", Signatures.RAW_INTEGER_BINARY));
        
        /*invocations.put(IMUL, new InvokableMethod(CHECKED_INTS, "checkedIMUL", Signatures.RAW_INTEGER_BINARY));
        invocations.put(IDIV, new InvokableMethod(CHECKED_INTS, "checkedIDIV", Signatures.RAW_INTEGER_BINARY));

        invocations.put(INEG, new InvokableMethod(CHECKED_INTS, "checkedINEG", Signatures.RAW_INTEGER_UNARY));
        invocations.put(IINC, new InvokableMethod(CHECKED_INTS, "checkedIINC", Signatures.RAW_INTEGER_BINARY));

        invocations.put(LADD, new InvokableMethod(CHECKED_LONGS, "checkedLADD", Signatures.RAW_LONG_BINARY));
        invocations.put(LSUB, new InvokableMethod(CHECKED_LONGS, "checkedLSUB", Signatures.RAW_LONG_BINARY));
        invocations.put(LMUL, new InvokableMethod(CHECKED_LONGS, "checkedLMUL", Signatures.RAW_LONG_BINARY));
        invocations.put(LDIV, new InvokableMethod(CHECKED_LONGS, "checkedLDIV", Signatures.RAW_LONG_BINARY));

        invocations.put(LNEG, new InvokableMethod(CHECKED_LONGS, "checkedLNEG", Signatures.RAW_LONG_UNARY));

        invocations.put(DADD, new InvokableMethod(CHECKED_DOUBLES, "checkedDADD", Signatures.RAW_DOUBLE_BINARY));
        invocations.put(DSUB, new InvokableMethod(CHECKED_DOUBLES, "checkedDSUB", Signatures.RAW_DOUBLE_BINARY));
        invocations.put(DMUL, new InvokableMethod(CHECKED_DOUBLES, "checkedDMUL", Signatures.RAW_DOUBLE_BINARY));
        invocations.put(DDIV, new InvokableMethod(CHECKED_DOUBLES, "checkedDDIV", Signatures.RAW_DOUBLE_BINARY));
        invocations.put(DREM, new InvokableMethod(CHECKED_DOUBLES, "checkedDREM", Signatures.RAW_DOUBLE_BINARY));
        invocations.put(DCMPL, new InvokableMethod(CHECKED_DOUBLES, "checkedDCMPL", Signatures.RAW_DOUBLE_CMP));
        invocations.put(DCMPG, new InvokableMethod(CHECKED_DOUBLES, "checkedDCMPG", Signatures.RAW_DOUBLE_CMP));

        invocations.put(FADD, new InvokableMethod(CHECKED_FLOATS, "checkedFADD", Signatures.RAW_FLOAT_BINARY));
        invocations.put(FSUB, new InvokableMethod(CHECKED_FLOATS, "checkedFSUB", Signatures.RAW_FLOAT_BINARY));
        invocations.put(FMUL, new InvokableMethod(CHECKED_FLOATS, "checkedFMUL", Signatures.RAW_FLOAT_BINARY));
        invocations.put(FREM, new InvokableMethod(CHECKED_FLOATS, "checkedFREM", Signatures.RAW_FLOAT_BINARY));
        invocations.put(FDIV, new InvokableMethod(CHECKED_FLOATS, "checkedFDIV", Signatures.RAW_FLOAT_BINARY));
        invocations.put(FCMPL, new InvokableMethod(CHECKED_FLOATS, "checkedFCMPL", Signatures.RAW_FLOAT_CMP));
        invocations.put(FCMPG, new InvokableMethod(CHECKED_FLOATS, "checkedFCMPG", Signatures.RAW_FLOAT_CMP));

        invocations.put(L2I, new InvokableMethod(CHECKED_CASTS, "checkedL2I", Signatures.RAW_L2I));
        invocations.put(I2S, new InvokableMethod(CHECKED_CASTS, "checkedI2S", Signatures.RAW_I2S));
        invocations.put(I2C, new InvokableMethod(CHECKED_CASTS, "checkedI2C", Signatures.RAW_I2C));
        invocations.put(I2B, new InvokableMethod(CHECKED_CASTS, "checkedI2B", Signatures.RAW_I2B));
        invocations.put(D2F, new InvokableMethod(CHECKED_CASTS, "checkedD2F", Signatures.RAW_D2F));
        invocations.put(D2I, new InvokableMethod(CHECKED_CASTS, "checkedD2I", Signatures.RAW_D2I));
        invocations.put(D2L, new InvokableMethod(CHECKED_CASTS, "checkedD2L", Signatures.RAW_D2L));
        invocations.put(F2I, new InvokableMethod(CHECKED_CASTS, "checkedF2I", Signatures.RAW_F2I));
        invocations.put(F2L, new InvokableMethod(CHECKED_CASTS, "checkedF2L", Signatures.RAW_F2L));
        invocations.put(I2F, new InvokableMethod(CHECKED_CASTS, "checkedI2F", Signatures.RAW_I2F));
        invocations.put(L2D, new InvokableMethod(CHECKED_CASTS, "checkedL2D", Signatures.RAW_L2D));*/
    }

    @Override
    public void instrument(MethodVisitor mv, int opCode) { 
       /* mv.visitLdcInsn(reaction.value());
        mv.visitLdcInsn(logFileName);*/
        
        Arg arg = Arg.fromOpCode(opCode);
        System.out.println("instrument: "+opCode);
        if (arg != null) {
            stats.incrementCounterValue(opCode);// arg
            invocations.get(opCode).invokeStatic(mv);
        }
    }
    
    @Override
    public boolean wantsToInstrument(int opcode) {
        return implementedMethods.get(opcode);
    }

}
