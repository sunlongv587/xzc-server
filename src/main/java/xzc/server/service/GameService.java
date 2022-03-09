package xzc.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.bean.AliveGame;
import xzc.server.bean.AliveRoom;
import xzc.server.util.CardUtil;

import java.util.List;

@Service
public class GameService {


    @Autowired
    private IdService idService;

    @Autowired
    private AliveGameHolder aliveGameHolder;

    private AliveGame create(List<AliveRoom.Member> members) {


        AliveGame aliveGame = new AliveGame()
                // 洗牌
                .setCardHouse(CardUtil.getCardHouse());
        // TODO: 2022/3/9 初始化一个game,
        // TODO: 2022/3/9 发牌


//                .set
//                .setGamerMap()

        return aliveGame;
    }

}
