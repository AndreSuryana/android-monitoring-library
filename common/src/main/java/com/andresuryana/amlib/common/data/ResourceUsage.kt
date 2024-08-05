package com.andresuryana.amlib.common.data

data class ResourceUsage<T>(
    val used: T,
    val total: T
)