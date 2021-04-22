#!/bin/sh

docker run --rm -u gradle -v "$PWD":/home/gradle/project -w /home/gradle/project gradle:jdk16 gradle clean bootJar
docker build -t "mailgroup:latest" .
