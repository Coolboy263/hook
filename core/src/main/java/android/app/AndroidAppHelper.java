package android.app;

import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

/**
 * Contains various methods for information about the current app.
 *
 * <p>For historical reasons, this class is in the {@code android.app} package. It can't be moved
 * without breaking compatibility with existing modules.
 */
@SuppressLint("PrivateApi")
public final class AndroidAppHelper {
    private AndroidAppHelper() {}




    /**
     * Returns the main {@link android.app.Application} object in the current process.
     *
     * <p>In a few cases, multiple apps might run in the same process, e.g. the SystemUI and the
     * Keyguard which both have {@code android:process="com.android.systemui"} set in their
     * manifest. In those cases, the first application that was initialized will be returned.
     */
    public static Application currentApplication() {
        try {
            Object ActivityThread = Class.forName("android.app.ActivityThread").getMethod("currentActivityThread").invoke(null);
            return (Application) XposedHelpers.callMethod(ActivityThread,"currentApplication");
        }catch (Exception e){
            return null;
        }
    }



    /**
     * Returns information about the main application in the current process.
     *
     * <p>In a few cases, multiple apps might run in the same process, e.g. the SystemUI and the
     * Keyguard which both have {@code android:process="com.android.systemui"} set in their
     * manifest. In those cases, the first application that was initialized will be returned.
     */
    public static ApplicationInfo currentApplicationInfo() {
        Object am;
        try{
        am = Class.forName("android.app.ActivityThread").getMethod("currentActivityThread").invoke(null);}
        catch (Exception e){
            return null;
        }

        Object boundApplication = getObjectField(am, "mBoundApplication");
        if (boundApplication == null) return null;

        return (ApplicationInfo) getObjectField(boundApplication, "appInfo");
    }



    /**
     * Returns the Android package name of the main application in the current process.
     *
     * <p>In a few cases, multiple apps might run in the same process, e.g. the SystemUI and the
     * Keyguard which both have {@code android:process="com.android.systemui"} set in their
     * manifest. In those cases, the first application that was initialized will be returned.
     */
    public static String currentPackageName() {
        ApplicationInfo ai = currentApplicationInfo();
        return (ai != null) ? ai.packageName : null;
    }


    /**
     * Returns the name of the current process. It's usually the same as the main package name.
     */
    public static String currentProcessName() {
        try {
        String processName = (String) Class.forName("android.app.ActivityThread").getMethod("currentPackageName").invoke(null);
        if (processName == null){
            return "";
        }
        return processName;
        }catch (Exception e){
            return "";
        }
    }
}
