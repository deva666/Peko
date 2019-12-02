# PEKO
**PE**rmissions with **KO**tlin

[![Build Status](https://travis-ci.org/deva666/Peko.svg?branch=master)](https://travis-ci.org/deva666/Peko) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Peko-blue.svg?style=flat)](https://android-arsenal.com/details/1/6861) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
---
### Android Permissions with Kotlin Coroutines or LiveData
No more callbacks, builders, listeners or verbose code for requesting Android permissions.  
Get Permission Request Result asynchronously with one function call.  
Thanks to [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines), permissions requests are async and lightweight (no new threads are used/created).

Or if you don't use Coroutines, and don't want to manage Lifecycles ... receive Permission Results with LiveData.

***

### Installation

Add `jcenter` repository

```
implementation 'com.markodevcic.peko:peko:2.0.0'
```

### What is new
Peko Version `2.0` now uses Android X packages, Kotlin v1.3.50 and Coroutines 1.3.0.
##
Breaking changes from Peko Version `1.0`

* `PermissionRequestResult` is renamed to `PermissionResult` and is now a sealed class.

    `PermissionResult` has a sealed class hierarchy of following types:
    `PermissionResult.Granted` -> returned when all requested permissions were granted
    
    `PermissionResult.Denied` -> returned when at least one of the permissions was denied
    
    `PermissionResult.Denied.NeedsRationale` -> subclass of `PermissionResult.Denied`, returned 
    when Android OS signals that at least one of the permissions needs to show a rationale
    
    `PermissionResult.Denied.DeniedPermanently` -> subclass of `PermissionResult.Denied`, returned when no 
    permissions need a Rationale and at least one of the permissions has a ticked Do Not Ask Again check box

    `PermissionResult.Denied.JustDenied` -> subclass of `PermissionResult.Denied`, returned when 
    previous two cases are not the cause, for example if you forget to register the Permission in
     AndroidManifest

* `PermissionRationale` interface was removed. Library does not show Permission Rationales anymore.
    You can check now if `PermissionResult` is of type `PermissionResult.NeedsRationale` and implement the rationale yourself.
    
*  Added support for requesting permissions with LiveData


##
Peko Version `1.0` uses AppCompat libraries and is [here](https://github.com/deva666/Peko/tree/release/1.0.1).

### Example 
In an Activity or a Fragment that implements `CoroutineScope` interface:
```kotlin
launch {
    val result = Peko.requestPermissionsAsync(this, Manifest.permission.READ_CONTACTS) 
    
    if (result is PermissionResult.Granted) {
        // we have contacts permission
    } else {
        // permission denied
    }
}
```

Or use one of the extension functions on an Activity or a Fragment:
```kotlin
launch {
    val result = requestPermissionsAsync(Manifest.permission.READ_CONTACTS) 
    
    if (result is PermissionResult.Granted) {
        // we have contacts permission
    } else {
        // permission denied
    }
}
```

Request multiple permissions:
```kotlin
launch {
    val result = requestPermissionsAsync(Manifest.permission.READ_CONTACTS, Manifest.permission.CAMERA) 
    
    if (result is PermissionResult.Granted) {
        // we have both permissions
    } else if (result is PermissionResult.Denied) {
        result.deniedPermissions.forEach { p ->
            // this one was denied
        }
    }
}
```

Denied Result has three subtypes which can be checked to see if we need Permission Rationale or 
user Clicked Do Not Ask Again:
```kotlin
launch {
    val result = requestPermissionsAsync(Manifest.permission.BLUETOOTH, Manifest.permission.CAMERA) 
    
    when (result) {
        is PermissionResult.Cancelled -> { } // interaction was interrupted
        is PermissionResult.Granted -> { } // woohoo, all requested permissions granted
        is PermissionResult.Denied.JustDenied -> { } // at least one permission was denied, maybe we forgot to register it in the AndroidManifest?
        is PermissionResult.Denied.NeedsRationale -> { } // user clicked Deny, let's show a rationale
        is PermissionResult.Denied.DeniedPermanently -> { } // Android System won't show Permission dialog anymore, let's tell the user we can't proceed 
    }
}
```

### LiveData
Hate Coroutines? No problem ... just create an instance of `PermissionsLiveData` and observe the results with your `LifecycleOwner`

In a ViewModel ... if you need to support orientation changes, or anywhere else if not (Presenter)
```kotlin
    val permissionLiveData = PermissionsLiveData()
    
    fun checkPermissions(vararg permissions: String) {
        permissionLiveData.checkPermissions(*permissions)
    }
```

In your `LifecycleOwner`, for example in an Activity
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    viewModel = ViewModelProviders.of(this).get(YourViewModel::class.java)
    // observe has to be called before checkPermissions, so we can get the LifecycleOwner
    viewModel.permissionLiveData.observe(this, Observer { r: PermissionResult ->
        // do something with permission results
    })
}

private fun askContactsPermissions() {
    viewModel.checkPermissions(Manifest.permission.READ_CONTACTS)
}

```

### Screen rotations
Library has support for screen rotations. 
To avoid memory leaks, all Coroutines that have not completed yet, should be cancelled in the `onDestroy` function.
When you detect a orientation change, cancel the `Job` of a `CoroutineScope` with an instance of `ActivityRotatingException`. Internally, this will retain the current request that is in progress. The request is then resumed with calling `resumeRequest` method.

Example:

First:
```kotlin

// job that will be cancelled in onDestroy
private val job = Job()

private fun requestPermission(vararg permissions: String) {
    launch { 
        val result = Peko.requestPermissionsAsync(this@MainActivity, *permissions)
        // check granted permissions
    }
}
```

Then in `onDestroy` of an Activity:
```kotlin
if (isChangingConfigurations) {
    job.cancel(ActivityRotatingException()) // screen rotation, retain the results
} else { 
    job.cancel() // no rotation, just cancel the Coroutine
}
``` 

And when this Activity gets recreated in one of the Activity lifecycle functions, e.g.`onCreate`:
```kotlin

// check if we have a request already (or some other way you detect screen orientation)
if (Peko.isRequestInProgress()) {
    launch {
        // get the existing request and await the result
        val result = Peko.resumeRequest() 
        // check granted permissions
    }
}
```

### LiveData and screen rotations
You don't have to do anything, this logic is already inside the `PermissionsLiveData` class. 
You just have to call observe in the `onCreate` method and of course use `androidx.lifecycle.ViewModel`. 


## License
```text
Copyright 2019 Marko Devcic

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