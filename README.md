# PEKO
### PErmissions in KOtlin


#### Android Permissions with Kotlin Coroutines
No more callbacks and listeners for requesting Android permissions.

***

Example in Android Activity:
```kotlin
launch (UI) {
    val result = Peko.requestPermissions(this, Manifest.permission.BLUETOOTH)
    
    if (result.grantedPermissions.contains(Manifest.permission.BLUETOOTH)) {
        //we have permission
    } else {
        //can't continue
    }
}
```
