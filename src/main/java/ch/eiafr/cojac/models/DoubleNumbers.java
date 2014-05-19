/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.eiafr.cojac.models;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 *
 * @author romain
 */
public class DoubleNumbers {
	
	private static Class doubleWrapper = DoubleWrapper.class;
	private static Class floatWrapper = FloatWrapper.class;
    
	public static Object[] newarray(int size) throws Exception{
		Object[] a = (Object[]) Array.newInstance(doubleWrapper, size);
        for (int i = 0; i < a.length; i++) {
            a[i] = doubleWrapper.getConstructor(double.class).newInstance(0);
        }
        return a;
    }

	public static Object initializeMultiArray(Object array, int dimensions) throws Exception {
        Object a[] = (Object[]) array;
        if(dimensions == 1){
            return newarray(a.length);
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = initializeMultiArray(a[i], dimensions-1);
        }
        return array;
    }
    
    private static Object[] convertArray(double[] array) throws Exception{
        Object[] a = (Object[]) Array.newInstance(doubleWrapper, array.length);
        for (int i = 0; i < a.length; i++)
            a[i] = doubleWrapper.getConstructor(double.class).newInstance(array[i]);
        return a;
    }
    
    private static double[] convertArray(Object[] array) throws Exception{
        double[] a = new double[array.length];
        for (int i = 0; i < a.length; i++){
			Method m = doubleWrapper.getMethod("toDouble", new Class[] {doubleWrapper});
			a[i] = (double)m.invoke(doubleWrapper, array[i]);
		}
        return a;
    }
    
	// Get the Type of an array of type compClass with the number of dimensions
    private static Class<?> arrayClass(Class<?> compClass, int dimensions) {
        if (dimensions == 0) {
            return compClass;
        }
        int[] dims = new int[dimensions];
        Object dummy = Array.newInstance(compClass, dims);
        return dummy.getClass();
    }

    public static Object convertArrayToReal(Object array, int dimensions) throws Exception {
        Object a;
		Object[] input = (Object[])array;
        if(dimensions == 1){
            a = convertArray(input);
        }
        else{
            Class<?> compType = arrayClass(double.class, dimensions - 1);
            a = Array.newInstance(compType, input.length);
            Object[] b = (Object[]) a; // All arrays or multi-arrays can be cast to Object[]
            for (int i = 0; i < b.length; i++) {
                b[i] = convertArrayToReal(input[i], dimensions-1); // Initialise the others dimensions
            }
        }
        return a;
    }
	
	public static Object convertArrayToCojac(Object array, int dimensions) throws Exception {
        Object a;
        if(dimensions == 1){
            a = convertArray((double[])array);
        }
        else{
			Object[] input = (Object[])array;
            Class<?> compType = arrayClass(doubleWrapper, dimensions - 1);
            a = Array.newInstance(compType, input.length);
            Object[] b = (Object[]) a; // All arrays or multi-arrays can be cast to Object[]
            for (int i = 0; i < b.length; i++) {
                b[i] = convertArrayToCojac(input[i], dimensions-1); // Initialise the others dimensions
            }
        }
        return a;
    }
    
	public static Object initialize(Object a) throws Exception{
		if(a == null)
			return doubleWrapper.getConstructor(double.class).newInstance(0);
		return a;
	}
	
	public static Object castFromObject(Object obj) throws Exception{
		if(obj instanceof Double)
			return doubleWrapper.getConstructor(double.class).newInstance((Double)obj);
		return obj;
	}
	
	public static Object convertFromObjectToReal(Object obj) throws Exception{
		if(obj == null)
			return obj;
		if(obj.getClass().isArray()){
			Class type = getArrayType(obj);
			if(type.equals(floatWrapper)){
				int dim = getArrayDimension(obj);
				return FloatNumbers.convertArrayToReal(obj, dim);
			}
			if(type.equals(doubleWrapper)){
				int dim = getArrayDimension(obj);
				return DoubleNumbers.convertArrayToReal(obj, dim);
			}
			if(isPrimitiveType(type))
				return obj;
			Object array[] = (Object[]) obj;
			for (int i = 0; i < array.length; i++) 
				array[i] = convertFromObjectToReal(array[i]);
			return (Object) array;
		}
		else{
			if(floatWrapper.isInstance(obj)){
				Method m = floatWrapper.getMethod("toRealFloatWrapper", new Class[] {floatWrapper});
				return m.invoke(floatWrapper, obj);
			}
			if(doubleWrapper.isInstance(obj)){
				Method m = doubleWrapper.getMethod("toRealDoubleWrapper", new Class[] {doubleWrapper});
				return m.invoke(doubleWrapper, obj);
			}
			return obj;
		}
	}
	
	public static Object convertFromObjectToCojac(Object obj) throws Exception{
		if(obj == null)
			return obj;
		if(obj.getClass().isArray()){
			Class type = getArrayType(obj);
			if(type.equals(float.class) || type.equals(Float.class)){
				int dim = getArrayDimension(obj);
				return FloatNumbers.convertArrayToCojac(obj, dim);
			}
			if(type.equals(double.class) || type.equals(Double.class)){
				int dim = getArrayDimension(obj);
				return DoubleNumbers.convertArrayToCojac(obj, dim);
			}
			if(isPrimitiveType(type))
				return obj;
			Object array[] = (Object[]) obj;
			for (int i = 0; i < array.length; i++) 
				array[i] = convertFromObjectToCojac(array[i]);
			return (Object) array;
		}
		else{
			if(obj instanceof Float)
				return floatWrapper.getConstructor(float.class).newInstance((Float)obj);
			if(obj instanceof Double)
				return doubleWrapper.getConstructor(double.class).newInstance((Double)obj);
			return obj;
		}
	}
	
	private static boolean isPrimitiveType(Class type){
		return type.equals(boolean.class) || type.equals(byte.class) ||
				type.equals(char.class) || type.equals(double.class) ||
				type.equals(float.class) || type.equals(int.class) ||
				type.equals(long.class) || type.equals(short.class);
	}
	
	private static Class getArrayType(Object array){
		Class type = array.getClass();
		while (type.isArray())
			type = type.getComponentType();
		return type;
	}
	
	public static int getArrayDimension(Object array) {
		int count = 0;
		Class type = array.getClass();
		while ( type.isArray() ) {
			count++;
			type = type.getComponentType();
		}
		return count;
	}
	
	public static void mergeOriginalArrayIntoCojac(Object cojac, Object original) throws Exception{
		if(cojac.getClass().isArray()){
			Class type = getArrayType(cojac);
			if(type.equals(floatWrapper)){
				int dim = getArrayDimension(cojac);
				mergeFloatArray(original, cojac, dim);
			}
			if(type.equals(doubleWrapper)){
				int dim = getArrayDimension(cojac);
				mergeDoubleArray(original, cojac, dim);
			}
			
		}
	}
	
	private static void mergeFloatArray(Object original, Object cojac, int dimension) throws Exception{
        if(dimension == 1){
            mergeFloatArray((float[])original, (Object[]) cojac);
        }
        else{
			Object[] originalArray = (Object[])original;
			Object[] cojacArray = (Object[])cojac;
            for (int i = 0; i < originalArray.length; i++) {
                mergeFloatArray(originalArray[i], cojacArray[i], dimension-1);
            }
        }
	}
	
	private static void mergeFloatArray(float[] original, Object[] cojac) throws Exception{
		for (int i = 0; i < cojac.length; i++) {
			Method m = floatWrapper.getMethod("toFloat", new Class[] {floatWrapper});
			float val = (float) m.invoke(floatWrapper, cojac[i]);
			if(original[i] != val){
				cojac[i] = floatWrapper.getConstructor(float.class).newInstance(original[i]);
			}
		}
	}
	
	private static void mergeDoubleArray(Object original, Object cojac, int dimension) throws Exception{
        if(dimension == 1){
            mergeDoubleArray((double[])original, (Object[]) cojac);
        }
        else{
			Object[] originalArray = (Object[])original;
			Object[] cojacArray = (Object[])cojac;
            for (int i = 0; i < originalArray.length; i++) {
                mergeDoubleArray(originalArray[i], cojacArray[i], dimension-1);
            }
        }
	}
	
	private static void mergeDoubleArray(double[] original, Object[] cojac) throws Exception{
		for (int i = 0; i < cojac.length; i++) {
			Method m = doubleWrapper.getMethod("toDouble", new Class[] {doubleWrapper});
			double val = (double) m.invoke(doubleWrapper, cojac[i]);
			if(original[i] != val){
				cojac[i] = doubleWrapper.getConstructor(double.class).newInstance(original[i]);
			}
		}
	}
	
}