# -*- coding: utf-8 -*-
# Create CastOptionsProvider.kt
provider_code = '''package com.iptv.app

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.CastMediaControlIntent

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? {
        return null
    }
}
'''
with open('app/src/main/java/com/iptv/app/CastOptionsProvider.kt', 'w', encoding='utf-8') as f:
    f.write(provider_code)

# Update AndroidManifest.xml
with open('app/src/main/AndroidManifest.xml', 'r', encoding='utf-8') as f:
    manifest = f.read()

meta_data = '''<application
        android:name=".IPTVApplication"
        android:allowBackup="true"
        android:icon="@drawable/logo"
        android:banner="@drawable/logo"
        android:label="MyIPTV"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">
        
        <meta-data
            android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
            android:value="com.iptv.app.CastOptionsProvider" />'''

import re
manifest = re.sub(r'<application[^>]*>', meta_data, manifest, count=1)

with open('app/src/main/AndroidManifest.xml', 'w', encoding='utf-8') as f:
    f.write(manifest)

print("Done")
