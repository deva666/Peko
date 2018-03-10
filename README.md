# PEKO
### PErmissions in KOtlin


#### Android Permissions with Kotlin Coroutines
No more callbacks, builders, listeners or verbose code for requesting Android permissions.
Request permissions with one function call, thanks to Kotlin Coroutines.

***

Example in Android Activity:
```kotlin
launch (UI) {
    val resultDeferred = Peko.requestPermissions(this, Manifest.permission.BLUETOOTH)
    val result = resultDeferred.await()
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

You can also show your own implementation of Permission Rationale to the user. Just implement the interface `PermissionRationale`. If `true` is returned from suspend function `shouldRequestAfterRationaleShown`, Permission Request will be repeated, otherwise the permission request completes and returns the current permission result.

Here is a SnackBar implementation:
```kotlin
class SnackBarRationale(private val snackbar: Snackbar) : PermissionRationale {
	override suspend fun shouldRequestAfterRationaleShown(): Boolean {
		return suspendCancellableCoroutine { continuation ->
			var resumed = false
			snackbar.setAction("Request again", {
				if (!resumed) {
					resumed = true
					continuation.resume(true)
				}
			})
			snackbar.addCallback(object : Snackbar.Callback(){
				override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
					super.onDismissed(transientBottomBar, event)
					if (!resumed) {
						resumed = true
						continuation.resume(false)
					}
				}
			})
			snackbar.show()
		}
	}
}
```

## License
```text
Copyright 2018 Marko Devcic

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```