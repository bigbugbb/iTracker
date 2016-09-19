package com.itracker.android.utils;


import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import static com.itracker.android.utils.LogUtils.makeLogTag;


public class AppQueryHandler extends AsyncQueryHandler {
    private final static String TAG = makeLogTag(AppQueryHandler.class);

    public AppQueryHandler(ContentResolver cr) {
        super(cr);
    }

    /**
     * Thin wrapper around {@link AsyncQueryHandler.WorkerHandler} that catches any <code>RuntimeException</code>
     * thrown and passes them back in a reply message. The exception that occurred is
     * in the <code>result</code> field.
     */
    protected class SturdyWorkerHandler extends WorkerHandler {
        public SturdyWorkerHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
            } catch (RuntimeException x) {
                // Pass the exception back to the calling thread (will be in args.result)
                WorkerArgs args = (WorkerArgs) msg.obj;
                Message reply = args.handler.obtainMessage(msg.what);
                args.result = x;
                reply.obj = args;
                reply.arg1 = msg.arg1;
                reply.sendToTarget();
            }
        }
    }

    @Override
    protected Handler createHandler(Looper looper) {
        return new SturdyWorkerHandler(looper);
    }

    /**
     * Called when a runtime exception occurred during the asynchronous operation.
     * <p>
     * The default re-throws the exception
     * @param token - The token that was passed into the operation
     * @param cookie - The cookie that was passed into the operation
     * @param error - The <code>RuntimeException</code> that was thrown during
     * the operation
     */
    protected void onError(int token, Object cookie, RuntimeException error) {
        throw error;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.obj instanceof WorkerArgs) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            if (args.result instanceof RuntimeException) {
                onError(msg.what, args.cookie, (RuntimeException) args.result);
                return;
            }
        }
        super.handleMessage(msg);
    }
}