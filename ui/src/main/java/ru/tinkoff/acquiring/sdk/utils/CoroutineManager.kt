/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author Mariya Chernyadieva
 */
internal class CoroutineManager(private val exceptionHandler: (Throwable) -> Unit) {

    private val job = SupervisorJob()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable -> launchOnMain { exceptionHandler(throwable) } }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + coroutineExceptionHandler + job)
    private val disposableSet = hashSetOf<Disposable>()

    fun <R> call(request: Request<R>, onSuccess: (R) -> Unit, onFailure: ((Exception) -> Unit)? = null) {
        disposableSet.add(request)

        launchOnBackground {
            request.execute(
                    onSuccess = {
                        launchOnMain {
                            onSuccess(it)
                        }
                    },
                    onFailure = {
                        launchOnMain {
                            if (onFailure == null) {
                                exceptionHandler.invoke(it)
                            } else {
                                onFailure(it)
                            }
                        }
                    })
        }
    }

    suspend fun <R> callSuspended(request: Request<R>): R {
        disposableSet.add(request)

        return withContext(IO) {
            suspendCoroutine<R> { continuation ->
                request.execute({ result ->
                    continuation.resume(result)
                }, { exception ->
                    continuation.resumeWithException(exception)
                })
            }
        }
    }

    fun cancelAll() {
        disposableSet.forEach {
            it.dispose()
        }
        job.cancel()
    }

    fun runWithDelay(timeMills: Long, block: () -> Unit) {
        coroutineScope.launch(Dispatchers.Main) {
            delay(timeMills)
            block.invoke()
        }
    }

    fun launchOnMain(block: suspend CoroutineScope.() -> Unit) {
        coroutineScope.launch(Dispatchers.Main) {
            block.invoke(this)
        }
    }

    fun launchOnBackground(block: suspend CoroutineScope.() -> Unit) {
        coroutineScope.launch(IO) {
            block.invoke(this)
        }
    }
}