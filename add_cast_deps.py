# -*- coding: utf-8 -*-
with open('app/build.gradle', 'r', encoding='utf-8') as f:
    text = f.read()

deps = '''
    // Google Cast SDK
    implementation 'com.google.android.gms:play-services-cast-framework:21.4.0'
    implementation 'androidx.mediarouter:mediarouter:1.6.0'
'''

text = text.replace('dependencies {', 'dependencies {' + deps)

with open('app/build.gradle', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
