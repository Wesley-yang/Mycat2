package io.mycat.router.function;

import io.mycat.router.NodeIndexRange;
import io.mycat.router.RuleFunction;
import io.mycat.router.hashFunction.HashFunction;
import io.mycat.router.migrate.MigrateTask;
import io.mycat.router.migrate.MigrateUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

public class ConsistentHashPreSlot extends RuleFunction {

    public ConsistentHashPreSlot(String name, int defaultSlotsNum, HashFunction hashFunction) {
        this.name = name;
        this.DEFAULT_SLOTS_NUM = defaultSlotsNum;
        this.rangeMap2 = new int[defaultSlotsNum];
        this.hashFunction = hashFunction;
    }

    private final String name;
    private final int DEFAULT_SLOTS_NUM;
    private final int[] rangeMap2;
    private final HashFunction hashFunction;
    private List<List<NodeIndexRange>> longRanges;


    @Override
    public String name() {
        return name;
    }

    @Override
    public int calculate(String columnValue) {
        long hash = hashFunction.hash(columnValue);
        int slot = (int) (hash % DEFAULT_SLOTS_NUM);
        return rangeMap2[slot];
    }

    @Override
    public int[] calculateRange(String beginValue, String endValue) {
        return calculateRange(beginValue, endValue);
    }

    @Override
    public int getPartitionNum() {
        return rangeMap2.length;
    }

    public ConsistentHashBalanceExpandResult balanceExpand(List<String> oldDataNodes, List<String> newDataNodes) {
        List<List<NodeIndexRange>> copy = MigrateUtils.copy(longRanges);
        SortedMap<String, List<MigrateTask>> stringListSortedMap = MigrateUtils.balanceExpand(copy, oldDataNodes, newDataNodes, DEFAULT_SLOTS_NUM);
        MigrateUtils.merge(copy, stringListSortedMap);
        ConsistentHashPreSlot consistentHash = new ConsistentHashPreSlot(name, DEFAULT_SLOTS_NUM, hashFunction);
        consistentHash.init(Collections.emptyMap(), NodeIndexRange.from(copy));
        return new ConsistentHashBalanceExpandResult(stringListSortedMap, consistentHash);
    }

    @AllArgsConstructor
    @Getter
    public static class ConsistentHashBalanceExpandResult {
        SortedMap<String, List<MigrateTask>> migrateTaskMap;
        ConsistentHashPreSlot consistentHash;
    }

    @Override
    protected void init(Map<String, String> prot, Map<String, String> ranges) {
        String countText = prot.get("count");
        if (countText != null) {
            int count = Integer.parseInt(countText);
            int slotSize = DEFAULT_SLOTS_NUM / count;
            longRanges = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                if (i == count - 1) {
                    longRanges.add(new ArrayList<>(Collections.singletonList(new NodeIndexRange(i, i * slotSize, (DEFAULT_SLOTS_NUM - 1)))));
                } else {
                    longRanges.add(new ArrayList<>(Collections.singletonList(new NodeIndexRange(i, i * slotSize, ((i + 1) * slotSize - 1)))));
                }
            }
        } else {
            longRanges = NodeIndexRange.getSplitLongRanges(ranges);
        }
        for (List<NodeIndexRange> longRanges : longRanges) {
            for (NodeIndexRange longRange : longRanges) {
                int valueStart = (int) longRange.valueStart;
                int valueEnd = (int) longRange.valueEnd;
                int nodeIndex = longRange.nodeIndex;
                for (int i = valueStart; i <= valueEnd; i++) {
                    rangeMap2[i] = nodeIndex;
                }
            }
        }
    }
}