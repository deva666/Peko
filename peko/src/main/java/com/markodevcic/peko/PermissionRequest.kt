package com.markodevcic.peko

internal class PermissionRequest(val granted: List<String>,
                                 val needRational: List<String>,
                                 val denied: List<String>)