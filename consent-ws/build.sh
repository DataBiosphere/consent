#!/usr/bin/env bash

set -ex

function docker_cmd() {
	if [ $DOCKER_CMD = "build" ] || [ $DOCKER_CMD = "push" ]; then
		if [ "$ENV" != "dev" ] && [ "$ENV" != "alpha" ] && [ "$ENV" != "staging" ] && [ "$ENV" != "perf" ]; then
			DOCKER_TAG=${BRANCH}
		else
			GIT_SHA=$(git rev-parse origin/$BRANCH)
			echo GIT_SHA=$GIT_SHA > env.properties
			DOCKER_TAG=${GIT_SHA:0:12}
		fi
		echo "building $REPO:$DOCKER_TAG..."
		docker build -t $REPO:$DOCKER_TAG --file consent-ws/Dockerfile .

		if [ $DOCKER_CMD = "push" ]; then
			echo "pushing $REPO:$DOCKER_TAG..."
			docker push $REPO:$DOCKER_TAG
		fi
	else
		echo "Not a valid docker option! Choose either build or push (which includes build)"
	fi
}

PROJECT=consent
BRANCH=${BRANCH:-$(git rev-parse --abbrev-ref HEAD)}
REPO=broadinstitute/$PROJECT

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
