# PEKO
**PE**rmissions with **KO**tlin

[![Build Status](https://travis-ci.org/deva666/Peko.svg?branch=master)](https://travis-ci.org/deva666/Peko) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=deva666_Peko&metric=alert_status)](https://sonarcloud.io/dashboard?id=deva666_Peko) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Peko-blue.svg?style=flat)](https://android-arsenal.com/details/1/6861) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
---
### Android Permissions with Kotlin Coroutines and Flow API
No more callbacks, builders, listeners or verbose code for requesting Android permissions.  
Get Permission Request Result as 
Thanks to [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) and [Flow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/) receive Permission Results .
***


### Thanks to JetBrains
Supported by [JetBrains Open Source](https://www.jetbrains.com/community/opensource/#support)

[<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.png" width=200 height=200/>](https://www.jetbrains.com/)

### Installation

Hosted on [Maven Central](https://search.maven.org/artifact/com.markodevcic/peko/2.2.0/aar)

```
implementation 'com.markodevcic:peko:2.2.0'
```

### Example 
First initialize the requester with Application Context. If you pass an `Activity` as `Context`, `IllegalStateException` is raised.
```kotlin
PermissionRequester.initialize(applicationContext)
```
Get the `PermissionRequester` interface.
```kotlin
val requester = PermissionRequester.instance
```
Request one or more permissions
```kotlin
launch {
	requester.requestPermissions(Manifest.permission.CAMERA)
      .collect { p ->
        when (p) {
			is PermissionResult.Granted -> print("${p.permission} granted") // nice, proceed 
            is PermissionResult.Denied -> print("${p.permission} denied") // denied, not interested in reason
            is PermissionResult.Denied.NeedsRationale -> print("${p.permission} needs rationale") // show rationale
            is PermissionResult.Denied.DeniedPermanently -> print("${p.permission} denied for good") // no go
            is PermissionResult.Cancelled -> print("request cancelled") // op canceled, repeat the request
        } 
      }
}

```

### Why Flows?
Requesting multiple permissions in a single go represents a data stream of `PermissionsResult` objects. `Flow` fits here perfectly.
Each permission requested is either granted or denied, with `Flow` we can operate on each emitted result item and inspect it individually, that is check if it is Granted, Denied or Needs Rationale.
And `Flow` is now part of `Kotlin Coroutines library, so no new dependencies are added.
They are also suspendable, require a coroutine to collect.

Don't want to use `Flow` API and collect items? No problem, suspendable extension functions that collect for you are there.
```kotlin
// just check all granted
launch {
  val isGranted: Boolean = requester.requestPermissions(Manifest.permission.CAMERA).allGranted()
}

// give me just granted permissions
launch {
  val granted: Collection<PermissionResult> =
    requester.requestPermissions(Manifest.permission.CAMERA).grantedPermissions()
}


// give me all denied permissions
launch {
  val denied: Collection<PermissionResult> =
    requester.requestPermissions(Manifest.permission.CAMERA).deniedPermissions()
}

// give me needs rationale permissions
launch {
  val needsRationale: Collection<PermissionResult> =
    requester.requestPermissions(Manifest.permission.CAMERA).needsRationalePermissions()
}

// give me needs denied permanently permissions
launch {
  val deniedPermanently: Collection<PermissionResult> =
    requester.requestPermissions(Manifest.permission.CAMERA).deniedPermanently()
}
```


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

### Testing
Common use case is that some business logic triggers permission requests. Business logic usually is placed in a `ViewModel`, `Presenter` or is decouple from a view in other way.
Android Permissions API requires `Context` for all Permission checks. This breaks the flow of business logic, in a way that a `ViewModel` has to delagete or communicate to the view to get the permissions.
Business Logic should be testable
Peko is built to break this dependency and all permission requests can be called from your business logic without requiring any `Context` or `Activity`.
Furthermore, having permission request free from view means that you can run your unit tests on JVM, without the need for emulators or physical devices.
To support this, `PermissionRequest` is an interface which can be easily mocked in your JVM unit tests.
One time registration of Application `Context` needs to be done during app startup with `PermissionRequester.initialize` method.

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
user Clicked Do Not Ask Again.
```kotlin
launch {
    val result = requestPermissionsAsync(Manifest.permission.BLUETOOTH, Manifest.permission.CAMERA) 
    
    when (result) {
        is PermissionResult.Granted -> { } // woohoo, all requested permissions granted
        is PermissionResult.Denied.JustDenied -> { } // at least one permission was denied, maybe we forgot to register it in the AndroidManifest?
        is PermissionResult.Denied.NeedsRationale -> { } // user clicked Deny, let's show a rationale
        is PermissionResult.Denied.DeniedPermanently -> { } // Android System won't show Permission dialog anymore, let's tell the user we can't proceed
        is PermissionResult.Cancelled -> { } // interaction was interrupted
    }
}
```

If you want to know which permissions were denied, they are a property of `Denied` class.
```
class Denied(val deniedPermissions: Collection<String>)
```

Need to check if permission is granted? Yes, let's skip the horrible Android API.
Single call, accepts multiple Strings as arguments, returns true if all are granted.
```
val granted = Peko.areGranted(activity, Manifest.permission.READ_CONTACTS)
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


### What is new
Peko Version `2` now uses Android X packages, Kotlin v1.5.30 and Coroutines 1.5.2.
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

    `PermissionResult.Cancelled` -> returned when Android System cancels the request, ie returned

* `PermissionRationale` interface was removed. Library does not show Permission Rationales anymore.
    You can check now if `PermissionResult` is of type `PermissionResult.NeedsRationale` and implement the rationale yourself.
    
*  Added support for requesting permissions with LiveData


##
Peko Version `1.0` uses AppCompat libraries and is [here](https://github.com/deva666/Peko/tree/release/1.0.1).


### License
```text
Copyright 2022 Marko Devcic

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
