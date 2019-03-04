package com.bgu.agent.base.operations;

import com.bgu.agent.base.IDataListener;
import com.bgu.agent.commons.IRunnableWithParameters;

import java.util.concurrent.Executor;

/**
 * Created with IntelliJ IDEA.
 * User: clint
 * Date: 12/3/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IDataOperation {
    void performOperation(IDataListener listener, IRunnableWithParameters runnable, Executor ... executors);
}
