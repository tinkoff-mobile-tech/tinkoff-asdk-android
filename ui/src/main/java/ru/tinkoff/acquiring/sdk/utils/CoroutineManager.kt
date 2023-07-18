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

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @author Mariya Chernyadieva
 */
// TODO нужны только диспы отсюда
internal class CoroutineManager(
    private val exceptionHandler: (Throwable) -> Unit,
    val io: CoroutineDispatcher = IO,
    val main: CoroutineDispatcher = Main
) {

    constructor(
        io: CoroutineDispatcher = IO,
        main: CoroutineDispatcher = Main
    ) : this({}, io, main)

    private val job = SupervisorJob()
    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable -> launchOnMain { exceptionHandler(throwable) } }
    private val coroutineScope = CoroutineScope(Main + coroutineExceptionHandler + job)
    private val disposableSet = hashSetOf<Disposable>()

    fun <R> call(
        request: Request<R>,
        onSuccess: (R) -> Unit,
        onFailure: ((Exception) -> Unit)? = null
    ) {
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

        return withContext(io) {
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

    fun launchOnMain(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch(main) {
            block.invoke(this)
        }
    }

    suspend fun withMain(block: suspend CoroutineScope.() -> Unit) {
        withContext(main) {
            block.invoke(this)
        }
    }

    fun launchOnBackground(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch(IO) {
            block.invoke(this)
        }
    }

    fun launchOnBackground(
        block: suspend CoroutineScope.() -> Unit,
        onError: (Throwable) -> Unit
    ): Job {
        return coroutineScope.launch(io) {
            try {
                block.invoke(this)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    onError(e)
                }
            }
        }
    }
}