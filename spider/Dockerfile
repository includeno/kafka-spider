FROM includeno/jdk11chrome:openjdk-11.0.13-jdkchrome99.0.4844.51

EXPOSE 8080

WORKDIR /app

#Version
ENV VERSION="1.0-SNAPSHOT"
ENV NAME="spider"

#Server
ENV server_port="8080"

#kafka
ENV kafka_server="127.0.0.1:9092"

#mysql
ENV datasource='jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC+8'
ENV datasource_username='root'
ENV datasource_password='root'

ENV spider_count="4"

#add jar file
ADD ${NAME}/target/${NAME}-${VERSION}.jar /app/application.jar
#ENTRYPOINT ["java","-jar","/app/application.jar","--dockerconfig=true"]
CMD java -jar /app/application.jar --server.port=${server_port} --spider.count=${spider_count} \
 --spring.kafka.bootstrap-servers=${kafka_server} \
 --spring.datasource.url="${datasource}" --spring.datasource.username=${datasource_username} --spring.datasource.password="${datasource_password}"