package xzc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xzc.server.util.SnowflakeIdWorker;

@Slf4j
@Service
public class IdService {

    private long counter = 0;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    public long snowflakeNextId() throws RuntimeException {
        long nextId = snowflakeIdWorker.nextId();
//        if (ThreadLocalRandom.current().nextLong() % 1000 == 0) {
//            log.info("generate nextId: {}.", nextId);
//        }
        return nextId;
    }

    public long[] snowflakeNextIds(int count) throws RuntimeException {
        // 不需要精确，并发没有问题
        counter++;
//        if (counter % 100 == 0) {
//            log.info("nextIds, count: {}.", count);
//        }

        return snowflakeIdWorker.nextIds(count);
    }

}
