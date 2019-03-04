package com.bgu.agent.base.operations;

import android.os.AsyncTask;
import com.bgu.agent.base.IDataListener;
import com.bgu.agent.commons.IRunnableWithParameters;
import com.bgu.agent.commons.logging.Logger;
import com.bgu.agent.data.exceptions.CongeorException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 6:23 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractDataOperation<T> implements IDataOperation {
    protected int status;
    String error = null;

    protected void onSuccess (Object ... params){
        status = 0;
        Logger.d(this.getClass(), "Operation completed successfully");
    }

    protected void onFail(Object ... params){
        CongeorException error = ( CongeorException ) params [ 0 ];
        status = error.getStatusCode();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        pw.flush();
        pw.close();
        this.error = sw.toString();
        Logger.e(this.getClass(), error.toString() + "\n\n" + this.error);
    }

    protected void onPostRun ( IRunnableWithParameters runnable, IDataListener listener, T returnVal, Object ... params ){
        if ( runnable != null ){
            if ( status == 0 )
                runnable.run(listener, status, returnVal, params);
            else
                runnable.run(listener, status, this.error );
        }
    }

    protected abstract class AsyncOperation extends AsyncTask<Object, Void, T> {
        protected WeakReference<IDataListener> dataListener;
        protected IRunnableWithParameters runnable;

        public AsyncOperation(IDataListener listener, IRunnableWithParameters runnable) {
            this.dataListener = new WeakReference<IDataListener>(listener);
            if ( runnable != null )
                this.runnable = runnable;
            else
                this.runnable = null;
        }

        protected void setThreadName() {
            Thread.currentThread().setName(this.getClass().getCanonicalName());
        }

        @Override
        protected void onPostExecute(T returnVal) {
            onPostRun(runnable, dataListener.get(), returnVal);
        }
    }
}
