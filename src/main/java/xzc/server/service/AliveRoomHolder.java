package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.UserInfo;
import xzc.server.constant.RedisKey;
import xzc.server.constant.RoomState;
import xzc.server.exception.XzcException;
import xzc.server.proto.common.ErrorCode;
import xzc.server.proto.room.RoomMember;
import xzc.server.proto.room.RoomType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private OptionalRoomCache optionalRoomCache;

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

    public void saveAliveRoom(AliveRoom aliveRoom) {
        redisTemplate.opsForValue().set(getAliveRoomKey(aliveRoom.getRoomId()), aliveRoom);
    }

    public AliveRoom getOrCreateRoom(RoomType roomType) {
        // 获取一个可用的房间
        List<Pair<Long, Integer>> list = optionalRoomCache.getList(roomType);
        if (CollectionUtils.isEmpty(list)) {
            // 创建新房间
            return createRoom(roomType);
        }
        // 选第一个
        return getAliveRoom(list.get(0).getLeft());
    }

    public AliveRoom getAndJoinRoomRetry(RoomType roomType, UserInfo userInfo, int retryTime) {
        // 获取一个可用的房间
        List<Pair<Long, Integer>> list = optionalRoomCache.getList(roomType);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        for (int i = 0; i < list.size() && i < retryTime; i++) {
            Pair<Long, Integer> pair = list.get(i);
            try {
                AliveRoom aliveRoom = join(pair.getLeft(), userInfo);
                // 从可选房间列表中为该房间减少一个座位并判断是否需要移除
                if (aliveRoom.getMaxPlayNum() == aliveRoom.getMembersMap().size()) {
                    optionalRoomCache.remove(aliveRoom.getRoomType(), aliveRoom.getRoomId());
                } else {
                    optionalRoomCache.decr(aliveRoom.getRoomType(), aliveRoom.getRoomId());
                }
                return aliveRoom;
            } catch (Exception e) {
                // 加入失败，重试
                log.warn("尝试加入失败，房间已满，重试，index: {}", i, e);
            }
        }
        return null;
    }

    public AliveRoom createAndJoinRoom(RoomType roomType, UserInfo userInfo) throws Exception {
        // 创建房间
        AliveRoom room = createRoom(roomType);
        // 加入房间
        return firstJoin(room, userInfo);
    }

    private AliveRoom createRoom(RoomType roomType) {
        long roomId = idService.snowflakeNextId();
        return new AliveRoom()
                .setRoomId(roomId)
                .setRoomType(roomType)
                .setState(RoomState.OPENED)
                .setMembersMap(new LinkedHashMap<>());
    }

    public AliveRoom firstJoin(AliveRoom aliveRoom, UserInfo userInfo) throws Exception {
        long joinTime = System.currentTimeMillis();

        Long roomId = aliveRoom.getRoomId();
        // 递增
        Integer joinedIncr = aliveRoom.getJoinedIncr();
        AliveRoom.Member member = userInfoToMember(userInfo)
                .setIndex(joinedIncr++)
                .setEvent(AliveRoom.MemberEvent.JOIN)
                .setState(AliveRoom.MemberState.IDLE)
                .setJoinTime(joinTime)
                .setLastStateChange(joinTime);
        aliveRoom.getMembersMap().put(member.getUid(), member);
        aliveRoom.setJoinedIncr(joinedIncr);

        saveAliveRoom(aliveRoom);
        // 保存到一个可选房间集合中，比如建一个zset，使用剩余座位数为score
        optionalRoomCache.add(aliveRoom.getRoomType(), roomId, aliveRoom.getMaxPlayNum() - 1);
        return aliveRoom;
    }

    public AliveRoom join(Long roomId, UserInfo userInfo) throws Exception {
        return handleWithAliveRoomLock(roomId, new WithAliveRoomCallable<AliveRoom>(roomId, userInfo.getUid()) {

            @Override
            protected AliveRoom innerCall() throws Exception {
                if (aliveRoom.getMembersMap().size() > aliveRoom.getMaxPlayNum()) {
                    throw new XzcException(ErrorCode.ERROR_CODE_ROOM_IS_FULL, "房间已满");
                }
                if (aliveRoom.getState() != RoomState.OPENED) {
                    throw new XzcException(ErrorCode.ERROR_CODE_ROOM_IS_NOT_EXISTS, "房间已不存在");
                }
                long joinTime = System.currentTimeMillis();

                Integer joinedIncr = aliveRoom.getJoinedIncr();
                AliveRoom.Member member = userInfoToMember(userInfo)
                        .setIndex(joinedIncr++)
                        .setEvent(AliveRoom.MemberEvent.JOIN)
                        .setState(AliveRoom.MemberState.IDLE)
                        .setJoinTime(joinTime)
                        .setLastStateChange(joinTime);
                aliveRoom.getMembersMap().put(member.getUid(), member);
                aliveRoom.setJoinedIncr(joinedIncr);

                return aliveRoom;
            }
        });
    }

    public AliveRoom ready(Long roomId, UserInfo userInfo, boolean readyOrCancel) throws Exception {
        return handleWithAliveRoomLock(roomId, new WithAliveRoomCallable<AliveRoom>(roomId, userInfo.getUid()) {

            @Override
            protected AliveRoom innerCall() throws Exception {
                // 修改状态为已准备
                AliveRoom.Member member = aliveRoom.getMembersMap().get(operator);
                member.setEvent(readyOrCancel ? AliveRoom.MemberEvent.READY : AliveRoom.MemberEvent.CANCEL_READY)
                        .setState(readyOrCancel ? AliveRoom.MemberState.PLAYING : AliveRoom.MemberState.IDLE);
                return aliveRoom;
            }
        });
    }

    public AliveRoom start(Long roomId, Long uid) throws Exception {
        return handleWithAliveRoomLock(roomId, new WithAliveRoomCallable<AliveRoom>(roomId, uid) {

            @Override
            protected AliveRoom innerCall() throws Exception {
                // 判断房间人数是否满足条件
                LinkedHashMap<Long, AliveRoom.Member> membersMap = aliveRoom.getMembersMap();
                // 修改状态为游戏中
                for (Map.Entry<Long, AliveRoom.Member> entry : membersMap.entrySet()) {
                    AliveRoom.Member member = entry.getValue();
                    if (member.getState() != AliveRoom.MemberState.PLAYING) {
                        throw new XzcException(ErrorCode.ERROR_CODE_HAS_MEMBER_NOT_READY, "有玩家没准备或游戏中，uid: " + entry.getKey() + ", state: " + member.getState());
                    }
                    member.setEvent(AliveRoom.MemberEvent.START)
                            .setState(AliveRoom.MemberState.IN_PLAY);
                }
                aliveRoom.setState(RoomState.IN_GAME);
                return aliveRoom;
            }
        });
    }

    public AliveRoom quit(Long roomId, Long uid) throws Exception {
        return handleWithAliveRoomLock(roomId, new WithAliveRoomCallable<AliveRoom>(roomId, uid) {

            @Override
            protected AliveRoom innerCall() throws Exception {
                LinkedHashMap<Long, AliveRoom.Member> membersMap = aliveRoom.getMembersMap();
                if (membersMap == null
                        || membersMap.get(operator) == null
                        || membersMap.get(operator).getState() != AliveRoom.MemberState.IDLE) {
                    throw new XzcException(ErrorCode.ERROR_CODE_MEMBER_STATE_NOT_IS_IDLE, "玩家不在Idle状态，" + operator);
                }
                membersMap.remove(operator);
                if (membersMap.size() == 0) {
                    aliveRoom.setState(RoomState.CLOSED);
                }
                return aliveRoom;
            }
        });
    }

    public RoomMember userInfoToRoomMember(UserInfo userInfo) {
        return RoomMember.newBuilder()
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


    public <T> T handleWithAliveRoomLock(long roomId, Callable<T> handleImpl) throws Exception {
        RLock lock = redissonClient.getLock(getAliveRoomLockKey(roomId));
        long timeout = 5000L;
        if (!lock.tryLock(timeout, timeout, TimeUnit.MILLISECONDS)) {
            //that should not happen.
            log.warn("Try get session lock timeout. {}", roomId);
            throw new RuntimeException("Get session lock timeout. Please try again");
        }
        try {
            // todo 使用线程池
            return handleImpl.call();
        } finally {
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

        //判断本次操作是否有修改，当true时，自动保存此aliveRoom
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
                    return null;
                } else {
                    throw e;
                }
            } finally {
                if (changed) {
                    // 保存修改后的aliveRoom
                    aliveRoom.setLastChangeTime(System.currentTimeMillis());
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
