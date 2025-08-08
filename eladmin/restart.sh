#!/bin/bash
  
# 定义启动jar包
JAR_NAME="eladmin-system-1.1.jar"
# 定义脚本所在决定路径
BASE_PATH=$(cd `dirname $0`; pwd)
ENV="prod"

# 设置java配置参数
JAR_CONFS=""
JAR_CONFS="--spring.profiles.active=${ENV}"
JAR_CONFS="$JAR_CONFS --logback.logpath=${BASE_PATH}/logs"
JAR_CONFS="$JAR_CONFS --eladmin.health.sign=fd660d06fb3fd8851f0"
JAR_CONFS="$JAR_CONFS --jasypt.encryptor.password=${JASYPT_ENCRYPTOR_PASSWORD}"

# 设置jvm参数
JAVA_OPTS=""
#JAVA_OPTS="-Xms2048m -Xmx2048m"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${BASE_PATH}/gc/`date +'%Y-%m-%d_%H-%M-%S'`_heapdump.hprof"

HEALTH_FLAG=true
HEALTH_CHECK_URL="http://127.0.0.1:11280/api/health/healthCheck"
CHANGE_HEALTH_URL="http://127.0.0.1:11280/api/health/modifyHealth?sign=fd660d06fb3fd8851f0&healthType="
HEALTH_DOWN_TIME=30


# 检查启动 JAR 是否存在
check_jar() {
    if [ ! -f "${BASE_PATH}/${JAR_NAME}" ]; then
        echo "${JAR_NAME} 启动包不存在"
        exit 1
    fi
}

# 修改服务健康状态
change_health_status() {
  if [ "$HEALTH_FLAG" = false ]; then
    echo "Health check is disabled."
    return 0
  fi

  if [ -z "$1" ]; then
    echo "Health status type is required."
    exit 1
  fi
  # 修改服务的健康状态
  change_response=$(curl -l -m 10 -o /dev/null -s -w "%{http_code}" "${CHANGE_HEALTH_URL}$1")
  if [ "$change_response" -eq 200 ]; then
    echo "Change health status to $1 success."
  else
    echo "Change failed with response code: $change_response."
    echo "Cancel stop service operation."
    exit 1;
  fi
}

down() {
  change_health_status "DOWN"
}

up() {
  change_health_status "UP"
}

# 判断服务是否正常
health() {
  if [ "$HEALTH_FLAG" = false ]; then
    echo "Health check is disabled."
    return 0
  fi
  # 循环调用接口查看服务是否正常
  for i in {1..60} ; do
    health_response=$(curl -l -m 10 -o /dev/null -s -w "%{http_code}" "$HEALTH_CHECK_URL")
    if [ "$health_response" -eq 200 ]; then
      echo "Service Health."
      exit 0
    else
      echo "Service is not healthy, response code: $health_response. Retry $i/60."
      sleep 3
    fi
  done
  echo "Service did not become healthy after 60 attempts."
  exit 1
}


# 停止服务
stop() {
    status
    if [ -n "$pid" ]; then
        echo "Stopping existing process with PID $pid"

        # 如果启用了健康检查，则关闭服务健康状态并且停止相应时间
        if [ "$HEALTH_FLAG" = true ]; then
          # 修改服务健康状态
          down
          # 修改服务健康状态后等待100秒再停止服务
          echo "Wait for $HEALTH_DOWN_TIME seconds to stop the service after modifying its health status"
          sleep "$HEALTH_DOWN_TIME"
        fi

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
    nohup java $JAVA_OPTS -jar "${BASE_PATH}/$JAR_NAME" $JAR_CONFS >> ${BASE_PATH}/nohup.out 2>&1 &
    #nohup java $JAVA_OPTS -jar "${BASE_PATH}/$JAR_NAME" $JAR_CONFS > /dev/null 2>> ${BASE_PATH}/error.out &
    # 启动后睡眠3秒
    sleep 3
    # 启动成功
    status
    if [ -n "$pid" ]; then
        echo "$JAR_NAME start successfully."
        # 检查服务是否健康
        health
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
