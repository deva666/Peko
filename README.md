# PEKO

**PE**rmissions with **KO**tlin

[![Build Status](https://travis-ci.org/deva666/Peko.svg?branch=master)](https://travis-ci.org/deva666/Peko) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=deva666_Peko&metric=alert_status)](https://sonarcloud.io/dashboard?id=deva666_Peko) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Peko-blue.svg?style=flat)](https://android-arsenal.com/details/1/6861) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
---

### Android Permissions with Kotlin Coroutines and Flow API

No more callbacks, builders, listeners or verbose code for requesting Android permissions.  
Get Permission Request Result with one function call.    
Built with [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
and [Flow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/).
***

### Thanks to JetBrains

Supported by [JetBrains Open Source](https://www.jetbrains.com/community/opensource/#support)

[<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.png" width=200 height=200/>](https://www.jetbrains.com/)

### Installation

Hosted on [Maven Central](https://search.maven.org/artifact/com.markodevcic/peko/2.2.0/aar)

```
implementation 'com.markodevcic:peko:3.0.0-ALPHA-01'
```

### Example

First initialize the `PermissionRequester` with Application Context. This enables all requests to be made without
a `Context` or `Activity`.   
If you pass an `Activity` as `Context`, `IllegalStateException` is raised.

```kotlin
PermissionRequester.initialize(applicationContext)
```

Get the instance of `PermissionRequester` interface.

```kotlin
val requester = PermissionRequester.instance()
```

Request one or more permissions. For each permission receive `PermissionResult` as async `Flow` stream of data.

```kotlin
launch {
	requester.requestPermissions(Manifest.permission.CAMERA, Manifest.permission.READ_CONTACTS)
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

Requesting multiple permissions in a single go represents a data stream of `PermissionsResult` objects. `Flow` fits here
perfectly. Each permission requested is either granted or denied, with `Flow` we can operate on each emitted result item
and inspect it individually, that is check if it is Granted, Denied or Needs Rationale. Flows are async and require a
coroutine to collect so this is not a huge update from Peko version 2. Also, they are now part of Kotlin Coroutines
library, so no new dependencies are added.

Don't want to use `Flow`? No problem, suspendable extension functions that collect for you are there.

```kotlin
// just check all granted
launch {
	val allGranted: Boolean = requester.requestPermissions(Manifest.permission.CAMERA).allGranted()
}

// give me just granted permissions
launch {
	val granted: Collection<PermissionResult> =
			requester.requestPermissions(Manifest.permission.CAMERA).grantedPermissions()
}


// give me all denied permissions, whatever the reason
launch {
	val denied: Collection<PermissionResult> =
			requester.requestPermissions(Manifest.permission.CAMERA).deniedPermissions()

// these can be then separated to see needs rationale or denied permanently permissions
	val needsRationale = denied.filterIsInstance<PermissionResult.Denied.NeedsRationale>()
	val deniedPermanently = denied.filterIsInstance<PermissionResult.Denied.DeniedPermanently>()
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

### Testing

Using permission requests as part of your business logic and want to run your unit tests on JVM?
Perfect, `PermissionRequester` is an interface which can be easily mocked in your unit tests. It does not require
a `Context` or `Activity` for any methods. Only a one time registration of Application `Context` needs to be done during
app startup with `PermissionRequester.initialize` method.

### Screen rotations

Library supports screen rotations. The only requirement is to preserve the instance of `PermissionRequester` during
device orientation change. How to do this is entirely up to a developer. Easiest way is to use `PermissionRequester`
with lifecycle aware Jetpack `ViewModel` which does this automatically.

### What is new

Peko Version `3` is now in Alpha stage. The API is mostly stabilised but still subject to change. Peko now uses
coroutine `Flow` instead of `suspend` function for returning `PermissionResult`. Support for `LiveData` is
removed. `Flow` can easily be adapted to work with `LiveData`.

## Breaking changes from Peko Version `2`

* `PermissionResult` now has a single `String` permission as property.
* `Peko` singleton is removed. `PermissionRequester` interface is now its replacement.
* Extension functions for `Fragment` and `Activity` are removed.

Peko Version `2.0` uses vanilla Kotlin coroutines, and is [here](https://github.com/deva666/Peko/tree/release/2.0.0).

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
