#!/usr/bin/env bash

set -ex

function docker_cmd() {
	if [ $DOCKER_CMD = "build" ] || [ $DOCKER_CMD = "push" ]; then
		echo GIT_SHA=$GIT_SHA > env.properties
		HASH_TAG=${GIT_SHA:0:12}
		
		echo "building $REPO:$HASH_TAG..."
		docker build -t $REPO:$HASH_TAG --file consent-ws/Dockerfile .

		if [ $DOCKER_CMD = "push" ]; then
			echo "pushing $REPO:$HASH_TAG..."
			docker push $REPO:$HASH_TAG
			docker tag $REPO:$HASH_TAG $REPO:$BRANCH
			docker push $REPO:$BRANCH
		fi
	else
		echo "Not a valid docker option! Choose either build or push (which includes build)"
	fi
}

PROJECT=consent
BRANCH=${BRANCH:-$(git rev-parse --abbrev-ref HEAD)}
REPO=broadinstitute/$PROJECT
GIT_SHA=$(git rev-parse origin/$BRANCH)

while [ "$1" != "" ]; do
    case $1 in
        -d | --docker) shift
                       echo $1
                       DOCKER_CMD=$1
                       docker_cmd
                       ;;
    esac
    shift
done
