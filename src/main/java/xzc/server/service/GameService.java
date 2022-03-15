package xzc.server.service;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.bean.AliveRoom;
import xzc.server.bean.UserInfo;
import xzc.server.constant.Card;
import xzc.server.proto.ChangeXzcCardRequest;
import xzc.server.proto.DiscardRequest;
import xzc.server.proto.TakeCardRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameService {


    @Autowired
    private IdService idService;

    @Autowired
    private AliveGameHolder aliveGameHolder;

    public AliveGame create(Map<Long, AliveRoom.Member> memberMap) {
        // 玩家按照加入房间顺序排序
        List<Long> memberIds = memberMap.entrySet().stream()
                .sorted((m1, m2) -> m2.getValue().getIndex().compareTo(m1.getValue().getIndex()))
                .map(member -> member.getValue().getUid())
                .collect(Collectors.toList());
        // 初始化一个game,
        AliveGame aliveGame = new AliveGame()
                // 生成ID
                .setId(idService.snowflakeNextId())
                // 玩家序列
                .setOrderlyGamers(memberIds)
                // 当前玩家
                .setLastChangeTime(System.currentTimeMillis());
        // 给玩家发牌
        int pointer = aliveGame.getCardLibraryIndex();
        Map<Long, AliveGame.Gamer> gamerMap = Maps.newHashMapWithExpectedSize(memberMap.size());
        for (Long memberId : memberIds) {
            AliveRoom.Member member = memberMap.get(memberId);
            AliveGame.Gamer gamer = new AliveGame.Gamer()
                    .setUid(memberId)
                    .setNickname(member.getNickname())
                    .setAvatar(member.getAvatar())
                    // 发牌
                    .setHandCard(aliveGame.getCardLibrary().get(pointer))
                    .setJoinTime(System.currentTimeMillis());
            pointer++;
            gamerMap.put(memberId, gamer);
        }
        aliveGame.setGamerMap(gamerMap)
                // 给小早川发牌
                .setXzcCard(aliveGame.getCardLibrary().get(pointer))
                .setCardLibraryIndex(pointer + 1);
        aliveGameHolder.saveGame(aliveGame);
        return aliveGame;
    }

    public void takeCard(UserInfo userInfo, TakeCardRequest takeCardRequest) throws Exception {
        long gameId = takeCardRequest.getGameId();
        AliveGame aliveGame = aliveGameHolder.takeCard(gameId, userInfo.getUid());
        // TODO: 2022/3/14 响应客户端，

        // TODO: 2022/3/14 通知其他玩家
    }

    public void discard(UserInfo userInfo, DiscardRequest discardRequest) throws Exception {
        long gameId = discardRequest.getGameId();
        Card card = Card.of(discardRequest.getCard());
        AliveGame aliveGame = aliveGameHolder.discard(gameId, userInfo.getUid(), card);
        // TODO: 2022/3/14 响应客户端，

        // TODO: 2022/3/14 通知其他玩家
    }

    public void changeXzcCard(UserInfo userInfo, ChangeXzcCardRequest changeXzcCardRequest) throws Exception {
        long gameId = changeXzcCardRequest.getGameId();
        AliveGame aliveGame = aliveGameHolder.changeXzcCard(gameId, userInfo.getUid());
        // TODO: 2022/3/14 响应客户端，

        // TODO: 2022/3/14 通知其他玩家
    }

}
