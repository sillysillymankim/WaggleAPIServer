package io.waggle.waggleapiserver.common.storage.event

import io.waggle.waggleapiserver.common.storage.StorageClient
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ImageDeleteEventListener(
    private val storageClient: StorageClient,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: ImageDeleteEvent) {
        storageClient.delete(event.imageUrl)
    }
}
