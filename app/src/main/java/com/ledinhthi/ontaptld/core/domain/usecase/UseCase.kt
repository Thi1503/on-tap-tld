package com.ledinhthi.ontaptld.core.domain.usecase

import kotlinx.coroutines.flow.Flow

/** 1 use case = 1 hành động nghiệp vụ. Gọi bằng `useCase(input)`. */
abstract class UseCase<in Input, out Output> {
    abstract suspend operator fun invoke(input: Input): Output
}

abstract class NoInputUseCase<out Output> {
    abstract suspend operator fun invoke(): Output
}

/**
 * Use case trả luồng dữ liệu observe được (đọc Room). Không `suspend` — trả `Flow`.
 * App offline-first: phần lớn "đọc" là stream, không phải request 1 lần.
 */
abstract class FlowUseCase<in Input, out Output> {
    abstract operator fun invoke(input: Input): Flow<Output>
}

abstract class NoInputFlowUseCase<out Output> {
    abstract operator fun invoke(): Flow<Output>
}
