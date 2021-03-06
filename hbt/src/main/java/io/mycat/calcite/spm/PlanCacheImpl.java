//package io.mycat.calcite.spm;
//
//import com.alibaba.druid.DbType;
//import com.alibaba.druid.sql.SQLUtils;
//import lombok.SneakyThrows;
//import org.apache.calcite.sql.type.SqlTypeName;
//import org.apache.commons.io.FileUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardOpenOption;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.Supplier;
//
//public class PlanCacheImpl implements PlanCache {
//
//    private ConcurrentHashMap<Key, Plan> cache;
//    final static Logger log = LoggerFactory.getLogger(PlanCacheImpl.class);
//
//    @SneakyThrows
//    public PlanCacheImpl() {
//        this.cache = new ConcurrentHashMap<>();
//    }
//
//    public Plan getMinCostPlan(String sql,Supplier<Plan> planSupplier) {
//        Key key = new Key(sql);
//        Plan plan = computeIfAbsent(key);
//        if (plan != null) {
//            return plan;
//        }
//        synchronized (this) {
//            long hash = SQLUtils.hash(sql, DbType.mysql);
//            Path file = path.resolve(String.valueOf(hash));
//            KeyGroup keyGroup;
//            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Files.readAllBytes(file));
//                 ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
//                keyGroup = (KeyGroup) objectInputStream.readObject();
//            } catch (Throwable throwable) {
//                log.error("", throwable);
//                keyGroup = new KeyGroup(hash);
//            }
//            plan = keyGroup.map.get(key);
//            if (plan == null) {
//                plan = planSupplier.get();
//                put(sql, types, plan);
//            }
//        }
//        return plan;
//    }
//
//    @SneakyThrows
//    private Plan computeIfAbsent(Key sql) {
//        return this.cache.getOrDefault(sql, null);
//    }
//
//
//    @SneakyThrows
//    public void put(String sql, List<SqlTypeName> types, Plan update) {
//        synchronized (this) {
//            Key key = new Key(sql, types);
//            long hash = SQLUtils.hash(sql, DbType.mysql);
//            Path file = path.resolve(String.valueOf(hash));
//            KeyGroup keyGroup;
//            Plan plan = computeIfAbsent(key);
//            if (!Files.exists(file)) {
//                keyGroup = new KeyGroup(hash);
//            } else {
//                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Files.readAllBytes(file));
//                     ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
//                    keyGroup = (KeyGroup) objectInputStream.readObject();
//                } catch (Throwable throwable) {
//                    log.error("", throwable);
//                    keyGroup = new KeyGroup(hash);
//                }
//            }
//            if (plan != null) {
//                if (plan.compareTo(update) <= 0) {
//                    save(key, keyGroup, plan);
//                }
//            }
//        }
//    }
//
//    private void save(Key key, KeyGroup keyGroup, Plan plan) {
//        cache.put(key, plan);
//        keyGroup.map.put(key, plan);
//        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//             ObjectOutputStream objectInputStream = new ObjectOutputStream(outputStream);) {
//            objectInputStream.writeObject(keyGroup);
//            Files.write(path, outputStream.toByteArray(), StandardOpenOption.WRITE);
//        } catch (Throwable throwable) {
//            log.error("", throwable);
//        }
//    }
//
//    public void clear() {
//        cache.clear();
//        try {
//            FileUtils.cleanDirectory(path.toFile());
//        } catch (IOException e) {
//            log.error("", e);
//        }
//    }
//}
//                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      