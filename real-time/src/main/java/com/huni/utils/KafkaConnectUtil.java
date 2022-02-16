package com.huni.utils;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;

/**
 * @author huni
 * @Classname KafkaSource
 * @Description 获取kafka链接工具类，包括sink,source
 * @Date 2022/2/15 11:57
 */
public class KafkaConnectUtil {
    private static String brokers = "linux121:9092,linux122:9092,linux123:9092";

    public static FlinkKafkaConsumer getFlinkKafkaConsumer(String topic,String groupId) {
        FlinkKafkaConsumer consumer=null;
        try{
            //2.读取kafka数据源 配置kafka信息
            Properties props = new Properties();
            props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,brokers);
            props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);
            consumer = new FlinkKafkaConsumer(topic,new SimpleStringSchema(),props);
            //从当前消费组记录的偏移量开始，接着上次的偏移量消费
            consumer.setStartFromGroupOffsets();
            //自动提交offset
            consumer.setCommitOffsetsOnCheckpoints(true);
        }catch (Exception e){
            e.printStackTrace();
        }
        return consumer;
    }

    /**
     * 获取kafka生产者
     * @param topic
     * @return
     */
    public static FlinkKafkaProducer getFlinkKafkaProducer(String topic) {

        return  new FlinkKafkaProducer(brokers,topic,new SimpleStringSchema());
    }
}
