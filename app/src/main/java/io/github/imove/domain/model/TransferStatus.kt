package io.github.imove.domain.model

enum class TransferStatus {
    QUEUED,
    TRANSFERRING,
    COMPLETED,
    SKIPPED,
    FAILED,
    CANCELLED
}
