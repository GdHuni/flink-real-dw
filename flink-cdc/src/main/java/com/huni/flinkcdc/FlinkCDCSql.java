package com.huni.flinkcdc;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

public class FlinkCDCSql {

    public static void main(String[] args) throws Exception {
        //1.建立环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);


        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        tableEnv.executeSql("CREATE TABLE mysql_binlog ( " +
                " id STRING NOT NULL, " +
                " tm_name STRING, " +
                " logo_url STRING " +
                ") WITH ( " +
                " 'connector' = 'mysql-cdc', " +
                " 'hostname' = '192.168.111.121', " +
                " 'port' = '3306', " +
                " 'username' = 'hive', " +
                " 'password' = '12345678', " +
                " 'database-name' = 'real_dw', " +
                " 'table-name' = 'base_trademark' " +
                ")");

        Table table = tableEnv.sqlQuery("select * from mysql_binlog");
        DataStream<Tuple2<Boolean, Row>> stream = tableEnv.toRetractStream(table, Row.class);
        stream.print();
        env.execute();

    }
}
