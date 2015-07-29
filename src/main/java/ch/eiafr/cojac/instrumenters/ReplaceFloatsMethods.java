/*
 * *
 *    Copyright 2014 Frédéric Bapst & Romain Monnard
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

import ch.eiafr.cojac.CojacReferences;
import ch.eiafr.cojac.FloatProxyMethod;
import static ch.eiafr.cojac.models.FloatReplacerClasses.*;

import static ch.eiafr.cojac.instrumenters.InvokableMethod.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;

public class ReplaceFloatsMethods {
    
	private final CojacReferences references;
    
    private final Map<MethodSignature, Object> suppressions = new HashMap<>(50);
    private final Map<MethodSignature, InvokableMethod> invocations = new HashMap<>(50);
    private final ArrayList<String> allMethodsConversions = new ArrayList<>(50);

    private static final String FL_NAME = Type.getType(Float.class).getInternalName();
    private static final String FL_DESCR = Type.getType(Float.class).getDescriptor();
    
    private static final String DL_NAME = Type.getType(Double.class).getInternalName();
    private static final String DL_DESCR = Type.getType(Double.class).getDescriptor();
    
    
    private static final String MATH_NAME = Type.getType(Math.class).getInternalName();
    private static final String MATH_DESCR = Type.getType(Math.class).getDescriptor();

    private static String CFW_N;
    private static String CFW;
    private static String CDW_N;
    private static String CDW;
    
    private final ArrayList<String> addedMethods = new ArrayList<>();
    private final FloatProxyMethod fpm;
    private final String classPath;
    private static final String COJAC_PROXY_METHODS_PREFIX = "COJAC_PROXY_METHOD_";
	private static final String COJAC_MAGIC_CALL_DOUBLE_PREFIX = "COJAC_MAGIC_DOUBLE_";
	private static final String COJAC_MAGIC_CALL_FLOAT_PREFIX = "COJAC_MAGIC_FLOAT_";
    
    public ReplaceFloatsMethods(FloatProxyMethod fpm, String classPath, CojacReferences references) {
		CFW_N = COJAC_FLOAT_WRAPPER_INTERNAL_NAME;
		CFW = COJAC_FLOAT_WRAPPER_TYPE_DESCR;
		CDW_N = COJAC_DOUBLE_WRAPPER_INTERNAL_NAME;
		CDW = COJAC_DOUBLE_WRAPPER_TYPE_DESCR;
        this.fpm = fpm;
        this.classPath = classPath;
		this.references = references;
		fillMethods();
    }

    private void fillMethods() {
        
        // Floats replacements
        suppressions.put(new MethodSignature(FL_NAME, "valueOf", "(F)"+FL_DESCR), CFW_N); // delete if the value is already a FloatWrapper
        suppressions.put(new MethodSignature(FL_NAME, "floatValue", "()F"), null); // delete in every case (keep FloatWrapper)
        
        invocations.put(new MethodSignature(FL_NAME, "valueOf", "(F)"+FL_DESCR), new InvokableMethod(CFW_N, "fromFloat", "(F)"+CFW, INVOKESTATIC));
        
        invocations.put(new MethodSignature(FL_NAME, "<init>", "(F)V"), new InvokableMethod(CFW_N, "<init>", "("+CFW+")V", INVOKESPECIAL));
        invocations.put(new MethodSignature(FL_NAME, "<init>", "(Ljava/lang/String;)V"), new InvokableMethod(CFW_N, "<init>", "(Ljava/lang/String;)V", INVOKESPECIAL));
        invocations.put(new MethodSignature(FL_NAME, "<init>", "(D)V"), new InvokableMethod(CFW_N, "<init>", "("+CDW+")V", INVOKESPECIAL));

        
        invocations.put(new MethodSignature(FL_NAME, "doubleValue", "()D"), new InvokableMethod(CFW_N, "f2d", "("+CFW+")"+CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(FL_NAME, "intValue", "()I"), new InvokableMethod(CFW_N, "f2i", "("+CFW+")I", INVOKESTATIC));
        invocations.put(new MethodSignature(FL_NAME, "longValue", "()J"), new InvokableMethod(CFW_N, "f2l", "("+CFW+")J", INVOKESTATIC));

        invocations.put(new MethodSignature(FL_NAME, "parseFloat", "(Ljava/lang/String;)F"), new InvokableMethod(CFW_N, "fromString", "(Ljava/lang/String;)"+CFW, INVOKESTATIC));
        
        allMethodsConversions.add(FL_NAME); // use proxy to call every other methods from Float
        
        
        // Doubles replacements
        suppressions.put(new MethodSignature(DL_NAME, "valueOf", "(D)"+DL_DESCR), CDW_N); // delete if the value is already a DoubleWrapper
        suppressions.put(new MethodSignature(DL_NAME, "doubleValue", "()D"), null); // delete in every case (keep DoubleWrapper)
        
        invocations.put(new MethodSignature(DL_NAME, "valueOf", "(D)"+DL_DESCR), new InvokableMethod(CDW_N, "fromDouble", "(D)"+CDW, INVOKESTATIC));

        invocations.put(new MethodSignature(DL_NAME, "<init>", "(D)V"), new InvokableMethod(CDW_N, "<init>", "("+CDW+")V", INVOKESPECIAL));
        invocations.put(new MethodSignature(DL_NAME, "<init>", "(Ljava/lang/String;)V"), new InvokableMethod(CDW_N, "<init>", "(Ljava/lang/String;)V", INVOKESPECIAL));
        invocations.put(new MethodSignature(DL_NAME, "<init>", "(F)V"), new InvokableMethod(CDW_N, "<init>", "("+CFW+")V", INVOKESPECIAL));

        
        invocations.put(new MethodSignature(DL_NAME, "floatValue", "()F"), new InvokableMethod(CDW_N, "d2f", "("+CDW+")"+CFW, INVOKESTATIC));
        invocations.put(new MethodSignature(DL_NAME, "intValue", "()I"), new InvokableMethod(CDW_N, "d2i", "("+CDW+")I", INVOKESTATIC));
        invocations.put(new MethodSignature(DL_NAME, "longValue", "()J"), new InvokableMethod(CDW_N, "d2l", "("+CDW+")J", INVOKESTATIC));

        invocations.put(new MethodSignature(DL_NAME, "parseFloat", "(Ljava/lang/String;)D"), new InvokableMethod(CDW_N, "fromString", "(Ljava/lang/String;)"+CDW, INVOKESTATIC));
        
        allMethodsConversions.add(DL_NAME); // use proxy to call every other methods from Double

        // TODO: redesign so that it is easier to maintain (and ensure every method gets implemented)
        //       maybe define only two methods math_unary/math_binary, and add a parameter 
        // Math Library
        invocations.put(new MethodSignature(MATH_NAME, "sqrt", "(D)D"),
                new InvokableMethod(CDW_N, "math_sqrt", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "pow", "(DD)D"),
                new InvokableMethod(CDW_N, "math_pow", "(" + CDW + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "sin", "(D)D"),
                new InvokableMethod(CDW_N, "math_sin", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "sinh", "(D)D"),
                new InvokableMethod(CDW_N, "math_sinh", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "asin", "(D)D"),
                new InvokableMethod(CDW_N, "math_asin", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "cos", "(D)D"),
                new InvokableMethod(CDW_N, "math_cos", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "cosh", "(D)D"),
                new InvokableMethod(CDW_N, "math_cosh", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "acos", "(D)D"),
                new InvokableMethod(CDW_N, "math_acos", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "tan", "(D)D"),
                new InvokableMethod(CDW_N, "math_tan", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "atan", "(D)D"),
                new InvokableMethod(CDW_N, "math_atan", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "tanh", "(D)D"),
                new InvokableMethod(CDW_N, "math_tanh", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "toRadians", "(D)D"),
                new InvokableMethod(CDW_N, "math_toRadians", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "toDegrees", "(D)D"),
                new InvokableMethod(CDW_N, "math_toDegrees", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "exp", "(D)D"),
                new InvokableMethod(CDW_N, "math_exp", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "log", "(D)D"),
                new InvokableMethod(CDW_N, "math_log", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "abs", "(D)D"),
                new InvokableMethod(CDW_N, "math_abs", "(" + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "max", "(DD)D"),
                new InvokableMethod(CDW_N, "math_max", "(" + CDW + CDW + ")" + CDW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME, "min", "(DD)D"),
                new InvokableMethod(CDW_N, "math_min", "(" + CDW + CDW + ")" + CDW, INVOKESTATIC));

        // for the BigDecimalFloat math function
        invocations.put(new MethodSignature(MATH_NAME,"abs","(F)F"),
                new InvokableMethod(CFW_N, "math_abs","(" + CFW +")" + CFW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME,"min","(FF)F"),
                new InvokableMethod(CFW_N, "math_min","(" + CFW + CFW +")" + CFW, INVOKESTATIC));
        invocations.put(new MethodSignature(MATH_NAME,"max","(FF)F"),
                new InvokableMethod(CFW_N, "math_max","(" + CFW + CFW +")" + CFW, INVOKESTATIC));

        allMethodsConversions.add(MATH_NAME);
    }
    
    
    public boolean instrument(MethodVisitor mv, int opcode, String owner, String name, String desc, Object stackTop){
        MethodSignature ms = new MethodSignature(owner, name, desc);
        
		if(name.startsWith(COJAC_MAGIC_CALL_DOUBLE_PREFIX)){
			cojacMagicCall(mv, name, desc, CDW_N);
			return true;
		}
		if(name.startsWith(COJAC_MAGIC_CALL_FLOAT_PREFIX)){
			cojacMagicCall(mv, name, desc, CFW_N);
			return true;
		}
		
        InvokableMethod replacementMethod = invocations.get(ms);
        
        if(suppressions.containsKey(ms)){
            Object supressionMethod = suppressions.get(ms);
            if(supressionMethod == null)
                return true;
            if(stackTop != null && stackTop.equals(supressionMethod))
                return true;
        }
        
        if(replacementMethod != null){
            replacementMethod.invoke(mv);
            return true;
        }
        else if(allMethodsConversions.contains(owner)){
            fpm.proxyCall(mv, opcode, owner, name, desc);
            return true;
        }
		
		if(references.hasToBeInstrumented(owner) == false){
			fpm.proxyCall(mv, opcode, owner, name, desc);
			return true;
		}
		
        return false;
    }
	
	
	private void cojacMagicCall(MethodVisitor mv, String name, String desc, String wrapper){
		String newDesc = replaceFloatMethodDescription(desc);
		InvokableMethod im = new InvokableMethod(wrapper, name, newDesc);
		im.invokeStatic(mv);
	}
	
}
