# AliuHook

Java Xposed Api for [LSPlant](https://github.com/LSPosed/LSPlant)

Please note that this is only a partial implementation of Xposed, since we only need method hooking. Thus, only XposedBridge and hook classes are implemented.
If you need the rest, just copy paste it from original Xposed and it should work with almost no modificiations.

Additionally, XposedBridge contains two new methods: `makeClassInheritable`, `deoptimizeMethod`

## Supported Android versions (same as LSPlant)
- Android 5.0 - 13 (API level 21 - 33)
- armeabi-v7a, arm64-v8a, x86, x86-64


## Get Started

```groovy
repositories {
    maven("https://maven.aliucord.com/snapshots")
}

dependencies {
    implementation "com.aliucord:Aliuhook:main-SNAPSHOT"
}
```

#### Now you're ready to get hooking! No init needed
```java
XposedBridge.hookMethod(Activity.class.getDeclaredMethod("onCreate", Bundle.class), new XC_MethodHook() {
    @Override
    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG, "Activity" + param.thisObject + "about to be created!");
    }
});
```



## Credit
- [LSPlant](https://github.com/LSPosed/LSPlant)
- [Pine](https://github.com/canyie/Pine)
- [Original Xposed API](https://github.com/rovo89/XposedBridge)