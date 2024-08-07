package com.andresuryana.amlib.core.data

data class ResourceUsage<T>(
    val used: T,
    val total: T
)