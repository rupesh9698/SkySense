# SkySense

1. Download the required Implementations and sync the project.

2. Download the API level 34 

3. Give the proper path to the Keystore file (provided in SkySense directory) in the build.gradle

  signingConfigs {
        debug {
            storeFile file('D:\\SkySense.jks')
            storePassword 'skysense'
            keyAlias 'skysense'
            keyPassword 'skysense'
        }
    }
