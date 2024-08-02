package com.andresuryana.monitoringlibrary.common.data

data class ResourceUsage<T>(
    val used: T,
    val total: T
)