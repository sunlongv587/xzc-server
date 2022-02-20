package xzc.server.util;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Snowflake 算法 Java 版实现
 */
@Slf4j
public class SnowflakeIdWorker {
    /**
     * 以 2022-02-20 作为开端
     */
    private static final long TW_EPOCH = 1645347903580L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    private static final long SEQUENCE_BITS = 12L;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    private long lastTimestamp = -1L;
    private long sequence = 0;

    private final long workerId;
    private final long dataCenterId;

    public SnowflakeIdWorker(long workerId, long dataCenterId) {
        // sanity check for workerId
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }

        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException(
                String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATA_CENTER_ID));
        }

        this.workerId = workerId;
        this.dataCenterId = dataCenterId;

        log.info(
            "worker starting. timestamp left shift {}, datacenter id bits {}, worker id bits {}, sequence bits {}, workerid {}",
            TIMESTAMP_LEFT_SHIFT,
            DATA_CENTER_ID_BITS,
            WORKER_ID_BITS,
            SEQUENCE_BITS,
            workerId);
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public static long getTimestamp() {
        return System.currentTimeMillis();
    }

    public synchronized long nextId() {
        return nextId0();
    }

    public synchronized long[] nextIds(int count) {
        if (count < 0) {
            count = 1;
        }

        if (count > 200) {
            count = 200;
        }

        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = nextId0();
        }

        return ids;
    }

    private long nextId0() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            log.error("clock is moving backwards.  Rejecting requests until {}.", lastTimestamp);
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - TW_EPOCH) << TIMESTAMP_LEFT_SHIFT)
            | (dataCenterId << DATA_CENTER_ID_SHIFT)
            | (workerId << WORKER_ID_SHIFT)
            | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    public Map<String, String> parseId(long id) {
        String idStr = Long.toBinaryString(id);
        int len = idStr.length();

        int sequenceStart = (int) (len - WORKER_ID_SHIFT);
        int workerStart = (int) (len - DATA_CENTER_ID_SHIFT);
        int timeEnd = (int) (len - TIMESTAMP_LEFT_SHIFT);

        String sequence = idStr.substring(sequenceStart, len);
        String workerId = idStr.substring(workerStart, sequenceStart);
        String dataCenterId = idStr.substring(timeEnd, workerStart);
        String time = idStr.substring(0, timeEnd);
        long timestamp = Long.parseLong(time, 2) + TW_EPOCH;

        Map<String, String> result = new HashMap<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        result.put("date", simpleDateFormat.format(new Date(timestamp)));
        result.put("dataCenterId", Integer.valueOf(dataCenterId, 2).toString());
        result.put("workerId", Integer.valueOf(workerId, 2).toString());
        result.put("sequence", Long.valueOf(sequence, 2).toString());

        return result;
    }
}
