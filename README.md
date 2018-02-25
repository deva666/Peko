# PEKO
### PErmissions in KOtlin


#### Android Permissions with Kotlin Coroutines
No more callbacks and listeners for requesting Android permissions.

***

Example in Android Activity:
```kotlin
launch (UI) {
    val result = Peko.requestPermissions(this, Manifest.permission.BLUETOOTH).await()
    
    if (result.grantedPermissions.contains(Manifest.permission.BLUETOOTH)) {
        //we have permission
    } else {
        //can't continue
    }
}
```

If you want to show a permission rationale to the user, you can use the built in `AlertDialogPermissionRationale`. This will show an Alert Dialog with your message and title, explaining to user why this rationale is needed. It will be shown only once and only if user denies the permission for the first time.

```kotlin
launch (UI) {
    val rationale = AlertDialogPermissionRationale(this@MainActivity) {
				this.setTitle("Need permissions")
				this.setMessage("Please give permissions to use this feature")	
				}
	val result = Peko.requestPermissions(this, Manifest.permission.BLUETOOTH, rationale = rationale).await()
}
```

You can also show your own implementation of Permission Rationale to the user. Just implement the interface `PermissionRationale`. If `true` is returned from suspend function `shouldRequestAfterRationaleShown`, Permission Request will be repeated, otherwise request permission request completes.
