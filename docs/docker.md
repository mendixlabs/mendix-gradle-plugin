# Building a Docker container

The self-contained distribution produced by `mxDistZip` can be used to create a
Docker container that serves the Mendix application. 

To generate this docker file for your project use `mxGenerateDockerfile`. As input it takes a template to which the base
image and the project name can be configured. 

| Parameter      | Description                                                                                      |
|----------------|--------------------------------------------------------------------------------------------------|
| `%BASE_IMAGE%` | The docker base image to use. Defaults to `eclipse-temurin:21.0.3_9-jre-jammy`.                  |
| `%PROJECT%` | The project name taken from `project.name`. This is default name the distribution config uses. |

As a result a Docker file like this is produced. 

```
FROM eclipse-temurin:21.0.3_9-jre-jammy

# Use a non-root user to run the app
RUN mkdir /home/nobody && \
  chown nobody:nogroup /home/nobody && \
  chmod 700 /home/nobody && \
  usermod -d /home/nobody nobody

# Copy application in different layers for optimization
COPY --chown=nobody:nogroup build/install/project/runtime /opt/app/runtime
COPY --chown=nobody:nogroup build/install/project/app /opt/app/deployment
COPY --chown=nobody:nogroup build/install/project/etc /opt/app/etc

EXPOSE 8080

USER nobody

CMD ["java", "-DMX_INSTALL_PATH=/opt/app", "-jar", "/opt/app/runtime/launcher/runtimelauncher.jar", "/opt/app/deployment", "/opt/app/etc/app.conf"]
```

Building the image is out of scope for this plugin, but you can
use the [Docker Gradle Plugin](https://bmuschko.github.io/gradle-docker-plugin/current/user-guide/) for that. Or run

```
docker build --rm -t mx/project:latest -f build/docker/Dockerfile .
```

and then start your app and see the results at [localhost:8080](http://localhost:8080/). Use `ctrl-c` to stop the app.

```
docker run --rm -p 8080:8080 mx/project:latest
```

To overwrite the configuration file mount a file to `/opt/app/etc/app.conf` or change the start command to point to an
alternative file location.
