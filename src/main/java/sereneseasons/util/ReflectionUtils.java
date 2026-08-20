package sereneseasons.util;

import java.lang.reflect.Method;

public class ReflectionUtils {
    private ReflectionUtils() {
    }
    
    /**
     * Gets a method from a class using reflection.
     * 
     * @param className The fully qualified class name
     * @param methodName The method name to find
     * @param params The parameter types of the method
     * @return The Method object, made accessible
     * @throws RuntimeException if the class or method cannot be found
     */
    public static Method getMethod(String className, String methodName, Class<?>... params) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, params);
            method.setAccessible(true);
            return method;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found: " + methodName + " in class " + className, e);
        } catch (Exception e) {
            throw new RuntimeException("Error accessing method: " + methodName + " in class " + className, e);
        }
    }
}