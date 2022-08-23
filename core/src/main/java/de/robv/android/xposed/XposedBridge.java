/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 *
 * Originally written by rovo89 as part of the original Xposed
 * Copyright 2013 rovo89, Tungstwenty
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0
 */

package de.robv.android.xposed;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings({"unused"})
public class XposedBridge {
    private static final String TAG = "AliuHook-XposedBridge";
    public static final ClassLoader BOOTCLASSLOADER = XposedBridge.class.getClassLoader();
    public static Class<?> aliuhook = null;
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final Map<Member, HookInfo> hookRecords = new HashMap<>();
    private static final Method callbackMethod;

    static {
        try {
            callbackMethod = XposedBridge.HookInfo.class.getMethod("callback", Object[].class);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize", t);
        }
    }
    public static void initReposed(Context context, Class<?> hook){
        aliuhook = hook;
        String command;
        JSONArray modulesPaths;
        JSONArray modulesXposedInit;
        try {
            // query the main app(reposed.app) for whether to shut down
            // or to load modules and get the modules paths
            Bundle bundle = context.getContentResolver().call(Uri.parse("content://reposed.app.contentProvider/"),"",null,null);
            log("reposed.app reply: "+bundle);
            command = bundle.getString("command");
            modulesPaths = new JSONArray(bundle.getString("modulesPaths"));
            modulesXposedInit = new JSONArray(bundle.getString("modulesXposedInit"));
        } catch (Exception t) {
            log(t);
            return;
        }
        if ("shutdown".equals(command)){
            AlertDialog.Builder builder;
            AlertDialog dialog;
            builder = new AlertDialog.Builder(context);
            builder.setTitle("App is frozen");
            builder.setMessage("Do you want to unfreeze this app?");
            builder.setPositiveButton("Unfreeze app", null);
            builder.setNegativeButton("Keep app frozen", null);
            dialog = builder.create();
            dialog.show();
            try {
            TimeUnit.SECONDS.sleep(99999);
            }catch (Throwable ignored){}
//            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
//            for (ActivityManager.AppTask task : am.getAppTasks()){
//                task.finishAndRemoveTask();
//            }
//            System.exit(0);
        }
        try {
            if ("loadModules".equals(command)) {
                //make the a new instance of LoadPackageParam
                var lpparam = new XC_LoadPackage.LoadPackageParam();
                //sets required fields for LoadPackageParam
                lpparam.packageName =  context.getPackageName();
                lpparam.processName = AndroidAppHelper.currentProcessName();
                lpparam.classLoader = aliuhook.getClassLoader();
                lpparam.appInfo =  context.getApplicationInfo();
                lpparam.isFirstApplication = true;
                //load all module and pass lpparam to it
                loadModulesList(modulesXposedInit,modulesPaths,lpparam);
            }
        } catch (Throwable th) {
            log(th);
        }
    }

    private static void loadModulesList(JSONArray modulesXposedInit,JSONArray appsPaths,Object lpparam) throws Throwable {
        for (int i = 0;i<appsPaths.length();i++){
            try {
                String sourceDir = appsPaths.getString(i);
                var moduleClassLoader = new PathClassLoader(sourceDir, null, XposedBridge.class.getClassLoader());
                String xposed_init = modulesXposedInit.getString(i);
                if (xposed_init == null) {
                    Log.d("Reposed",String.format("Error while loading module from %s: No xposed_init file found", sourceDir));
                    continue;
                }
                Class<?> handleLoadPackageClass;
                try {
                    handleLoadPackageClass = Class.forName(xposed_init, true, moduleClassLoader);
                    if (!IXposedMod.class.isAssignableFrom(handleLoadPackageClass)) {
                        Log.d("Reposed",String.format("Error while loading module from %s: Xposed_init class %s doesn't implement any sub-interface of IXposedMod", sourceDir, handleLoadPackageClass.getName()));
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    log(String.format("Error while loading module from %s: Xposed_init Class %s is not found", sourceDir, xposed_init));
                    log(e);
                    continue;
                }
                handleLoadPackageClass.getMethod("handleLoadPackage", Class.forName("de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam", true, moduleClassLoader)).invoke(handleLoadPackageClass.newInstance(), lpparam);
            }catch (Throwable th){
                log(th);
            }
        }
    }

    private static String getXposed_init(Context context,String pkgName) {
        try {
            AssetManager assetManager = context.getPackageManager().getResourcesForApplication(pkgName).getAssets();
            StringBuilder sb = new StringBuilder();
            InputStream is = assetManager.open("xposed_init");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            br.close();
            return sb.toString();
        } catch (Throwable e) {
            return null;
        }
    }

    private static Method hook0(Object context, Member original, Method callback){
        try {
            Method method = aliuhook.getDeclaredMethod("hook", Object.class, Member.class, Method.class);
            return (Method) method.invoke(null,context,original,callback);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }


    private static boolean unhook0(Member target) {
        try {
            Method method = aliuhook.getDeclaredMethod("unhook", Member.class);
            return (boolean) method.invoke(null,target);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }

    private static boolean deoptimize0(Member target){
        try {
            Method method = aliuhook.getDeclaredMethod("deoptimize", Member.class);
            return (boolean) method.invoke(null,target);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }

    private static boolean makeClassInheritable0(Class<?> target){
        try {
            Method method = aliuhook.getDeclaredMethod("makeClassInheritable", Class.class);
            return (boolean) method.invoke(null,target);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }

    // Not used for now
    private static boolean isHooked0(Member target){
        try {
            Method method = aliuhook.getDeclaredMethod("isHooked", Member.class);
            return (boolean) method.invoke(null,target);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }

    /**
     * Disable profile saver to try to prevent ART ahead of time compilation
     * which may lead to aggressive method inlining, thus resulting in those
     * methods being unhookable unless you first call {@link #deoptimizeMethod(Member)} on all callers
     * of that method.
     * <p>
     * You could also try deleting /data/misc/profiles/cur/0/com.YOURPACKAGE/primary.prof
     * <p>
     * See https://source.android.com/devices/tech/dalvik/configure#how_art_works for more info
     *
     * @return Whether disabling profile saver succeeded
     */
    public static boolean disableProfileSaver(){
        try {
            Method method = aliuhook.getDeclaredMethod("disableProfileSaver");
            return (boolean) method.invoke(null);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }

    /**
     * Disables HiddenApi restrictions, thus allowing you access to all private interfaces.
     * <p>
     *
     * @return Whether disabling hidden api succeeded
     * @see <a href="https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces">https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces</a>
     */
    public static boolean disableHiddenApiRestrictions(){
        try {
            Method method = aliuhook.getDeclaredMethod("disableHiddenApiRestrictions");
            return (boolean) method.invoke(null);
        }
        catch (Throwable th){
            throw new RuntimeException(th);
        }
    }

    private static void checkMethod(Member method) {
        if (method == null)
            throw new NullPointerException("method must not be null");
        if (!(method instanceof Method || method instanceof Constructor<?>))
            throw new IllegalArgumentException("method must be a Method or Constructor");

        var modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers))
            throw new IllegalArgumentException("method must not be abstract");
    }

    /**
     * Check if a method is hooked
     * @param method The method to check
     * @return true if method is hooked
     */
    public static boolean isHooked(Member method) {
        return hookRecords.containsKey(method);
    }

    /**
     * Make a final class inheritable. Removes final modifier from class and its constructors and makes
     * constructors accessible (private -> protected)
     *
     * @param clazz Class to make inheritable
     */
    public static boolean makeClassInheritable(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException("class must not be null");

        return makeClassInheritable0(clazz);
    }

    /**
     * Deoptimize a method to avoid inlining
     *
     * @param method The method to deoptimize. Generally it should be a caller of a method
     *               that is inlined.
     */
    public static boolean deoptimizeMethod(Member method) {
        checkMethod(method);
        return deoptimize0(method);
    }

    /**
     * Hook any method (or constructor) with the specified callback.
     *
     * @param method   The method to be hooked.
     * @param callback The callback to be executed when the hooked method is called.
     * @return An object that can be used to remove the hook.
     */
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) {
        checkMethod(method);
        if (callback == null) throw new NullPointerException("callback must not be null");

        HookInfo hookRecord;
        synchronized (hookRecords) {
            hookRecord = hookRecords.get(method);
            if (hookRecord == null) {
                hookRecord = new HookInfo(method);
                var backup = hook0(hookRecord, method, callbackMethod);
                if (backup == null) throw new IllegalStateException("Failed to hook method");
                hookRecord.backup = backup;
                hookRecords.put(method, hookRecord);
            }
        }

        hookRecord.callbacks.add(callback);

        return callback.new Unhook(method);
    }

    /**
     * Hooks all methods with a certain name that were declared in the specified class. Inherited
     * methods and constructors are not considered. For constructors, use
     * {@link #hookAllConstructors} instead.
     *
     * @param hookClass  The class to check for declared methods.
     * @param methodName The name of the method(s) to hook.
     * @param callback   The callback to be executed when the hooked methods are called.
     * @return A set containing one object for each found method which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member method : hookClass.getDeclaredMethods())
            if (method.getName().equals(methodName))
                unhooks.add(hookMethod(method, callback));
        return unhooks;
    }

    /**
     * Hook all constructors of the specified class.
     *
     * @param hookClass The class to check for constructors.
     * @param callback  The callback to be executed when the hooked constructors are called.
     * @return A set containing one object for each found constructor which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member constructor : hookClass.getDeclaredConstructors())
            unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

    /**
     * Removes the callback for a hooked method/constructor.
     *
     * @param method   The method for which the callback should be removed.
     * @param callback The reference to the callback as specified in {@link #hookMethod}.
     * @deprecated Use {@link XC_MethodHook.Unhook#unhook} instead. An instance of the {@code Unhook}
     * class is returned when you hook the method.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static void unhookMethod(Member method, XC_MethodHook callback) {
        synchronized (hookRecords) {
            var record = hookRecords.get(method);
            if (record != null) {
                record.callbacks.remove(callback);
                if (record.callbacks.size() == 0) {
                    hookRecords.remove(method);
                    unhook0(method);
                }
            }
        }
    }

    /**
     * Basically the same as {@link Method#invoke}, but calls the original method
     * as it was before the interception by Xposed. Also, access permissions are not checked.
     *
     * <p class="caution">There are very few cases where this method is needed. A common mistake is
     * to replace a method and then invoke the original one based on dynamic conditions. This
     * creates overhead and skips further hooks by other modules. Instead, just hook (don't replace)
     * the method and call {@code param.setResult(null)} in {@link XC_MethodHook#beforeHookedMethod}
     * if the original method should be skipped.
     *
     * @param method     The method to be called.
     * @param thisObject For non-static calls, the "this" pointer, otherwise {@code null}.
     * @param args       Arguments for the method call as Object[] array.
     * @return The result returned from the invoked method.
     * @throws NullPointerException      if {@code receiver == null} for a non-static method
     * @throws IllegalAccessException    if this method is not accessible (see {@link AccessibleObject})
     * @throws IllegalArgumentException  if the number of arguments doesn't match the number of parameters, the receiver
     *                                   is incompatible with the declaring class, or an argument could not be unboxed
     *                                   or converted by a widening conversion to the corresponding parameter type
     * @throws InvocationTargetException if an exception was thrown by the invoked method
     */
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (args == null)
            args = EMPTY_ARRAY;

        var hookRecord = hookRecords.get(method);
        try {
            // Checking method is not needed if we found hookRecord
            if (hookRecord != null)
                return invokeMethod(hookRecord.backup, thisObject, args);

            checkMethod(method);
            return invokeMethod(method, thisObject, args);
        } catch (InstantiationException ex) {
            // This should never be reached
            throw new IllegalArgumentException("The class this Constructor belongs to is abstract and cannot be instantiated");
        }
    }

    private static Object invokeMethod(Member member, Object thisObject, Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (member instanceof Method) {
            var method = (Method) member;
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        } else {
            var ctor = (Constructor<?>) member;
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        }
    }

    /**
     * @hide
     */
    public static final class CopyOnWriteSortedSet<E> {
        private transient volatile Object[] elements = EMPTY_ARRAY;

        // Aliucord added
        public int size() {
            return elements.length;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean add(E e) {
            int index = indexOf(e);
            if (index >= 0)
                return false;

            Object[] newElements = new Object[elements.length + 1];
            System.arraycopy(elements, 0, newElements, 0, elements.length);
            newElements[elements.length] = e;
            Arrays.sort(newElements);
            elements = newElements;
            return true;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean remove(E e) {
            int index = indexOf(e);
            if (index == -1)
                return false;

            Object[] newElements = new Object[elements.length - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, elements.length - index - 1);
            elements = newElements;
            return true;
        }

        private int indexOf(Object o) {
            for (int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i]))
                    return i;
            }
            return -1;
        }

        public Object[] getSnapshot() {
            return elements;
        }
    }

    // Aliucord changed: public, so that it can be passed as lsplant context object
    public static class HookInfo {
        public Member backup;
        private final Member method;
        final CopyOnWriteSortedSet<XC_MethodHook> callbacks = new CopyOnWriteSortedSet<>();
        private final boolean isStatic;
        private final Class<?> returnType;

        public HookInfo(Member method) {
            this.method = method;
            isStatic = Modifier.isStatic(method.getModifiers());
            if (method instanceof Method) {
                var rt = ((Method) method).getReturnType();
                if (!rt.isPrimitive()) {
                    returnType = rt;
                    return;
                }
            }
            returnType = null;
        }

        public Object callback(Object[] args) throws Throwable {
            var param = new XC_MethodHook.MethodHookParam();
            param.method = method;

            if (isStatic) {
                param.thisObject = null;
                param.args = args;
            } else {
                param.thisObject = args[0];
                param.args = new Object[args.length - 1];
                System.arraycopy(args, 1, param.args, 0, args.length - 1);
            }

            var hooks = callbacks.getSnapshot();
            var hookCount = hooks.length;

            // shouldn't happen since 0 remaining callbacks leads to unhook
            if (hookCount == 0) {
                try {
                    return invokeMethod(backup, param.thisObject, param.args);
                } catch (InvocationTargetException e) {
                    //noinspection ConstantConditions
                    throw e.getCause();
                }
            }

            int beforeIdx = 0;
            do {
                try {
                    ((XC_MethodHook) hooks[beforeIdx]).beforeHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    param.setResult(null);
                    param.returnEarly = false;
                    continue;
                }

                if (param.returnEarly) {
                    beforeIdx++;
                    break;
                }
            } while (++beforeIdx < hookCount);

            if (!param.returnEarly) {
                try {
                    param.setResult(invokeMethod(backup, param.thisObject, param.args));
                } catch (InvocationTargetException e) {
                    param.setThrowable(e.getCause());
                }
            }

            int afterIdx = beforeIdx - 1;
            do {
                Object lastResult = param.getResult();
                Throwable lastThrowable = param.getThrowable();

                try {
                    ((XC_MethodHook) hooks[afterIdx]).afterHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    if (lastThrowable == null)
                        param.setResult(lastResult);
                    else
                        param.setThrowable(lastThrowable);
                }
            } while (--afterIdx >= 0);

            var result = param.getResultOrThrowable();
            if (returnType != null)
                result = returnType.cast(result);
            return result;
        }
    }

    public static void log(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        Log.d("Reposed",exceptionAsString);
    }
    public static void log(String s) {
        Log.d("Reposed",s);
    }
}

