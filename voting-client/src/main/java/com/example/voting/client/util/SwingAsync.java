// voting-client/src/main/java/com/example/voting/client/util/SwingAsync.java
package com.example.voting.client.util;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class SwingAsync {
    private SwingAsync() {}

    public static <T> void run(JFrame owner, Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        owner.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                owner.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                try {
                    T r = get();
                    onSuccess.accept(r);
                } catch (Exception e) {
                    onError.accept(e);
                }
            }
        };
        worker.execute();
    }
}
