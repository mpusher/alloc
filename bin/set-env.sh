#!/usr/bin/env bash
#开启远程调试
#JVM_FLAGS="$JVM_FLAGS -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8008"

#GC配置
#运行模式 整个堆内存大小 GC算法
#JVM_FLAGS="$JVM_FLAGS -server -Xmx1024m -Xms1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
#GC日志 发生OOM时创建堆内存转储文件
#JVM_FLAGS="$JVM_FLAGS -Xloggc:$MP_LOG_DIR/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
#发生OOM后的操作
#JVM_FLAGS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$MP_LOG_DIR -XX:OnOutOfMemoryError=$MP_BIN_DIR/restart.sh"