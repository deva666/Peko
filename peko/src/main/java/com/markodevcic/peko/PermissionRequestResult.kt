package com.markodevcic.peko

class PermissionRequestResult(val granted: Collection<String>,
							  val denied: Collection<String>)