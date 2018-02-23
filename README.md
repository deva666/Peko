# PEKO
**_PErmissions in KOtlin_**


Android Permissions with Kotlin Coroutines

_example in Android Activity_

```
launch (UI) {
    val result = Peko.requestPermissions(this, Manifest.permission.BLUETOOTH)
    
    if (result.grantedPermissions.contains(Manifest.permission.BLUETOOTH)) {
        //we have permission
    } else {
        //can't continue
    }
}
```
