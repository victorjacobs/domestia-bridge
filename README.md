# Domestia-bridge

Deprecated in favor of https://github.com/victorjacobs/go-domestia.

[![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/vjacobs/domestia.svg)](https://hub.docker.com/r/vjacobs/domestia)

Bridges a Domestia DMC-008 controller to Home Assistant using MQTT autodiscovery.

## Usage

1. Create a `domestia.yaml` configuration file. Example [here](domestia.sample.yaml)
2. `docker run -v $(pwd)/domestia.yaml:/app/domestia.yaml vjacobs/domestia`
