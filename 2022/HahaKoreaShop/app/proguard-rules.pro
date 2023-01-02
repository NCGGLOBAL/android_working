# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Program\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 百度PushSDK
-dontwarn com.baidu.**
-keep class com.baidu.**{*; }

# 小米代理推送
-dontwarn com.xiaomi.**
-keep class com.xiaomi.**{*; }

# 魅族代理推送
-dontwarn com.meizu.cloud.**
-keep class com.meizu.cloud.**{*; }

# OPPO代理推送
-keep public class * extends android.app.Service
-keep class com.heytap.msp.** { *;}

# VIVO代理推送
-dontwarn com.vivo.push.**
-keep class com.vivo.push.**{*; }
-keep class com.vivo.vms.**{*; }

# 华为代理推送
-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}