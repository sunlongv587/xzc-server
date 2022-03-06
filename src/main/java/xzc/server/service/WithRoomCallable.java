package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import xzc.server.bean.AliveRoom;

import java.util.concurrent.Callable;

@Slf4j
public abstract class WithRoomCallable<T> implements Callable<T> {


    public WithRoomCallable(Long roomId, Long operator) {
        this.operator = operator;
//        this.aliveSessionHandler = aliveSessionHandler;
        this.roomId = roomId;
    }

    protected Long roomId;
    protected AliveRoom aliveRoom;
    protected Long operator;
//    protected Permission permission;
//    protected AliveSessionHandler aliveSessionHandler;

    //判断本次操作是否有修改，当true时，自动保存此aliveSession
    protected boolean change = true;

    //默认抛出异常，如果不想抛异常，做忽略处理，则调用setIgnoreException置为true
    protected boolean ignoreException = false;

    public void setIgnoreException(boolean ignoreException) {
        this.ignoreException = ignoreException;
    }

    @Override
    public final T call() throws Exception {
        try {
            beforeCall();
            return innerCall();
        } catch (Exception e) {
            //一旦发生异常，不保存session
            change = false;
            if (ignoreException) {
                log.info("Handle room is exception, room id：{}！", roomId, e);
                return null;
            } else {
                throw e;
            }
        } finally {
//            if (change) {
//                //保存修改后的aliveSession
//                aliveSession.setLastChangeTime(System.currentTimeMillis());
//                aliveSessionHandler.saveSession(aliveSession);
//            }
        }
    }

    protected void beforeCall() throws Exception {

    }

    protected abstract T innerCall() throws Exception;
}
