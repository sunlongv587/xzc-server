package xzc.server.service;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.bean.AliveRoom;
import xzc.server.constant.Card;
import xzc.server.util.CardUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
                // 洗牌
                .setCardHouse(CardUtil.getCardHouse())
                // 玩家序列
                .setOrderedGamerList(memberIds)
                // 起始玩家
                .setStartingGamer(memberIds.get(0))
                // 当前玩家
                .setCurGamerId(memberIds.get(0))
                .setLastChangeTime(System.currentTimeMillis());
        // 给玩家发牌
        List<Card> cardHouse = aliveGame.getCardHouse();
        Queue<Card> cardQueue = new LinkedList<>(cardHouse);
        Map<Long, AliveGame.Gamer> gamerMap = Maps.newHashMapWithExpectedSize(memberMap.size());
        for (Long memberId : memberIds) {
            AliveRoom.Member member = memberMap.get(memberId);
            AliveGame.Gamer gamer = new AliveGame.Gamer()
                    .setUid(memberId)
                    .setNickname(member.getNickname())
                    .setAvatar(member.getAvatar())
                    // 发牌
                    .setCard(cardQueue.poll())
                    .setJoinTime(System.currentTimeMillis());
            gamerMap.put(memberId, gamer);
        }
        aliveGame.setGamerMap(gamerMap)
                // 给小早川发牌
                .setXzcCard(cardQueue.poll());
        aliveGameHolder.saveGame(aliveGame);
        return aliveGame;
    }

}
