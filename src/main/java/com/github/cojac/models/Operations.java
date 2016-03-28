package com.github.cojac.models;
import  org.objectweb.asm.Opcodes;
import static com.github.cojac.models.Parameters.*;
import static com.github.cojac.models.Signatures.*;
public enum Operations {
    //TODO : enumerate
    
    
   
    
            /*INTEGERS*/
            IADD(Opcodes.IADD,INTEGER_BINARY.description, INTEGER_BINARY_PARAMS.params),
            ISUB(Opcodes.ISUB,INTEGER_BINARY.description, INTEGER_BINARY_PARAMS.params),
            IMUL(Opcodes.IMUL,INTEGER_BINARY.description, INTEGER_BINARY_PARAMS.params),
            IDIV(Opcodes.IDIV,INTEGER_BINARY.description, INTEGER_BINARY_PARAMS.params),
            
            INEG(Opcodes.INEG,INTEGER_UNARY.description, INTEGER_UNARY_PARAMS.params),
            //IINC(Opcodes.IINC,INTEGER_UNARY.description, INTEGER_UNARY_PARAMS.params),
            
            /*LONGS*/
            LADD(Opcodes.LADD,LONG_BINARY.description, LONG_BINARY_PARAMS.params),
            LSUB(Opcodes.LSUB,LONG_BINARY.description, LONG_BINARY_PARAMS.params),
            LMUL(Opcodes.LMUL,LONG_BINARY.description, LONG_BINARY_PARAMS.params),
            LDIV(Opcodes.LDIV,LONG_BINARY.description, LONG_BINARY_PARAMS.params),
            
            LNEG(Opcodes.LNEG,LONG_UNARY.description, LONG_UNARY_PARAMS.params),
            
            /*FLOATS*/
            FADD(Opcodes.FADD,FLOAT_BINARY.description, FLOAT_BINARY_PARAMS.params),
            FSUB(Opcodes.FSUB,FLOAT_BINARY.description, FLOAT_BINARY_PARAMS.params),
            FMUL(Opcodes.FMUL,FLOAT_BINARY.description, FLOAT_BINARY_PARAMS.params),
            FDIV(Opcodes.FDIV,FLOAT_BINARY.description, FLOAT_BINARY_PARAMS.params),
            FREM(Opcodes.FREM,FLOAT_BINARY.description, FLOAT_BINARY_PARAMS.params),
            FCMPL(Opcodes.FCMPL,FLOAT_CMP.description, FLOAT_BINARY_PARAMS.params),//  -1 If NaN
            FCMPG(Opcodes.FCMPG,FLOAT_CMP.description, FLOAT_BINARY_PARAMS.params),//  1 if NaN
            
            FNEG(Opcodes.FNEG,FLOAT_UNARY.description, FLOAT_UNARY_PARAMS.params),
            
            /*DOUBLES*/
            DADD(Opcodes.DADD,DOUBLE_BINARY.description, DOUBLE_BINARY_PARAMS.params),
            DSUB(Opcodes.DSUB,DOUBLE_BINARY.description, DOUBLE_BINARY_PARAMS.params),
            DMUL(Opcodes.DMUL,DOUBLE_BINARY.description, DOUBLE_BINARY_PARAMS.params),
            DDIV(Opcodes.DDIV,DOUBLE_BINARY.description, DOUBLE_BINARY_PARAMS.params),
            DREM(Opcodes.DREM,DOUBLE_BINARY.description, DOUBLE_BINARY_PARAMS.params),
            DCMPL(Opcodes.DCMPL,DOUBLE_CMP.description, DOUBLE_BINARY_PARAMS.params),//  -1 If NaN
            DCMPG(Opcodes.DCMPG,DOUBLE_CMP.description, DOUBLE_BINARY_PARAMS.params),//  1 if NaN
            
            DNEG(Opcodes.DNEG,DOUBLE_UNARY.description, DOUBLE_UNARY_PARAMS.params),
            
            /*CASTING*/
            
            /*INT TO ...*/
            I2S(Opcodes.I2S,I2S_CAST.description, INTEGER_UNARY_PARAMS.params),
            I2C(Opcodes.I2C,I2C_CAST.description, INTEGER_UNARY_PARAMS.params),
            I2B(Opcodes.I2B,I2B_CAST.description, INTEGER_UNARY_PARAMS.params),
            I2F(Opcodes.I2F,I2F_CAST.description, INTEGER_UNARY_PARAMS.params),
            I2L(Opcodes.I2L,I2L_CAST.description, INTEGER_UNARY_PARAMS.params),
            I2D(Opcodes.I2D,I2D_CAST.description, INTEGER_UNARY_PARAMS.params),
            
            /*LONG TO ...*/
            L2I(Opcodes.L2I,L2I_CAST.description, LONG_UNARY_PARAMS.params),
            L2F(Opcodes.L2F,L2F_CAST.description, LONG_UNARY_PARAMS.params),
            L2D(Opcodes.L2D,L2D_CAST.description, LONG_UNARY_PARAMS.params),
            
            /*FLOAT TO ...*/
            F2I(Opcodes.F2I,F2I_CAST.description, FLOAT_UNARY_PARAMS.params),
            F2D(Opcodes.F2D,F2D_CAST.description, FLOAT_UNARY_PARAMS.params),
            F2L(Opcodes.F2L,F2L_CAST.description, FLOAT_UNARY_PARAMS.params),
            
            /*DOUBLE TO ...*/
            D2F(Opcodes.D2F,D2F_CAST.description, DOUBLE_UNARY_PARAMS.params),
            D2I(Opcodes.D2I,D2I_CAST.description, DOUBLE_UNARY_PARAMS.params),
            D2L(Opcodes.D2L,D2L_CAST.description, DOUBLE_UNARY_PARAMS.params);

    
    
    public final int opCodeVal;
    //public final String opCodeName;
    public final String signature;
    public final Class<?>[] parameters;
    
    private Operations(int opCodeVal, String signature, Class<?>[] parameters ) {
       this.opCodeVal=opCodeVal;
       //this.opCodeName=opCodeName;
       this.signature = signature;
       this.parameters = parameters;
    }
}
