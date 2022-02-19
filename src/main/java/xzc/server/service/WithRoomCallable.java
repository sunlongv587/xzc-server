package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import xzc.server.bean.AliveRoom;

import java.util.concurrent.Callable;

@Slf4j
public abstract class WithRoomCallable<T> implements Callable<T> {


    public WithRoomCallable(Long sessionId, Long operator) {
        this.operator = operator;
//        this.aliveSessionHandler = aliveSessionHandler;
        this.sessionId = sessionId;
    }

    protected Long sessionId;
    protected AliveRoom aliveSession;
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
                log.info("处理{}课堂session 发生异常，忽略此异常内容：{}！", sessionId, e.getMessage());
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
//        aliveSession = aliveSessionHandler.isFinishedSession(sessionId);
//        // 判定权限，permission 为空不需要判定。
//        if (permission != null && operator != null) {
//            List<AliveSession.Members> alivedList = aliveSession.getAlivedList();
//            if (!alivedList.stream().map(AliveSession.Members::getUid).collect(Collectors.toList()).contains(operator)) {
//                throw new NeukoException(ErrorCode.NOT_IN_MEETING);
//            }
//            Optional<AliveSession.Members> memberOptional = alivedList.stream().filter(e -> operator.equals(e.getUid())).findFirst();
//            if (memberOptional.isPresent() && memberOptional.get().getRoleEnum() != null) {
//                boolean match = memberOptional.get().getRoleEnum().getPermissionSet().contains(permission);
//                if (!match) {
//                    log.info("用户: {}, 没有权限操作: {}，Session: {}", operator, permission, aliveSession.getSessionId());
//                    throw new NeukoException(ErrorCode.UNAUTHORIZED_OPERATION);
//                }
//            } else {
//                log.info("用户: {}, 没有权限操作: {}，Session: {}", operator, permission, aliveSession.getSessionId());
//                throw new NeukoException(ErrorCode.UNAUTHORIZED_OPERATION);
//            }
//        }
    }

    protected abstract T innerCall() throws Exception;
}
