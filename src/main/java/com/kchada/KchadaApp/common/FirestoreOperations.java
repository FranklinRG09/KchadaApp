package com.kchada.KchadaApp.common;

import com.google.api.core.ApiFuture;

import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class FirestoreOperations {

    public <T> T await(ApiFuture<T> future) {

        try {
            return future.get();

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Operación interrumpida",
                    exception
            );

        } catch (ExecutionException exception) {

            Throwable cause = exception.getCause();

            if (cause instanceof ApiException apiException) {
                throw apiException;
            }

            throw new IllegalStateException(
                    "Error al realizar una operación en Firestore",
                    exception
            );
        }
    }
}
