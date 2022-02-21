package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.UserInfo;
import xzc.server.constant.RedisKey;
import xzc.server.constant.RoomState;
import xzc.server.proto.Participant;
import xzc.server.proto.ParticipantEvent;
import xzc.server.proto.ParticipantState;
import xzc.server.proto.RoomType;

import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AliveRoomHolder {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IdService idService;

    public String getAliveRoomKey(Long roomId) {
        return RedisKey.makeKey(RedisKey.ALIVE_ROOM, roomId);
    }

    public String getAliveRoomLockKey(Long roomId) {
        return RedisKey.makeKey(RedisKey.ALIVE_ROOM_LOCK, roomId);
    }

    public String getOptionalRoomsKey(RoomType roomType) {
        return RedisKey.makeKey(RedisKey.ALIVE_ROOM, roomType.name());
    }

    public AliveRoom getAliveRoom(Long roomId) {
        return (AliveRoom) redisTemplate.opsForValue().get(getAliveRoomKey(roomId));
    }

    // TODO: 2022/2/16 维护可使用的房间
    public void saveAliveRoom(AliveRoom aliveRoom) {
        redisTemplate.opsForValue().set(getAliveRoomKey(aliveRoom.getRoomId()), aliveRoom);
    }

    public AliveRoom getOrCreateRoom(RoomType roomType) {
        // todo 获取或者创建一个可用的房间
        return createRoom(roomType);
    }

    public AliveRoom createRoom(RoomType roomType) {
        long roomId = idService.snowflakeNextId();
        AliveRoom aliveRoom = new AliveRoom()
                .setRoomId(roomId)
                .setRoomType(roomType)
                .setState(RoomState.OPENED)
                .setMembersMap(new LinkedHashMap<>());
        // 保存房间
        saveAliveRoom(aliveRoom);
        // TODO: 2022/2/20 保存到一个可选房间集合中，比如建一个zset，使用剩余座位数为score
        return aliveRoom;
    }

    public void join(AliveRoom aliveRoom, UserInfo userInfo) throws Exception {
        handleWithAliveRoomLock(aliveRoom.getRoomId(), new WithAliveRoomCallable<Void>(aliveRoom, userInfo.getUid()) {

            @Override
            protected Void innerCall() throws Exception {
                long joinTime = System.currentTimeMillis();
                AliveRoom.Member member = userInfoToMember(userInfo)
                        .setEvent(AliveRoom.MemberEvent.JOIN)
                        .setState(AliveRoom.MemberState.IDLE);
                aliveRoom.getMembersMap().put(member.getUid(), member);
                return null;
            }
        });
    }

    public Participant userInfoToParticipant(UserInfo userInfo) {
        return Participant.newBuilder()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar())
                .build();
    }


    public AliveRoom.Member userInfoToMember(UserInfo userInfo) {
        return new AliveRoom.Member()
                .setUid(userInfo.getUid())
                .setNickname(userInfo.getNickname())
                .setAvatar(userInfo.getAvatar());
    }


    public <T> T handleWithAliveRoomLock(long roomId, Callable<T> handleImpl) throws Exception{
        RLock lock = redissonClient.getLock(getAliveRoomLockKey(roomId));
        long timeout = 5000L;
        if (!lock.tryLock(timeout, timeout, TimeUnit.MILLISECONDS)) {
            //that should not happen.
            log.warn("Try get session lock timeout. {}",roomId);
            throw new RuntimeException("Get session lock timeout. Please try again");
        }
        try {
            return handleImpl.call();
        }finally {
            lock.unlockAsync();
        }
    }

    abstract class WithAliveRoomCallable<T> implements Callable<T> {

        public WithAliveRoomCallable(Long roomId, Long operator) {
            this.operator = operator;
            this.roomId = roomId;
        }


        public WithAliveRoomCallable(AliveRoom aliveRoom, Long operator) {
            if (aliveRoom == null || aliveRoom.getRoomId() == null) {
                throw new IllegalArgumentException("Alive room can not be empty.");
            }
            this.operator = operator;
            this.roomId = aliveRoom.getRoomId();
            this.aliveRoom = aliveRoom;
        }

        protected Long roomId;
        protected AliveRoom aliveRoom;
        protected Long operator;

        //判断本次操作是否有修改，当true时，自动保存此aliveSession
        protected boolean changed = true;

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
                changed = false;
                if (ignoreException) {
                    log.warn("Handle alive room: {} operate is exception，ignore this：{}！", roomId, e);
                }
                return null;
            } finally {
                if (changed) {
                    //保存修改后的aliveSession
//                    aliveRoom.setLastChangeTime(System.currentTimeMillis());
                    saveAliveRoom(aliveRoom);
                }
            }
        }

        protected void beforeCall() throws Exception {
            if (this.aliveRoom == null) {
                this.aliveRoom = getAliveRoom(roomId);
            }

            if (aliveRoom == null) {
                log.error("Can not find alive room: {}", roomId);
                throw new NoSuchElementException("cannot find alive room " + roomId);
            }
        }

        protected abstract T innerCall() throws Exception;
    }

}
