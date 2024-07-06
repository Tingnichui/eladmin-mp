#!/bin/bash
  
# 定义启动jar包
JAR_NAME="eladmin-system-1.1.jar"
# 定义脚本所在决定路径
BASE_PATH=$(cd `dirname $0`; pwd)
ENV="prod"

# 设置java配置参数
JAR_CONFS="--spring.profiles.active=${ENV}"
JAR_CONFS="$JAR_CONFS --logback.logpath=${BASE_PATH}/logs"

# 设置jvm参数
JAVA_OPTS="-Xms512m -Xmx512m"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${BASE_PATH}/gc/`date +'%Y-%m-%d_%H-%M-%S'`_heapdump.hprof"


# 检查启动 JAR 是否存在
check_jar() {
    if [ ! -f "${BASE_PATH}/${JAR_NAME}" ]; then
        echo "${JAR_NAME} 启动包不存在"
        exit 1
    fi
}

# 停止服务
stop() {
    status
    if [ -n "$pid" ]; then
        echo "Stopping existing process with PID $pid"

        mkdir -p "${BASE_PATH}/stack"
        mkdir -p "${BASE_PATH}/gc"
        jstack -l ${pid} > ${BASE_PATH}/stack/`date +'%Y-%m-%d_%H-%M-%S'`_${pid}.stack
        kill -9 "$pid"
        sleep 3
        echo "$JAR_NAME stopped successfully."
    else
        echo "$JAR_NAME has stopped."
    fi
}

# 启动服务
start() {
    check_jar
    status
    if [ -n "$pid" ]; then
        echo "$JAR_NAME has started."
        exit 1
    fi

    #nohup java -jar "$JAR_NAME" > /dev/null 2>&1 &
    #nohup java $JAVA_OPTS -jar "${BASE_PATH}/$JAR_NAME" $JAR_CONFS > ${BASE_PATH}/nohup.out 2>&1 &
    nohup java $JAVA_OPTS -jar "${BASE_PATH}/$JAR_NAME" $JAR_CONFS > /dev/null 2>> ${BASE_PATH}/error.out &
    # 启动后睡眠3秒
    sleep 3
    # 启动成功
    status
    if [ -n "$pid" ]; then
        echo "$JAR_NAME start successfully."
    else
        echo -e "\033[1;31m $JAR_NAME is not running! \033[0m"
    fi
}

# 重新启动
restart() {
    stop
    start
}

status() {
    # 使用绝对路劲寻找pid，防止出错
    pid=$(ps -ef | grep "${BASE_PATH}/$JAR_NAME" | grep -v grep | awk '{print $2}')
    if [ -n "$pid" ]; then
        echo "$JAR_NAME running PID is $pid."
    fi
}

# 停止服务
force_stop() {
    status
    if [ -n "$pid" ]; then
        echo "Stopping existing process with PID $pid"

        mkdir -p "${BASE_PATH}/stack"
        jstack -l ${pid} > ${BASE_PATH}/stack/`date +'%Y-%m-%d_%H-%M-%S'`_${pid}.stack

        mkdir -p "${BASE_PATH}/gc"
        jmap -dump:format=b,file=${BASE_PATH}/gc/`date +'%Y-%m-%d_%H-%M-%S'`_${pid}.hprof ${pid}

        kill -9 "$pid"
        sleep 3
        echo "$JAR_NAME stopped successfully."
    else
        echo "$JAR_NAME has stopped."
    fi
}

#使用说明，用来提示输入参数
usage() {
  echo "Usage: sh restart.sh [start|stop|restart|status|health]"
  exit 1
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
 "start")
  start
  ;;
 "stop")
  stop
  ;;
 "restart")
  restart
  ;;
 "status")
  status
  ;;
 "down")
  down
  ;;
 "up")
  up
  ;;
 "health")
  health
  ;;
 "force_stop")
  force_stop
  ;;
 *)
  usage
  ;;
esac