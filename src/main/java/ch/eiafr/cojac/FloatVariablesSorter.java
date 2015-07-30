/*
 * *
 *    Copyright 2014 Romain Monnard & Frédéric Bapst
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

import static ch.eiafr.cojac.models.FloatReplacerClasses.*;
import java.util.ArrayList;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * Remap the local variables indices for load & store instructions
 * The FloatVariablesSorter is used to remap the variable defined by the
 * parameters passing for method invocation and delegate the other local
 * variable remappage to the LocalVariableSorter.
 */
public class FloatVariablesSorter extends LocalVariablesSorter{
    
    private final int[] firstFrameMapping;
    private final int maxRenumber;
    private static final Type OBJECT_TYPE = Type
            .getObjectType("java/lang/Object");
    
    public FloatVariablesSorter(int access, String desc, MethodVisitor mv) {
        super(Opcodes.ASM5, access, desc, mv);

        Type[] args = Type.getArgumentTypes(desc);
        
        if(args.length == 0){
            firstFrameMapping = null;
            maxRenumber = 0;
        }
        else{
            firstFrameMapping = new int[args.length*2];

            firstFrameMapping[0] = 0;
            int index = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
            int nbrVars = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
            for (Type arg : args) {
                firstFrameMapping[index] = nbrVars;
                index += arg.getSize();
                nbrVars += arg.getSize();
                if (arg.equals(COJAC_DOUBLE_WRAPPER_TYPE)) {
                    index ++;
                }
            }
            maxRenumber = index;
        }
    }


    @Override
    public void visitVarInsn(final int opcode, final int var) {
        Type type;
        switch (opcode) {
        case Opcodes.LLOAD:
        case Opcodes.LSTORE:
            type = Type.LONG_TYPE;
            break;

        case Opcodes.DLOAD:
        case Opcodes.DSTORE:
            type = Type.DOUBLE_TYPE;
            break;

        case Opcodes.FLOAD:
        case Opcodes.FSTORE:
            type = Type.FLOAT_TYPE;
            break;

        case Opcodes.ILOAD:
        case Opcodes.ISTORE:
            type = Type.INT_TYPE;
            break;

        default:
            // case Opcodes.ALOAD:
            // case Opcodes.ASTORE:
            // case RET:
            type = OBJECT_TYPE;
            break;
        }
        mv.visitVarInsn(opcode, remapFirstFrame(var, type));
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        mv.visitIincInsn(remapFirstFrame(var, Type.INT_TYPE), increment);
    }

    @Override
    public void visitLocalVariable(final String name, final String desc, final String signature, final Label start, final Label end, final int index) {
        int newIndex = remapFirstFrame(index, Type.getType(desc));
        mv.visitLocalVariable(name, desc, signature, start, end, newIndex);
    }

    @Override
    public void visitFrame(int type, int nLocal, final Object[] local, int nStack, final Object[] stack) {
        ArrayList<Object> newLocal = new ArrayList<>();
        for (Object object : local) {
            if(object == Opcodes.DOUBLE){
                newLocal.add(COJAC_DOUBLE_WRAPPER_INTERNAL_NAME);
                newLocal.add(Opcodes.TOP);
                nLocal++;
            }
            else if(object == Opcodes.FLOAT){
                newLocal.add(COJAC_FLOAT_WRAPPER_INTERNAL_NAME);
            }
            else if(object instanceof String && ((String)object).endsWith("[D")){
                String tab = (String) object;
                tab = tab.replaceAll("D", COJAC_DOUBLE_WRAPPER_TYPE_DESCR);
                newLocal.add(tab);
            }
            else if(object instanceof String && ((String)object).endsWith("[F")){
                String tab = (String) object;
                tab = tab.replaceAll("F", COJAC_FLOAT_WRAPPER_TYPE_DESCR);
                newLocal.add(tab);
            }
            else{
                newLocal.add(object);
            }
        }
		
		ArrayList<Object> newStack = new ArrayList<>();
		for (Object object : stack) {
			if(object == Opcodes.DOUBLE){
                newStack.add(COJAC_DOUBLE_WRAPPER_INTERNAL_NAME);
            }
            else if(object == Opcodes.FLOAT){
                newStack.add(COJAC_FLOAT_WRAPPER_INTERNAL_NAME);
            }
            else if(object instanceof String && ((String)object).endsWith("[D")){
                String tab = (String) object;
                tab = tab.replaceAll("D", COJAC_DOUBLE_WRAPPER_TYPE_DESCR);
                newStack.add(tab);
            }
            else if(object instanceof String && ((String)object).endsWith("[F")){
                String tab = (String) object;
                tab = tab.replaceAll("F", COJAC_FLOAT_WRAPPER_TYPE_DESCR);
                newStack.add(tab);
            }
            else{
                newStack.add(object);
            }
		}
        mv.visitFrame(type, nLocal, newLocal.toArray(), nStack, newStack.toArray());
    }

    private int remapFirstFrame(final int var, final Type type){
        if(var + type.getSize() > maxRenumber){
            return var;
        }
        return firstFrameMapping[var];
    }
    
}
